use crate::{controller::*, models::*, AppState};
use axum::{extract::State, http::StatusCode, Json};
use serde::Deserialize;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{debug, error};

// --- Status Handlers ---
pub async fn get_cpu_info(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<Json<CpuInfo>, StatusCode> {
    debug!("Handling /cpu request");
    let app_state = state.read().await;
    let cpu_info = app_state.system_status.cpu.clone();
    Ok(Json(cpu_info))
}

pub async fn get_memory_info(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<Json<MemoryInfo>, StatusCode> {
    debug!("Handling /memory request");
    let app_state = state.read().await;
    let memory_info = app_state.system_status.memory.clone();
    Ok(Json(memory_info))
}

pub async fn get_processes_info(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<Json<ProcessesInfo>, StatusCode> {
    debug!("Handling /processes request");
    let app_state = state.read().await;
    let processes_info = app_state.system_status.processes.clone();
    Ok(Json(processes_info))
}

// --- Control Handlers ---
fn map_control_error(e: ControlError) -> (StatusCode, String) {
    error!("Control operation failed: {}", e);
    match e {
        ControlError::AddressResolution(parse_err) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("Failed to parse controller address: {}", parse_err),
        ),
        ControlError::Connection(addr, io_err) => (
            StatusCode::SERVICE_UNAVAILABLE,
            format!(
                "Cannot connect to controller service at {}: {}",
                addr, io_err
            ),
        ),
        ControlError::Authentication => (
            StatusCode::INTERNAL_SERVER_ERROR,
            "Authentication with controller service failed".to_string(),
        ),
        ControlError::Io(_) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            "Communication error with controller service".to_string(),
        ),
        ControlError::Timeout => (
            StatusCode::GATEWAY_TIMEOUT,
            "Request to controller service timed out".to_string(),
        ),
        ControlError::ControllerError(code) => (
            StatusCode::BAD_GATEWAY,
            format!("Controller service reported error code: {}", code),
        ),
        ControlError::InvalidResponse => (
            StatusCode::BAD_GATEWAY,
            "Invalid response received from controller service".to_string(),
        ),
        ControlError::InternalMutexError => (
            StatusCode::INTERNAL_SERVER_ERROR,
            "Internal server error processing request (controller state)".to_string(),
        ),
    }
}

#[derive(Deserialize, Debug)]
pub struct KillRequest {
    pid: u32,
}

pub async fn kill_process(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(payload): Json<KillRequest>,
) -> Result<StatusCode, (StatusCode, String)> {
    debug!(
        "Handling POST /control/process/kill with payload: {:?}",
        payload
    );
    let app_state = state.read().await;
    match app_state.controller_client.kill_process(payload.pid).await {
        Ok(_) => Ok(StatusCode::OK),
        Err(e) => Err(map_control_error(e)),
    }
}

pub async fn shutdown_system(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<StatusCode, (StatusCode, String)> {
    debug!("Handling POST /control/system/shutdown");
    let app_state = state.read().await;
    match app_state.controller_client.shutdown_system().await {
        Ok(_) => Ok(StatusCode::OK),
        Err(e) => Err(map_control_error(e)),
    }
}

pub async fn reboot_system(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<StatusCode, (StatusCode, String)> {
    debug!("Handling POST /control/system/reboot");
    let app_state = state.read().await;
    match app_state.controller_client.reboot_system().await {
        Ok(_) => Ok(StatusCode::OK),
        Err(e) => Err(map_control_error(e)),
    }
}

pub async fn ping_controller(
    State(state): State<Arc<RwLock<AppState>>>,
) -> Result<StatusCode, (StatusCode, String)> {
    debug!("Handling POST /control/ping");
    let app_state = state.read().await;
    match app_state.controller_client.ping_controller().await {
        Ok(_) => Ok(StatusCode::OK),
        Err(e) => Err(map_control_error(e)),
    }
}
