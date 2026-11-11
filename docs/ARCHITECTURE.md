# GlidePad Subsystem Architecture

This document details the internal design, component relationships, concurrency models, and state persistence mechanisms of the GlidePad Android and ChromeOS client.

---

## 1. Architectural Layers

GlidePad follows **Clean Architecture** and **MVVM** (Model-View-ViewModel):

```
┌────────────────────────────────────────────────────────┐
│                      UI / View Layer                   │
│   Composable screens, adaptive layouts & event handlers│
└───────────────────────────┬────────────────────────────┘
                            │ User Actions / Gestures
                            ▼
┌────────────────────────────────────────────────────────┐
│                   MainViewModel Layer                  │
│    Orchestrates state, coroutines & sub-engines        │
└──────────────┬────────────────────────────┬────────────┘
               │                            │
               ▼                            ▼
┌─────────────────────────────┐ ┌────────────────────────┐
│      Kinematics & Logic     │ │       Data Layer       │
│  • TouchpadEngine           │ │  • PreferencesManager  │
│  • BenchmarkEngine          │ │  • Firestore Repos     │
└──────────────┬──────────────┘ └────────────────────────┘
               │
               ▼
┌────────────────────────────────────────────────────────┐
│                   I/O & Service Layer                  │
│  • NetworkStreamManager (UDP socket I/O & Heartbeats)  │
│  • InAppReceiverManager (ChromeOS host daemon)         │
│  • BluetoothHidManager (BluetoothProfile.HID_DEVICE)   │
│  • AuthManager (Google CredentialManager & Firebase)   │
└────────────────────────────────────────────────────────┘
```

---

## 2. Component Details

### 2.1. `TouchpadEngine` (`com.example.engine.TouchpadEngine`)
- **Responsibility**: Translates raw multi-touch pointer events into ergonomically smooth cursor trajectories, scroll deltas, and gesture actions.
- **Kinematic Modeling**:
  - Implements **Minimum-Jerk velocity profile** to filter raw touch jitter.
  - Computes instantaneous delta velocity: $v = \frac{\sqrt{dx^2 + dy^2}}{\Delta t}$.
  - Supports 3 selectable acceleration curves:
    - `LINEAR`: Direct $1:1$ linear scaling with sensitivity multiplier.
    - `NATURAL`: Sigmoidal exponential acceleration modeled after modern macOS trackpad mechanics.
    - `AGGRESSIVE`: High-speed ballistic acceleration for multi-monitor desktop navigation.
  - **Ballistic Decay**: Smooth flick inertia when releasing fingers at high speed.
  - **Multi-Touch Gestures**:
    - 1-finger: Movement & accelerated drag-drop.
    - 2-finger: Smooth bi-directional scrolling & tap-to-right-click.
    - 3-finger: Task switching (swipe left/right) and mission control/minimize (swipe up/down).
    - 4-finger: Custom desktop action bindings.

### 2.2. `NetworkStreamManager` (`com.example.service.NetworkStreamManager`)
- **Responsibility**: Manages the low-latency UDP client socket, outgoing packet serializations, retry queues, and inbound pong telemetry.
- **Threading Model**:
  - Outgoing packets are dispatched asynchronously via `Dispatchers.IO`.
  - Inbound packet listener runs on an independent coroutine loop with `soTimeout = 3000ms`.
- **Heartbeat Contention Suppression**:
  - Tracks `lastGestureTransmitTime`. If gestures were sent within 1.2s, heartbeat ping execution is suppressed to avoid socket queue jitter.

### 2.3. `InAppReceiverManager` (`com.example.service.InAppReceiverManager`)
- **Responsibility**: Embedded UDP host receiver server for ChromeOS, Chromebooks, and large-screen Android tablets.
- **Port Management**: Binds to port `8089` (or custom ephemeral port).
- **Live Telemetry**: Computes packets per second (PPS), cumulative packet counts, radar crosshair coordinates, and responds to discovery/heartbeat pings.

### 2.4. `BluetoothHidManager` (`com.example.service.BluetoothHidManager`)
- **Responsibility**: Registers the device as a standard Bluetooth Human Interface Device (HID) peripheral.
- **HID Descriptors**:
  - Standard Mouse Report (4 bytes: buttons, relative X, relative Y, scroll wheel).
  - Standard Keyboard Report (8 bytes: modifier byte, reserved byte, 6 keycodes).
- **Compatibility**: Requires Android 9.0+ (API 28) with `BluetoothProfile.HID_DEVICE`.

### 2.5. Data Persistence & Cloud Sync
- **Local Storage**: `PreferencesManager` uses Jetpack `DataStore` to persist sensitivity, acceleration curves, gesture bindings, and theme preferences.
- **Cloud Storage**: `CloudMacrosRepository` and `PcProfilesRepository` persist user macro decks and saved target PC profiles to Firebase Firestore, protected under user-scoped security rules (`users/{userId}/...`).
- **Authentication**: `AuthManager` implements Google Sign-In through Android Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
