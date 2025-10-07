# System Status API

This is a REST API that provides system status information for a Raspberry Pi (or similar Linux system). It retrieves data about CPU, memory, and processes from status files. It also *attempts* to integrate with an external system controller (currently, the controller integration is a work in progress).

## API Endpoints

The following GET endpoints are available to retrieve system status data:

*   `/cpu`: Returns CPU information, including temperature and per-core usage statistics.
*   `/memory`: Returns memory information, including total, free, and available RAM.
*   `/processes`: Returns a list of currently running processes with details such as PID, name, state, user, group, and memory usage.

## Usage

To access the API, you'll need the IP address of the Raspberry Pi (or system where the API is running) and the port number (default is 3000).  Make sure the API is running on the Raspberry Pi before attempting to connect.

**Assumptions:**

*   The API is running on the Raspberry Pi.
*   You have network connectivity to the Raspberry Pi.
*   The status files (`/tmp/cpu`, `/tmp/ram`, `/tmp/proc`) are being populated by a process like `rpi_watch`.

**Example URLs:**

Assuming the Raspberry Pi's IP address is `192.168.100.192`, you can use these URLs:

*   `http://192.168.100.192:3000/cpu`
*   `http://192.168.100.192:3000/memory`
*   `http://192.168.100.192:3000/processes`


Example Output (JSON Payloads):

/cpu:
```
{
  "cpu_temperature": 42.5,
  "cpu_usage": {
    "full": {
      "user_norm": 10,
      "user_nice": 0,
      "kernel": 20,
      "idle": 900,
      "iowait": 5,
      "irq": 0,
      "soft_irq": 0
    },
    "cores": [
      {
        "core_id": 0,
        "stats": {
          "user_norm": 5,
          "user_nice": 0,
          "kernel": 10,
          "idle": 220,
          "iowait": 1,
          "irq": 0,
          "soft_irq": 0
        }
      },
      {
        "core_id": 1,
        "stats": {
          "user_norm": 3,
          "user_nice": 0,
          "kernel": 7,
          "idle": 225,
          "iowait": 0,
          "irq": 0,
          "soft_irq": 0
        }
      }
      // ... more cores ...
    ]
  }
}
```
/memory:
```
{"total":3882924,"free":368532,"available":2974856}
```

/processes:
```
{
  "processes": [
    {
      "pid": 471,
      "name": "systemd-timesyn",
      "state_code": "S",
      "state_description": "sleeping",
      "user": "997",
      "group": "997",
      "memory_rss": 90708,
      "memory_virt": 156112,
      "swap": 0,
      "threads": 2,
      "utime": 0
    },
    {
      "pid": 477,
      "name": "avahi-daemon",
      "state_code": "S",
      "state_description": "sleeping",
      "user": "104",
      "group": "109",
      "memory_rss": 7360,
      "memory_virt": 7444,
      "swap": 0,
      "threads": 1,
      "utime": 0
    }
    // ... more processes ...
  ]
}
```

Important: This API relies on a separate process to collect system status data and write it to the `/tmp/cpu`, `/tmp/ram`, and `/tmp/proc` files. If these files are empty or not being updated, the API will not function correctly.


## Configuration

The API reads system status data from the following files:

*   `/tmp/cpu`: CPU temperature and usage statistics.
*   `/tmp/ram`: Memory information.
*   `/tmp/proc`: Process information.

These file paths can be changed using environment variables (see the "Development" section below).

## Development

To run the API, you'll need Rust and Cargo installed.

1.  Clone the repository.
2.  Navigate to the server directory in your terminal.
3.  Run `cargo run`.

The following environment variables can be used to configure the API:

*   `BIND_ADDRESS`: The IP address and port the API should listen on (default: `127.0.0.1:3000`). **Important: Set this to `0.0.0.0:3000` to allow access from other machines on the network.**
*   `CPU_FILE`: The path to the CPU status file (default: `/tmp/cpu`).
*   `RAM_FILE`: The path to the RAM status file (default: `/tmp/ram`).
*   `PROC_FILE`: The path to the process status file (default: `/tmp/proc`).
*   `CONTROL_HOST`: The hostname or IP address of the controller service (default: `127.0.0.1`).
*   `CONTROL_PORT`: The port number of the controller service (default: 9999).
*   `CONTROL_KEY`: The authentication key for the controller service (default: `0xDEADBEEF`).



## Interactive Terminal over WebSocket (MVP)

This server exposes a minimal interactive shell over WebSocket. It spawns a `/bin/sh -l` process attached to a PTY and bridges bytes in both directions. Suitable for a chat-like terminal UI in the mobile app.

### Endpoint

- `GET /terminal/ws` (WebSocket upgrade)
  - No authentication (MVP).
  - Working directory: current process `HOME` if set, else `/`.
  - Shell: `/bin/sh -l`.
  - Session lifetime: bounded to the WS connection; closing the socket terminates the shell.

