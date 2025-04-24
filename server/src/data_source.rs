use crate::models::*;
use std::{io, path::Path};
use thiserror::Error;
use tokio::fs;
use tracing::{debug, error, warn};

#[derive(Error, Debug)]
pub enum ParseError {
    #[error("I/O error reading file '{0}': {1}")]
    Io(String, io::Error),
    #[error("File '{file}' parse error: {message} (Line: '{line}')")]
    FileFormat {
        file: String,
        message: String,
        line: String,
    },
    #[error("Value parse error in file '{file}': {message} (Line: '{line}', Value: '{value}')")]
    ValueFormat {
        file: String,
        message: String,
        line: String,
        value: String,
    },
}

fn extract_value<'a>(line: &'a str, key_prefix: &str) -> Option<&'a str> {
    line.strip_prefix(key_prefix)
        .and_then(|s| s.split(':').nth(1))
        .map(|s| s.trim().split_whitespace().next().unwrap_or(""))
        .filter(|s| !s.is_empty())
}

fn parse_numeric<T: std::str::FromStr>(
    value_str: &str,
    line: &str,
    file_path: &Path,
) -> Result<T, ParseError> {
    value_str.parse::<T>().map_err(|_| ParseError::ValueFormat {
        file: file_path.to_string_lossy().into_owned(),
        message: format!("Could not parse as {}", std::any::type_name::<T>()),
        line: line.to_string(),
        value: value_str.to_string(),
    })
}

// --- File Specific Parsers ---

async fn parse_cpu_file(file_path: &Path) -> Result<CpuInfo, ParseError> {
    let content = fs::read_to_string(file_path)
        .await
        .map_err(|e| ParseError::Io(file_path.to_string_lossy().into_owned(), e))?;

    let mut cpu_info = CpuInfo::default();
    // Initialize usage structure here as it's part of cpu_info
    let mut cpu_usage = CpuUsage::default();
    let mut current_core_stat: Option<CoreStat> = None;
    let mut cores = Vec::new();
    let mut parsing_full_cpu = false;

    for line in content.lines() {
        let trimmed_line = line.trim();
        if trimmed_line.is_empty() {
            continue;
        }

        // --- Temperature ---
        if let Some(value_str) = extract_value(trimmed_line, "CPU temp") {
            match parse_numeric::<f32>(value_str, trimmed_line, file_path) {
                Ok(temp_milli) => cpu_info.cpu_temperature = Some(temp_milli / 1000.0),
                Err(e) => warn!("{}: {}", file_path.display(), e),
            }
            parsing_full_cpu = false;
            if current_core_stat.is_some() {
                if let Some(core) = current_core_stat.take() {
                    cores.push(core);
                }
            }
        }
        // --- Full CPU Header ---
        else if trimmed_line.starts_with("Full CPU:") {
            cpu_usage.full.get_or_insert_with(Default::default);
            parsing_full_cpu = true;
            if current_core_stat.is_some() {
                // Finish core if starting full CPU block
                if let Some(core) = current_core_stat.take() {
                    cores.push(core);
                }
            }
        }
        // --- Core Header ---
        else if trimmed_line.starts_with("Core ") {
            parsing_full_cpu = false;
            if let Some(core) = current_core_stat.take() {
                cores.push(core);
            }

            if let Some(id_str) = trimmed_line
                .strip_prefix("Core ")
                .and_then(|s| s.strip_suffix(':'))
                .map(|s| s.trim())
            {
                match id_str.parse::<u32>() {
                    Ok(id) => {
                        current_core_stat = Some(CoreStat {
                            core_id: id,
                            stats: CpuStat::default(),
                        })
                    }
                    Err(_) => warn!(
                        "{}: Failed to parse core ID from '{}'",
                        file_path.display(),
                        trimmed_line
                    ),
                }
            } else {
                warn!(
                    "{}: Malformed Core header line: '{}'",
                    file_path.display(),
                    trimmed_line
                );
            }
        } else {
            // Try parsing as Full CPU Stat if in that section
            if parsing_full_cpu {
                if let Some(stat) = cpu_usage.full.as_mut() {
                    if let Some(val_str) = extract_value(trimmed_line, "User norm") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.user_norm = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "User nice") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.user_nice = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "Kernel") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.kernel = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "Idle") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.idle = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "Iowait") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.iowait = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "Irq") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.irq = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else if let Some(val_str) = extract_value(trimmed_line, "Soft irq") {
                        match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                            Ok(val) => stat.soft_irq = Some(val),
                            Err(e) => warn!("{}: {}", file_path.display(), e),
                        }
                    } else {
                        // Line is within "Full CPU" but doesn't match known keys
                        warn!(
                            "{}: Ignoring unrecognized line in Full CPU section: {}",
                            file_path.display(),
                            trimmed_line
                        );
                    }
                }
            }
            // Try parsing as Core Stat if currently parsing a core
            else if let Some(core) = current_core_stat.as_mut() {
                if let Some(val_str) = extract_value(trimmed_line, "User norm") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.user_norm = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "User nice") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.user_nice = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "Kernel") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.kernel = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "Idle") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.idle = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "Iowait") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.iowait = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "Irq") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.irq = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else if let Some(val_str) = extract_value(trimmed_line, "Soft irq") {
                    match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                        Ok(val) => core.stats.soft_irq = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else {
                    // Line is within a "Core" but doesn't match known keys
                    warn!(
                        "{}: Ignoring unrecognized line in Core {} section: {}",
                        file_path.display(),
                        core.core_id,
                        trimmed_line
                    );
                }
            }
            // Otherwise, it's an unrecognized line outside known sections
            else {
                warn!(
                    "{}: Ignoring unrecognized line: {}",
                    file_path.display(),
                    trimmed_line
                );
            }
        }
    }

    // Finish the last core
    if let Some(core) = current_core_stat.take() {
        cores.push(core);
    }
    if !cores.is_empty() {
        cpu_usage.cores = Some(cores);
    }

    // Assign usage stats to the main CpuInfo struct if any were found
    if cpu_usage.full.is_some() || cpu_usage.cores.is_some() {
        cpu_info.cpu_usage = Some(cpu_usage);
    }

    Ok(cpu_info)
}

