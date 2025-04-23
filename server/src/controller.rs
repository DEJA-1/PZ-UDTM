use byteorder::{LittleEndian, WriteBytesExt};
use std::io::{self, Cursor};
use std::net::{AddrParseError, SocketAddr};
use std::time::Duration;
use thiserror::Error;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio::time::timeout;
use tracing::{debug, error, info, warn};

const CONNECT_TIMEOUT: Duration = Duration::from_secs(2);
const CMD_TIMEOUT: Duration = Duration::from_secs(5);

const CMD_PING: u8 = 0x01;
const CMD_KILL_PROCESS: u8 = 0x02;
const CMD_SHUTDOWN: u8 = 0x03;
const CMD_REBOOT: u8 = 0x04;

pub const RESP_OK: u8 = 0x00;
pub const RESP_ERROR_GENERIC: u8 = 0x01;
pub const RESP_ERROR_INVALID_CMD: u8 = 0x02;
pub const RESP_ERROR_INVALID_ARG: u8 = 0x03;
pub const RESP_ERROR_PERMISSION: u8 = 0x04;

// --- Error Enum ---
#[derive(Error, Debug)]
pub enum ControlError {
    #[error("Failed to resolve controller address: {0}")]
    AddressResolution(AddrParseError), // Correct error type
    #[error("Failed to connect to controller at {0}: {1}")]
    Connection(SocketAddr, io::Error),
    #[error("Authentication failed")]
    Authentication,
    #[error("Communication error: {0}")]
    Io(#[from] io::Error),
    #[error("Command timed out")]
    Timeout,
    #[error("Controller responded with error code: {0}")]
    ControllerError(u8),
    #[error("Invalid response from controller")]
    InvalidResponse,
    #[error("Internal Error: Failed to get stream from mutex guard")]
    InternalMutexError,
}

// --- Command Packet Struct ---
#[derive(Debug, Clone, Copy)]
struct CommandPacket {
    command_id: u8,
    arg1: u32,
    padding: [u8; 3],
}

impl CommandPacket {
    fn new(command_id: u8, arg1: u32) -> Self {
        CommandPacket {
            command_id,
            arg1,
            padding: [0; 3],
        }
    }

    fn serialize(&self) -> Result<[u8; 8], io::Error> {
        let mut buf = [0u8; 8];
        let mut cursor = Cursor::new(&mut buf[..]);
        WriteBytesExt::write_u8(&mut cursor, self.command_id)?;
        WriteBytesExt::write_u32::<LittleEndian>(&mut cursor, self.arg1)?;
        std::io::Write::write_all(&mut cursor, &self.padding)?;
        Ok(buf)
    }
}

// --- Controller Client Struct ---
#[derive(Debug)]
pub struct ControllerClient {
    stream: Mutex<Option<TcpStream>>,
    addr: SocketAddr,
    key: u32,
}

impl ControllerClient {
    pub async fn new(host: &str, port: u16, key: u32) -> Result<Self, ControlError> {
        let addr_str = format!("{}:{}", host, port);
        let addr = addr_str
            .parse::<SocketAddr>()
            .map_err(ControlError::AddressResolution)?;

        info!(
            "Controller client configured for address: {}, key: 0x{:X}",
            addr, key
        );

        Ok(ControllerClient {
            stream: Mutex::new(None),
            addr,
            key,
        })
    }

    async fn get_connection(
        &self,
    ) -> Result<tokio::sync::MutexGuard<'_, Option<TcpStream>>, ControlError> {
        let mut stream_guard = self.stream.lock().await;

        if let Some(ref mut stream) = *stream_guard {
            match stream.peer_addr() {
                Ok(_) => {
                    debug!("Assuming existing controller connection is valid");
                    return Ok(stream_guard);
                }
                Err(e) => {
                    warn!(
                        "Existing connection seems invalid (error: {}). Reconnecting.",
                        e
                    );
                    *stream_guard = None;
                }
            }
        }

