use std::{env, net::SocketAddr, path::PathBuf, str::FromStr};
use tracing::warn;

#[derive(Debug, Clone)]
pub struct Settings {
    // Server Settings
    pub bind_address: SocketAddr,
    pub update_interval_secs: u64,
    pub log_level: String,
    // Data Source Files
    pub cpu_file: PathBuf,
    pub ram_file: PathBuf,
    pub proc_file: PathBuf,
    pub ext_temp_file: PathBuf,
    // Controller Settings
    pub controller_host: String,
    pub controller_port: u16,
    pub controller_key: u32,
}

fn get_env_var<T>(name: &str, default: T) -> T
where
    T: FromStr + std::fmt::Display,
{
    match env::var(name) {
        Ok(val_str) => match val_str.parse::<T>() {
            Ok(val) => val,
            Err(_) => {
                warn!(
                    "Failed to parse environment variable '{}' value '{}'. Using default: {}",
                    name, val_str, default
                );
                default
            }
        },
        Err(env::VarError::NotPresent) => default,
        Err(env::VarError::NotUnicode(_)) => {
            warn!(
                "Environment variable '{}' contains invalid UTF-8. Using default: {}",
                name, default
            );
            default
        }
    }
}

fn get_env_var_string(name: &str, default: String) -> String {
    match env::var(name) {
        Ok(val) => val,
        Err(env::VarError::NotPresent) => default,
        Err(env::VarError::NotUnicode(_)) => {
            warn!(
                "Environment variable '{}' contains invalid UTF-8. Using default: {}",
                name, default
            );
            default
        }
    }
}

impl Settings {
    pub fn load() -> Self {
        //dotenv::dotenv().ok(); // Load .env file if present

        Settings {
            // --- Server Settings ---
            bind_address: get_env_var(
                "BIND_ADDRESS",
                "127.0.0.1:3000"
                    .parse()
                    .expect("Default BIND_ADDRESS must parse"),
            ),
            update_interval_secs: get_env_var("UPDATE_INTERVAL_SECS", 5u64),
            log_level: get_env_var_string("LOG_LEVEL", "info".to_string()),

            // --- Data Source Files ---
            cpu_file: PathBuf::from(get_env_var_string("CPU_FILE", "/tmp/cpu".to_string())),
            ram_file: PathBuf::from(get_env_var_string("RAM_FILE", "/tmp/ram".to_string())),
            proc_file: PathBuf::from(get_env_var_string("PROC_FILE", "/tmp/proc".to_string())),
            ext_temp_file: PathBuf::from(get_env_var_string(
                "EXT_TEMP_FILE",
                "/tmp/ext_temp".to_string(),
            )),

            // --- Controller Settings ---
            controller_host: get_env_var_string("CONTROL_HOST", "127.0.0.1".to_string()),
            controller_port: get_env_var("CONTROL_PORT", 31337u16),
            controller_key: get_env_var("CONTROL_KEY", 0xDEADBEEF),
        }
    }
}
