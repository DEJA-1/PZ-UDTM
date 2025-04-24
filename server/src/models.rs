use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct CpuInfo {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cpu_temperature: Option<f32>, // In Celsius
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cpu_usage: Option<CpuUsage>,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct CpuUsage {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub full: Option<CpuStat>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cores: Option<Vec<CoreStat>>,
}

// Combined Stat structure for Full/Core CPU usage
#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct CpuStat {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_norm: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_nice: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kernel: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub idle: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub iowait: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub irq: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub soft_irq: Option<u64>,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct CoreStat {
    pub core_id: u32,
    #[serde(flatten)]
    pub stats: CpuStat,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct MemoryInfo {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total: Option<u64>, // Assuming kB
    #[serde(skip_serializing_if = "Option::is_none")]
    pub free: Option<u64>, // Assuming kB
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available: Option<u64>, // Assuming kB
}

// --- Process Info ---

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct ProcessesInfo {
    pub processes: Vec<ProcessInfo>,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct ProcessInfo {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state_code: Option<String>, // e.g., "S"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state_description: Option<String>, // e.g., "sleeping"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub group: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory_rss: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory_virt: Option<u64>, // Virtual Memory Size (second number) in kB
    #[serde(skip_serializing_if = "Option::is_none")]
    pub swap: Option<u64>, // Swap usage in kB
    #[serde(skip_serializing_if = "Option::is_none")]
    pub threads: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub utime: Option<u64>, // User mode time (consider units, might be jiffies/ticks)
}

// --- Overall System Status ---
// Holds all parsed data combined from the files
#[derive(Debug, Clone, Default)]
pub struct SystemStatus {
    pub cpu: CpuInfo,
    pub memory: MemoryInfo,
    pub processes: ProcessesInfo,
}