// Parses /tmp/ram
async fn parse_ram_file(file_path: &Path) -> Result<MemoryInfo, ParseError> {
    let content = fs::read_to_string(file_path)
        .await
        .map_err(|e| ParseError::Io(file_path.to_string_lossy().into_owned(), e))?;
    let mut memory_info = MemoryInfo::default();

    for line in content.lines() {
        let trimmed_line = line.trim();
        if trimmed_line.is_empty() {
            continue;
        }

        if let Some(val_str) = extract_value(trimmed_line, "Ram total") {
            match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                Ok(val) => memory_info.total = Some(val),
                Err(e) => warn!("{}: {}", file_path.display(), e),
            }
        } else if let Some(val_str) = extract_value(trimmed_line, "Ram free") {
            match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                Ok(val) => memory_info.free = Some(val),
                Err(e) => warn!("{}: {}", file_path.display(), e),
            }
        } else if let Some(val_str) = extract_value(trimmed_line, "Ram available") {
            match parse_numeric::<u64>(val_str, trimmed_line, file_path) {
                Ok(val) => memory_info.available = Some(val),
                Err(e) => warn!("{}: {}", file_path.display(), e),
            }
        } else {
            // Ignore other lines silently
            debug!(
                "{}: Ignoring unrecognized line: {}",
                file_path.display(),
                trimmed_line
            );
        }
    }
    Ok(memory_info)
}

