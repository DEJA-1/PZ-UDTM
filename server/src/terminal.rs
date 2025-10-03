use std::io::{Read, Write};
use std::path::Path;
use std::sync::Arc;

use anyhow::Result;
use axum::extract::ws::{Message, WebSocket};
use futures_util::{SinkExt, StreamExt};
use portable_pty::{native_pty_system, CommandBuilder, PtyPair, PtySize};
use tokio::sync::{mpsc, Mutex};
use tracing::{debug, error, info, warn};

pub struct PtySession {
    pub pair: PtyPair,
}

impl PtySession {
    pub fn spawn_shell_in_home() -> Result<Self> {
        let pty_system = native_pty_system();
        let pair = pty_system.openpty(PtySize {
            rows: 24,
            cols: 80,
            pixel_width: 0,
            pixel_height: 0,
        })?;

        let mut cmd = CommandBuilder::new("/bin/sh");
        cmd.arg("-l");
        match std::env::var("HOME") {
            Ok(home) => {
                info!("PTY: using HOME as cwd: {}", home);
                cmd.cwd(Path::new(&home));
            }
            Err(_) => {
                warn!("PTY: HOME not set; using / as cwd");
                cmd.cwd(Path::new("/"));
            }
        }

        info!("PTY: spawning /bin/sh -l");
        let _child = pair.slave.spawn_command(cmd)?;
        Ok(PtySession { pair })
    }
}

pub async fn bridge_ws_to_pty(ws: WebSocket) -> Result<()> {
    info!("PTY bridge: starting session");
    let session = PtySession::spawn_shell_in_home()?;
    info!("PTY bridge: session spawned");
    let reader = session.pair.master.try_clone_reader()?;
    let mut writer = session.pair.master.take_writer()?;

    let (ws_tx, mut ws_rx) = ws.split();

    let (tx, mut rx) = mpsc::channel::<Vec<u8>>(32);

    std::thread::spawn(move || {
        let mut r = reader;
        loop {
            let mut buf = [0u8; 4096];
            match r.read(&mut buf) {
                Ok(0) => {
                    info!("PTY: EOF");
                    break;
                }
                Ok(n) => {
                    debug!("PTY: read {} bytes", n);
                    let _ = tx.blocking_send(buf[..n].to_vec());
                }
                Err(e) => {
                    error!("PTY read error: {}", e);
                    break;
                }
            }
        }
        info!("PTY reader finished");
    });

    let ws_send = Arc::new(Mutex::new(ws_tx));
    let ws_send_clone = ws_send.clone();
    let reader_task = tokio::spawn(async move {
        while let Some(chunk) = rx.recv().await {
            let mut guard = ws_send_clone.lock().await;
            debug!("WS: sending {} bytes", chunk.len());
            if let Err(e) = guard.send(Message::Binary(chunk)).await {
                error!("WS send error: {}", e);
                break;
            }
        }
    });

    while let Some(msg) = ws_rx.next().await {
        match msg {
            Ok(Message::Binary(data)) => {
                debug!("WS: recv binary {} bytes", data.len());
                if let Err(e) = writer.write_all(&data) {
                    error!("PTY write error: {}", e);
                    break;
                }
            }
            Ok(Message::Text(text)) => {
                debug!("WS: recv text {} bytes", text.len());
                if let Err(e) = writer.write_all(text.as_bytes()) {
                    error!("PTY write error: {}", e);
                    break;
                }
            }
            Ok(Message::Close(frame)) => {
                info!("WS: close received: {:?}", frame);
                break;
            }
            Ok(Message::Ping(_)) => {
                debug!("WS: ping");
            }
            Ok(Message::Pong(_)) => {
                debug!("WS: pong");
            }
            Err(e) => {
                error!("WS receive error: {}", e);
                break;
            }
        }
    }

    // No extra newline on close

    let _ = reader_task.await;
    Ok(())
}


