# GlidePad Wire Protocol Specifications

This document defines the formal network transport and serialization specification for GlidePad. All communication between GlidePad controllers and host receivers occurs over UDP datagrams.

---

## 1. Network Transport Model

- **Transport**: User Datagram Protocol (UDP).
- **Default Port**: `8089` (Host Receiver listening port).
- **Client Port**: Dynamic ephemeral port bound by `NetworkStreamManager` (`DatagramSocket(0)`), transmitted in `client_port` fields for bidirectional responses.
- **Buffer Size**: Standard UDP datagrams under 1,024 bytes (well within the typical 1,472-byte Ethernet MTU threshold to prevent IP packet fragmentation).
- **Encoding**: UTF-8 encoded JSON strings.

---

## 2. Packet Types & Schemas

### 2.1. Mouse Motion & Button Events (`type: "mouse"`)

Sent at up to 120Hz during active multi-touch gestures on the trackpad.

#### Schema
```json
{
  "type": "mouse",
  "dx": <integer>,
  "dy": <integer>,
  "btn": <integer>,
  "wheel": <integer>,
  "time": <integer_ms>
}
```

#### Field Definitions
| Field | Type | Description | Bounds / Values |
|---|---|---|---|
| `type` | String | Fixed packet identifier. | `"mouse"` |
| `dx` | Integer | Relative horizontal pixel delta calculated by kinematics filter. | `-1000` to `1000` |
| `dy` | Integer | Relative vertical pixel delta calculated by kinematics filter. | `-1000` to `1000` |
| `btn` | Integer | Mouse button state bitmask. | `0` (None), `1` (Left), `2` (Right), `4` (Middle) |
| `wheel` | Integer | Discrete scroll wheel delta. | Positive (Scroll Up), Negative (Scroll Down) |
| `time` | Long | Client timestamp in milliseconds (`System.currentTimeMillis()`). | Standard Unix epoch ms |

#### Example
```json
{"type": "mouse", "dx": 18, "dy": -7, "btn": 0, "wheel": 0, "time": 1731321045120}
```

---

### 2.2. Keyboard Text & Key Events (`type: "keyboard"`)

Dispatched when typing text or executing hotkey/macro sequences.

#### Schema
```json
{
  "type": "keyboard",
  "text": <string>,
  "keys": [<string>, ...]
}
```

#### Field Definitions
| Field | Type | Description |
|---|---|---|
| `type` | String | Fixed packet identifier: `"keyboard"`. |
| `text` | String | Direct text string to type into host applications (supports UTF-8 characters). |
| `keys` | Array<String> | List of virtual key codes to press and release in sequence (e.g. `["ctrl", "c"]`). |

#### Supported Key Tokens
- Modifiers: `"ctrl"`, `"alt"`, `"shift"`, `"win"`, `"cmd"`, `"super"`
- Navigation: `"up"`, `"down"`, `"left"`, `"right"`, `"home"`, `"end"`, `"pageup"`, `"pagedown"`
- Actions: `"enter"`, `"backspace"`, `"tab"`, `"esc"`, `"delete"`, `"space"`
- Media: `"play_pause"`, `"vol_up"`, `"vol_down"`, `"mute"`, `"next"`, `"prev"`

#### Example
```json
{"type": "keyboard", "text": "", "keys": ["ctrl", "shift", "esc"]}
```

---

### 2.3. Heartbeat Ping & Pong (`type: "ping" | "pong"`)

Monitors connection liveness and calculates bidirectional round-trip latency (RTT).

#### Ping Packet (Phone -> Receiver)
```json
{
  "type": "ping",
  "client_port": 58492,
  "time": 1731321045150
}
```

#### Pong Packet (Receiver -> Phone)
```json
{
  "type": "pong",
  "name": "Alex PC (Linux)",
  "status": "ok",
  "echo_time": 1731321045150,
  "server_time": 1731321045152
}
```

#### Heartbeat Suppression Invariant
To prevent network contention and socket queue latency on 2.4GHz Wi-Fi:
- If the trackpad has experienced motion packets within the last **1,200 ms**, the client background heartbeat timer **suppresses outgoing ping packets**.
- Heartbeat pings resume automatically once the trackpad enters an idle state.

---

### 2.4. Local Network Discovery (`type: "discover" | "announce"`)

Broadcast discovery mechanism over the local subnet (e.g. `255.255.255.255:8089`).

#### Discover (Phone Broadcast)
```json
{
  "type": "discover",
  "client_port": 58492,
  "time": 1731321045200
}
```

#### Announce (Host Receiver Reply)
```json
{
  "type": "announce",
  "name": "GlidePad PC Receiver",
  "status": "ok",
  "port": 8089
}
```

---

### 2.5. Synthetic Benchmark Packets (`type: "benchmark_ping" | "benchmark_pong"`)

High-rate telemetry stream for stress-testing network throughput, packet loss, and jitter.

#### Benchmark Ping (Phone -> Receiver)
```json
{
  "type": "benchmark_ping",
  "seq": 104,
  "client_port": 58492,
  "time": 1731321045300
}
```

#### Benchmark Pong (Receiver -> Phone)
```json
{
  "type": "benchmark_pong",
  "seq": 104,
  "server_time": 1731321045302
}
```