        if stream_guard.is_none() {
            info!("Attempting to connect to controller at {}", self.addr);
            match timeout(CONNECT_TIMEOUT, TcpStream::connect(self.addr)).await {
                Ok(Ok(mut stream)) => {
                    info!("Connected to controller. Authenticating...");
                    let key_bytes = self.key.to_le_bytes();

                    match timeout(CMD_TIMEOUT, async {
                        stream.write_all(&key_bytes).await?;
                        stream.flush().await?;
                        Ok::<_, io::Error>(())
                    })
                    .await
                    {
                        Ok(Ok(_)) => {
                            info!("Authentication successful.");
                            *stream_guard = Some(stream);
                        }
                        Ok(Err(e)) => {
                            error!("Authentication failed: IO error: {}", e);
                            *stream_guard = None;
                            return Err(ControlError::Authentication);
                        }
                        Err(_) => {
                            error!("Authentication failed: Timeout");
                            *stream_guard = None;
                            return Err(ControlError::Timeout);
                        }
                    }
                }
                Ok(Err(e)) => {
                    error!("Failed to connect to controller: {}", e);
                    return Err(ControlError::Connection(self.addr, e));
                }
                Err(_) => {
                    error!("Timeout connecting to controller at {}", self.addr);
                    return Err(ControlError::Timeout);
                }
            }
        }
        Ok(stream_guard)
    }

    pub async fn send_command(&self, command_id: u8, arg1: u32) -> Result<u8, ControlError> {
        let packet = CommandPacket::new(command_id, arg1);
        let packet_bytes = packet.serialize()?;

        debug!(
            "Sending command: ID=0x{:02X}, Arg1={}, Bytes={:?}",
            command_id, arg1, packet_bytes
        );

        let mut stream_guard = self.get_connection().await?;
        let stream_opt = stream_guard.as_mut();

        let stream = match stream_opt {
            Some(s) => s,
            None => {
                error!("BUG: get_connection succeeded but stream is None upon use");
                return Err(ControlError::InternalMutexError);
            }
        };

        let result: Result<u8, ControlError> = async {
            timeout(CMD_TIMEOUT, stream.write_all(&packet_bytes)).await??;
            timeout(CMD_TIMEOUT, stream.flush()).await??;

            let mut response_buf = [0u8; 1];
            timeout(CMD_TIMEOUT, stream.read_exact(&mut response_buf)).await??;

            Ok(response_buf[0])
        }
        .await;

        match result {
            Ok(response_code) => {
                debug!("Received response code: 0x{:02X}", response_code);
                if response_code == RESP_OK {
                    Ok(response_code)
                } else {
                    warn!("Controller returned error code: {}", response_code);
                    Err(ControlError::ControllerError(response_code))
                }
            }
            Err(e) => {
                warn!("Command failed: {}. Invalidating connection.", e);
                *stream_guard = None;
                Err(e) // Pass Timeout/Io errors directly
            }
        }
    }

    // --- Specific Command Wrappers ---

    pub async fn kill_process(&self, pid: u32) -> Result<(), ControlError> {
        info!("Requesting kill process PID: {}", pid);
        self.send_command(CMD_KILL_PROCESS, pid).await?;
        info!("Kill command for PID {} acknowledged by controller.", pid);
        Ok(())
    }

    pub async fn shutdown_system(&self) -> Result<(), ControlError> {
        info!("Requesting system shutdown");
        self.send_command(CMD_SHUTDOWN, 0).await?;
        info!("Shutdown command acknowledged by controller.");
        Ok(())
    }

    pub async fn reboot_system(&self) -> Result<(), ControlError> {
        info!("Requesting system reboot");
        self.send_command(CMD_REBOOT, 0).await?;
        info!("Reboot command acknowledged by controller.");
        Ok(())
    }

    pub async fn ping_controller(&self) -> Result<(), ControlError> {
        info!("Pinging controller");
        self.send_command(CMD_PING, 0).await?;
        info!("Ping successful (acknowledged by controller).");
        Ok(())
    }
}

impl From<tokio::time::error::Elapsed> for ControlError {
    fn from(_: tokio::time::error::Elapsed) -> Self {
        ControlError::Timeout
    }
}
