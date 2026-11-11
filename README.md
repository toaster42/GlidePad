# GlidePad: Ultra-Low-Latency Wireless Trackpad & Remote Control

[![Android CI](https://github.com/aistudio/glidepad/actions/workflows/ci.yml/badge.svg)](https://github.com/aistudio/glidepad/actions/workflows/ci.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20ChromeOS-00E5FF.svg)](https://developer.android.com)
[![Protocol](https://img.shields.io/badge/Protocol-UDP%20%2F%20Bluetooth%20HID-7000FF.svg)](#wire-protocol-specifications)
[![License](https://img.shields.io/badge/License-Apache%202.0-10B981.svg)](LICENSE)

**GlidePad** transforms your Android phone, tablet, or Chromebook into an ultra-low-latency wireless trackpad, keyboard, and macro deck for Windows, macOS, Linux, and ChromeOS hosts. Engineered for high performance, GlidePad supports dual transmission modes: **Local Wi-Fi UDP Streaming** (sub-5ms packet turnaround with Flash & Hogan minimum-jerk kinematics) and **Standard Bluetooth HID**.

---

## Table of Contents

- [Key Capabilities](#key-capabilities)
- [System Architecture](#system-architecture)
- [Cross-Platform Quickstart](#cross-platform-quickstart)
  - [1. Mobile App (Controller)](#1-mobile-app-controller)
  - [2. Python PC Receiver (Host)](#2-python-pc-receiver-host)
  - [3. ChromeOS Dual-Role Mode](#3-chromeos-dual-role-mode)
- [Wire Protocol Specifications](#wire-protocol-specifications)
- [Google Sign-In & Play App Signing Setup](#google-sign-in--play-app-signing-setup)
- [Technical Documentation Index](#technical-documentation-index)
- [Building & Testing](#building--testing)
- [Troubleshooting](#troubleshooting)

---

## Key Capabilities

- **Kinematic Multi-Touch Engine**: Minimum-jerk trajectory modeling (Flash & Hogan 1985) with wrist-pivot curvature, natural tremor smoothing, and ballistic flick decay.
- **Hardware-Level Precision**: Supports 1-finger accelerated pointer, 2-finger smooth inertial scrolling, 3-finger app switching, and customizable 4-finger gestures.
- **Dual Connectivity**:
  - **Local Wi-Fi (UDP :8089)**: Microsecond packet streaming with active gesture heartbeat suppression to prevent socket queue congestion.
  - **Bluetooth HID Profile**: Native driverless pairing with PCs, Smart TVs, and presentation displays without companion software.
- **Full Keyboard & Hotkey Latch Deck**: Virtual keyboard with latching sticky modifier keys (`Ctrl`, `Alt`, `Shift`, `Super/Win`), media controls, and quick macros.
- **Cloud Macro Sync**: Secure Firebase Firestore synchronization for custom macro decks and per-device PC network profiles.
- **ChromeOS & Large-Screen Optimization**:
  - Resizable freeform desktop window support with auto-adaptive Material 3 `NavigationRail`.
  - Built-in **In-App UDP Receiver (`:8089`)** allowing Chromebooks to act as the receiving host for other phones.
  - Physical mouse hover, scroll wheel, and keyboard shortcut listeners (`1`-`6` tab switching, `Esc` navigation).
- **Network Benchmark Engine**: Live round-trip latency (RTT) measurement, jitter analysis, throughput gauge (`Hz`), and packet loss reporting.

---

## System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        GLIDEPAD SYSTEM TOPOLOGY                        │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                     Jetpack Compose UI Layer                     │  │
│  │  • TouchpadScreen          • KeyboardScreen                      │  │
│  │  • MacrosScreen            • VirtualPreviewScreen                │  │
│  │  • ReceiverDashboardScreen • ConnectionDialog                    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │ User Input Events                │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                 Presentation & State Management                  │  │
│  │  • MainViewModel (Coroutines, StateFlows, Preferences)           │  │
│  │  • TouchpadEngine (Minimum-jerk filtering, acceleration curves)  │  │
│  │  • BenchmarkEngine (Synthetic stress test & jitter analytics)    │  │
│  └───────────────────┬──────────────────────────────┬───────────────┘  │
│                      │                              │                  │
│         Bluetooth HID│                  Wi-Fi UDP   │                  │
│                      ▼                              ▼                  │
│  ┌─────────────────────────┐  ┌──────────────────────────────────────┐ │
│  │ BluetoothHidManager     │  │ NetworkStreamManager                 │ │
│  │ • Android BluetoothProfile│  │ • Non-blocking DatagramSocket (8089) │ │
│  │ • 4-byte Mouse HID Rept │  │ • Ephemeral client socket binding    │ │
│  │ • 8-byte Keyboard Rept  │  │ • Active-gesture ping suppression    │ │
│  └─────────────────────────┘  └──────────────────┬───────────────────┘ │
└──────────────────────────────────────────────────┼─────────────────────┘
                                                   │ UDP Datagrams (:8089)
                                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        RECEIVER & HOST TARGETS                         │
│                                                                        │
│  ┌─────────────────────────────────┐  ┌──────────────────────────────┐ │
│  │ Python PC Receiver (v2.3)       │  │ ChromeOS In-App Receiver     │ │
│  │ • pynput mouse & keyboard hooks │  │ • InAppReceiverManager daemon│ │
│  │ • Windows SendInput API         │  │ • Live Radar Coordinate HUD  │ │
│  │ • Linux uinput / X11 / Wayland  │  │ • Real-time PPS & Loss Stats │ │
│  │ • macOS Quartz Event Taps       │  │ • Bidirectional Ping Echo    │ │
│  └─────────────────────────────────┘  └──────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Cross-Platform Quickstart

### 1. Mobile App (Controller)

1. Build and install the APK on your Android device (or download from the GitHub Releases page):
   ```bash
   gradle :app:assembleDebug
   ```
2. Open **GlidePad** on your phone.
3. Tap the **Wi-Fi / Connection pill** in the top header to open the Connection Dialog.
4. Enter the local IP address of your host computer (e.g. `192.168.1.120`).

### 2. Python PC Receiver (Host)

The PC receiver script runs on the target computer (Windows, macOS, or Linux). It listens on UDP port `8089` and translates incoming network packets into OS input events.

#### Requirements
- Python 3.8+
- `pynput` library

#### Setup
```bash
# Clone the repository
git clone https://github.com/aistudio/glidepad.git
cd glidepad

# Create a virtual environment (recommended)
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install pynput

# Launch the Ultra-Low-Latency PC Receiver (v2.3)
python3 glidepad_receiver_v2_3.py
```

*For Linux systems using Wayland or without root input access, consult the [Python Receiver Guide](docs/PYTHON_RECEIVERS.md).*

### 3. ChromeOS Dual-Role Mode

On Chromebooks, GlidePad works in two distinct modes:

1. **Controller Mode**: Use your Chromebook's touchscreen or physical keyboard to remote-control another device. The UI automatically switches to an ergonomic **NavigationRail** on the left edge.
2. **Host Receiver Mode**: Turn your Chromebook into the receiving host!
   - Open GlidePad on your Chromebook and navigate to the **Host Receiver** tab (or press key `5`).
   - Click **Start Receiver**. Your Chromebook starts an embedded UDP daemon on port `8089`.
   - On your phone, enter your Chromebook's displayed IP to control it wirelessly with real-time pointer HUD feedback.

---

## Wire Protocol Specifications

GlidePad uses a compact, low-overhead JSON-over-UDP datagram protocol.

### 1. Pointer Motion (`mouse`)
```json
{
  "type": "mouse",
  "dx": 12,
  "dy": -6,
  "btn": 1,
  "wheel": 0,
  "time": 1731320000000
}
```
- `dx`, `dy`: Integer coordinate deltas from kinematic filter.
- `btn`: Button bitmask (`0`=None, `1`=Left Click, `2`=Right Click, `4`=Middle Click).
- `wheel`: Integer scroll steps (positive=up, negative=down).
- `time`: Unix millisecond timestamp for latency & jitter telemetry.

### 2. Keyboard Event (`keyboard`)
```json
{
  "type": "keyboard",
  "text": "Hello World",
  "keys": ["ctrl", "c"]
}
```

### 3. Heartbeat & Discovery (`ping` / `pong`)
```json
// Phone -> PC Ping
{"type": "ping", "client_port": 54321, "time": 1731320000000}

// PC -> Phone Pong
{"type": "pong", "name": "Workstation (Linux)", "status": "ok", "echo_time": 1731320000000}
```

*For full packet schemas, validation rules, and error handling, see [docs/WIRE_PROTOCOL.md](docs/WIRE_PROTOCOL.md).*

---

## Google Sign-In & Play App Signing Setup

If cloud synchronization fails with:
```
Sign-in error: [16] Account reauth failed
```
This indicates that the **SHA-1 certificate fingerprint** of the active APK signing key is missing from your Firebase / Google Cloud Console OAuth credentials.

### Resolving Error 16 for Google Play Releases

When an app is distributed via the Google Play Store, **Google Play App Signing** replaces your private upload key with a Google-managed production signing key:

1. Open the [Google Play Console](https://play.google.com/console).
2. Select your app and go to **Release > Setup > App integrity**.
3. Under the **App signing key certificate** tab, copy the **SHA-1 certificate fingerprint**.
4. Open the [Firebase Console](https://console.firebase.google.com/) for your project:
   - Go to **Project Settings > General**.
   - Under **Your apps > Android app (`com.aistudio.glidepad.kxvtwm`)**, click **Add fingerprint**.
   - Paste the Google Play App Signing SHA-1 fingerprint.
5. In `app/src/main/res/values/strings.xml`, verify that `default_web_client_id` contains the OAuth 2.0 **Web client ID** (not the Android client ID).

---

## Technical Documentation Index

| Guide | Description |
|---|---|
| [System Architecture](docs/ARCHITECTURE.md) | In-depth breakdown of `TouchpadEngine`, coroutine threading models, and state flows |
| [Wire Protocol Specs](docs/WIRE_PROTOCOL.md) | Complete byte-level and JSON datagram specifications for all packet types |
| [Python Receivers](docs/PYTHON_RECEIVERS.md) | PC receiver scripts, permissions setup, benchmark receiver, and integration tests |
| [ChromeOS Guide](docs/CHROMEOS_GUIDE.md) | Multi-window desktop adaptation, NavigationRail layouts, and embedded host receiver |

---

## Building & Testing

### Prerequisites
- JDK 17
- Android SDK (API 36 / Android 16)
- Python 3.8+ (for companion scripts & loopback tests)

### Commands

```bash
# Compile and build the debug APK
gradle :app:assembleDebug

# Run unit tests and Robolectric tests (JVM)
gradle :app:testDebugUnitTest

# Run the realistic humanlike touch loopback integration test suite (Python)
python3 test_receiver_integration.py

# Verify Python script syntax
python3 -m py_compile *.py

# Generate interactive Dokka HTML documentation
gradle dokkaGenerateHtml
```

---

## Troubleshooting

- **Cursor moves too fast / slow**: Open **Settings & Tuning** in the app to adjust the sensitivity multiplier (0.2x – 4.0x) or switch between **Linear**, **Natural**, and **Aggressive** acceleration curves.
- **Wi-Fi connection shows offline**:
  1. Confirm your phone and PC are connected to the same Wi-Fi network or mobile hotspot.
  2. Verify that your PC firewall allows incoming UDP packets on port `8089`.
  3. Ensure the receiver script is running (`python3 glidepad_receiver_v2_3.py`).
- **Heartbeat lagging trackpad**: GlidePad incorporates automatic active-gesture suppression—heartbeat pings are throttled during active continuous gestures to ensure zero touch packet contention.