### Data framing and behavior

- Server sends output as WebSocket Binary frames (opaque bytes). Treat as UTF‑8 text when rendering.
- Client may send input as Text or Binary frames; both are written as bytes to stdin.
- Send a newline (`\n`) to submit a command.
- Special keys:
  - Enter: `\n`
  - Ctrl‑C: single byte `0x03`
  - Backspace: `0x7f` (DEL) or `0x08`
  - Arrow keys and other escape sequences are forwarded as received.
- Resize: not implemented yet. Future plan is a small JSON control message like `{ "type": "resize", "rows": 30, "cols": 120 }`.
- Ping/Pong: WebSocket heartbeats for liveness; they do not affect terminal data.

### Quick start (CLI test)

1. Run the server:
```bash
BIND_ADDRESS=127.0.0.1:3000 cargo run
```
2. Connect from dev machine:
```bash
websocat ws://127.0.0.1:3000/terminal/ws
```
3. Type commands and press Enter:
```text
pwd
whoami
cd / && ls
```

Notes:
- Some WS CLIs echo input locally, so you may see each line twice (input echo + shell echo). This is expected in raw tools.

### Android integration (OkHttp example)

Use OkHttp’s WebSocket to stream output and send keystrokes. Treat incoming Binary frames as UTF‑8 when rendering.

```kotlin
val client = OkHttpClient()
val request = Request.Builder()
    .url("ws://<host>:3000/terminal/ws")
    .build()

val listener = object : WebSocketListener() {
    override fun onOpen(ws: WebSocket, response: Response) {
        // Nudge prompt
        ws.send("\n")
    }

    override fun onMessage(ws: WebSocket, text: String) {
        // Some servers may deliver Text; append to UI
        appendToTerminal(text)
    }

    override fun onMessage(ws: WebSocket, bytes: ByteString) {
        val chunk = bytes.string(Charsets.UTF_8)
        appendToTerminal(chunk)
    }

    override fun onClosing(ws: WebSocket, code: Int, reason: String) {
        ws.close(1000, null)
    }

    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        showError(t.message ?: "WS failure")
    }
}

val webSocket = client.newWebSocket(request, listener)

// Sending user input (append "\n" on Enter)
fun sendUserLine(line: String) {
    webSocket.send(line + "\n")
}

// Sending special keys
fun sendCtrlC() { webSocket.send(ByteString.of(0x03)) }
fun sendBackspace() { webSocket.send(ByteString.of(0x7f.toByte())) }
```

UI tips:
- Render output in a monospace, scrollable view. Do not locally echo user input; rely on shell echo to avoid duplicates.
- Append `\n` when the user presses Enter.

### Keepalive (Ping/Pong) from the mobile app

WebSocket ping/pong keeps the connection alive through NAT/firewalls and detects dead peers.

- Server behavior: the server stack responds to ping frames with pong and logs ping/pong events. No app-layer data is affected.
- OkHttp (recommended): configure automatic ping frames on the client.

```kotlin
// OkHttp client with automatic WebSocket ping frames every 30 seconds
val client = OkHttpClient.Builder()
    .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
    .build()

// Reconnect strategy (simple): onFailure, backoff and newWebSocket(...)
override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
    log("ws failure: ${t.message}")
    retryWithBackoff()
}
```

- Ktor client (alternative): set `pingInterval`/`timeout` in the WebSockets plugin and it will handle heartbeats for you.

```kotlin
val client = HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = 30000
        timeout = 60000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
```

Operational notes:
- Choose 15–30s ping interval. Too frequent pings waste battery; too infrequent can let some NATs drop the connection.
- Treat repeated ping timeouts/`onFailure` as a signal to reconnect (with backoff).
- You do not need to send application-level "heartbeat" messages; WebSocket ping/pong is sufficient.

### Error handling and logs

Server logs helpful events:
- Upgrade lifecycle: `"/terminal/ws: upgrade requested"`, `"upgraded; starting PTY bridge"`.
- PTY spawn: working directory and shell, errors like `Permission denied (os error 13)`.
- Byte flow: `PTY: read N bytes`, `WS: sending N bytes`, `WS: recv text/binary` sizes.
- Close reasons and PTY EOF.

Common issues:
- Permission denied on spawn: running as a user without permissions to chosen shell or cwd. The server now uses `$HOME` or `/` to avoid root-only dirs.
- No prompt: send a newline once after connect (the server also nudges with one newline).

### Security (MVP)

- No authentication, no rate limiting, and no resource caps yet. Do not expose publicly as-is. For production, add a token check, idle timeouts, max session count, and run as a constrained user.

### Future extensions

- Window resize support.
- Optional startup command/script (e.g., start directly in a TUI app).
- Simple auth header or session token.
- ANSI/VT parsing on client for better rendering.