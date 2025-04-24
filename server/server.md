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


