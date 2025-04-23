mod config;
mod controller;
mod data_source;
mod handlers;
mod models;

use axum::{
    routing::{get, post},
    Router,
};
use config::Settings;
use controller::ControllerClient;
use data_source::read_status_files;
use models::SystemStatus;
use std::{process, sync::Arc, time::Duration};
use tokio::sync::RwLock;
use tracing::{error, info, warn};
use tracing_subscriber::{fmt, prelude::*, EnvFilter};

// --- Simplified Shared Application State ---
#[derive(Debug)]
pub struct AppState {
    pub system_status: SystemStatus,
    pub controller_client: Arc<ControllerClient>,
}

#[tokio::main]
async fn main() {
    // --- Load Configuration ---
    let settings = Settings::load();

    let log_level_filter = EnvFilter::try_from_default_env()
        .or_else(|_| EnvFilter::try_new(&settings.log_level))
        .unwrap_or_else(|_| EnvFilter::new("info"));

    tracing_subscriber::registry()
        .with(fmt::layer())
        .with(log_level_filter)
        .init();

    info!("Starting system status API...");
    info!(
        "Data source files: CPU='{}', RAM/Usage='{}', Proc='{}'",
        settings.cpu_file.display(),
        settings.ram_file.display(),
        settings.proc_file.display()
    );
    info!(
        "Data refresh interval: {} seconds",
        settings.update_interval_secs
    );
    info!(
        "Controller: host='{}', port={}, key=0x{:X}",
        settings.controller_host, settings.controller_port, settings.controller_key
    );
    info!("Server binding to: {}", settings.bind_address);

    let controller_client = match ControllerClient::new(
        &settings.controller_host,
        settings.controller_port,
        settings.controller_key,
    )
    .await
    {
        Ok(client) => Arc::new(client),
        Err(e) => {
            error!("Fatal: Failed to initialize controller client: {}", e);
            process::exit(1);
        }
    };

    match controller_client.ping_controller().await {
        Ok(_) => info!("Initial ping to controller successful."),
        Err(e) => warn!(
            "Initial ping to controller failed: {}. Control functions may fail.",
            e
        ),
    }

    // --- Create Shared State ---
    let shared_state = Arc::new(RwLock::new(AppState {
        system_status: SystemStatus::default(),
        controller_client: Arc::clone(&controller_client),
    }));

    // --- Background Task Periodic Updates ---
    let state_clone_for_updater = Arc::clone(&shared_state);
    let settings_clone_for_updater = settings.clone();
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(
            settings_clone_for_updater.update_interval_secs,
        ));
        loop {
            interval.tick().await;
            info!("Background task: Updating system status from files...");

            let new_status = read_status_files(
                &settings_clone_for_updater.cpu_file,
                &settings_clone_for_updater.ram_file,
                &settings_clone_for_updater.proc_file,
            )
            .await;

            {
                let mut state_guard = state_clone_for_updater.write().await;
                state_guard.system_status = new_status;
            }
            info!("Background task: System status update complete.");
        }
    });

    // --- Setup Axum Router ---
    let app = Router::new()
        .route("/cpu", get(handlers::get_cpu_info))
        .route("/memory", get(handlers::get_memory_info))
        .route("/processes", get(handlers::get_processes_info))
        .route("/control/ping", post(handlers::ping_controller))
        .route("/control/process/kill", post(handlers::kill_process))
        .route("/control/system/shutdown", post(handlers::shutdown_system))
        .route("/control/system/reboot", post(handlers::reboot_system))
        .with_state(shared_state);

    // --- Run the Server ---
    info!("Server listening on {}", settings.bind_address);
    let listener = tokio::net::TcpListener::bind(settings.bind_address)
        .await
        .unwrap_or_else(|e| {
            error!(
                "Fatal: Failed to bind to address {}: {}",
                settings.bind_address, e
            );
            process::exit(1);
        });

    axum::serve(listener, app.into_make_service())
        .await
        .expect("Failed to start server");
}