// Parses /tmp/proc (Process Info)
async fn parse_proc_file(file_path: &Path) -> Result<ProcessesInfo, ParseError> {
    let content = fs::read_to_string(file_path)
        .await
        .map_err(|e| ParseError::Io(file_path.to_string_lossy().into_owned(), e))?;
    let mut processes_info = ProcessesInfo::default();
    let mut current_process: Option<ProcessInfo> = None;

    for line in content.lines() {
        let trimmed_line = line.trim();
        if trimmed_line.is_empty() {
            continue;
        }

        if trimmed_line.starts_with("Proc:") {
            if let Some(proc) = current_process.take() {
                if proc.pid.is_some() {
                    processes_info.processes.push(proc);
                } else {
                    warn!(
                        "{}: Discarding process block due to missing PID",
                        file_path.display()
                    );
                }
            }

            let parts: Vec<&str> = trimmed_line.splitn(3, ' ').collect();
            if parts.len() >= 3 {
                match parts[1].parse::<u32>() {
                    Ok(pid) => {
                        current_process = Some(ProcessInfo {
                            pid: Some(pid),
                            name: Some(parts[2].to_string()),
                            ..Default::default()
                        });
                    }
                    Err(_) => {
                        warn!(
                            "{}: Failed to parse PID from Proc line: '{}'",
                            file_path.display(),
                            trimmed_line
                        );
                        current_process = None;
                    }
                }
            } else {
                warn!(
                    "{}: Malformed Proc line: '{}'",
                    file_path.display(),
                    trimmed_line
                );
                current_process = None;
            }
        } else if let Some(proc) = current_process.as_mut() {
            if trimmed_line.starts_with("State:") {
                if let Some(value_part) = trimmed_line.splitn(2, ':').nth(1) {
                    let value_trimmed = value_part.trim(); // e.g., "S (sleeping)" or "R"
                    if let Some((code, desc_part)) = value_trimmed.split_once('(') {
                        // Case: "S (sleeping)"
                        proc.state_code = Some(code.trim().to_string()); // "S"
                        proc.state_description =
                            Some(desc_part.trim_end_matches(')').trim().to_string());
                    // "sleeping"
                    } else {
                        // Case: "R" (or anything without parenthesis)
                        proc.state_code = Some(value_trimmed.to_string()); // "R"
                        proc.state_description = None; // No description present
                    }
                } else {
                    // Should not happen if starts_with("State:") is true, but defensive check
                    warn!(
                        "{}: Malformed State line (missing ':'?): '{}'",
                        file_path.display(),
                        trimmed_line
                    );
                }
            } else if let Some(value_str) = extract_value(trimmed_line, "User") {
                proc.user = value_str.split_whitespace().last().map(String::from);
            } else if let Some(value_str) = extract_value(trimmed_line, "Group") {
                proc.group = value_str.split_whitespace().last().map(String::from);
            } else if let Some(value_str) = extract_value(trimmed_line, "Memory") {
                // extract_value gets the first part "2479488/2479488", need to split again
                if let Some((rss_str, virt_str)) = value_str.split_once('/') {
                    match parse_numeric::<u64>(rss_str, trimmed_line, file_path) {
                        Ok(val) => proc.memory_rss = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                    match parse_numeric::<u64>(virt_str, trimmed_line, file_path) {
                        Ok(val) => proc.memory_virt = Some(val),
                        Err(e) => warn!("{}: {}", file_path.display(), e),
                    }
                } else {
                    // Fallback if only one number is present? Or log warning.
                    warn!(
                        "{}: Malformed Memory line (expected 'rss/virt'): '{}'",
                        file_path.display(),
                        trimmed_line
                    );
                }
            } else if let Some(value_str) = extract_value(trimmed_line, "Swap") {
                match parse_numeric::<u64>(value_str, trimmed_line, file_path) {
                    Ok(val) => proc.swap = Some(val),
                    Err(e) => warn!("{}: {}", file_path.display(), e),
                }
            } else if let Some(value_str) = extract_value(trimmed_line, "Threads") {
                match parse_numeric::<u32>(value_str, trimmed_line, file_path) {
                    Ok(val) => proc.threads = Some(val),
                    Err(e) => warn!("{}: {}", file_path.display(), e),
                }
            } else if let Some(value_str) = extract_value(trimmed_line, "Utime") {
                match parse_numeric::<u64>(value_str, trimmed_line, file_path) {
                    Ok(val) => proc.utime = Some(val),
                    Err(e) => warn!("{}: {}", file_path.display(), e),
                }
            } else if trimmed_line.starts_with("Max_cpus:") {
                debug!(
                    "{}: Ignoring Max_cpus line: {}",
                    file_path.display(),
                    trimmed_line
                );
            } else {
                debug!(
                    "{}: Ignoring unrecognized line within process block: {}",
                    file_path.display(),
                    trimmed_line
                );
            }
        } else {
            // Should not happen often if file format is consistent
            debug!(
                "{}: Ignoring unrecognized line outside process block: {}",
                file_path.display(),
                trimmed_line
            );
        }
    }

    if let Some(proc) = current_process.take() {
        if proc.pid.is_some() {
            processes_info.processes.push(proc);
        } else {
            warn!(
                "{}: Discarding last process block due to missing PID",
                file_path.display()
            );
        }
    }

    Ok(processes_info)
}

pub async fn read_status_files(cpu_path: &Path, ram_path: &Path, proc_path: &Path) -> SystemStatus {
    debug!(
        "Reading status files: CPU='{}', RAM='{}', Proc='{}'", // Renamed log slightly
        cpu_path.display(),
        ram_path.display(),
        proc_path.display()
    );

    let mut system_status = SystemStatus::default();

    match parse_cpu_file(cpu_path).await {
        Ok(cpu_info) => {
            system_status.cpu = cpu_info;
        }
        Err(e) => {
            error!("Failed to parse CPU file '{}': {}", cpu_path.display(), e);
        }
    }

    match parse_ram_file(ram_path).await {
        Ok(memory_info) => {
            system_status.memory = memory_info;
        }
        Err(e) => {
            error!("Failed to parse RAM file '{}': {}", ram_path.display(), e);
        }
    }

    match parse_proc_file(proc_path).await {
        Ok(processes_info) => {
            system_status.processes = processes_info;
        }
        Err(e) => {
            error!(
                "Failed to parse Process file '{}': {}",
                proc_path.display(),
                e
            );
        }
    }

    debug!("Status file parsing complete. Status: {:?}", system_status);
    system_status
}
