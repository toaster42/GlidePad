# GlidePad Subsystem Architecture

This document details the internal design, component relationships, concurrency models, and state persistence mechanisms of the GlidePad Android and ChromeOS client.

---

## 1. Architectural Layers

GlidePad follows **Clean Architecture** and **MVVM** (Model-View-ViewModel):

```
┌────────────────────────────────────────────────────────┐
│                      UI / View Layer                   │
│  • MainLayoutFrame (COMPACT / EXPANDED / FLOAT shell)  │
│  • TouchpadSurface & FloatModeOverlay                  │
│  • WindowLayoutPolicy & resolveShortcut()              │
│  • Composable screens & modal dialogs                  │
└───────────────────────────┬────────────────────────────┘
                            │ User Actions / Gestures
                            ▼
┌────────────────────────────────────────────────────────┐
│                   MainViewModel Layer                  │
│  • Orchestrates state, coroutines & sub-engines        │
│  • FloatModeController (auto + manual Float override)  │
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

### 2.3. `BluetoothHidManager` (`com.example.service.BluetoothHidManager`)
- **Responsibility**: Registers the device as a standard Bluetooth Human Interface Device (HID) peripheral.
- **HID Descriptors**:
  - Standard Mouse Report (4 bytes: buttons, relative X, relative Y, scroll wheel).
  - Standard Keyboard Report (8 bytes: modifier byte, reserved byte, 6 keycodes).
- **Compatibility**: Requires Android 9.0+ (API 28) with `BluetoothProfile.HID_DEVICE`.

### 2.4. Adaptive UI, Layout & Float Mode (`com.example.ui.layout`, `com.example.ui.components`)
- **`WindowLayoutPolicy` (`com.example.ui.layout.WindowLayoutPolicy`)**:
  - Pure window-size policy resolving `LayoutMode` (`COMPACT`, `EXPANDED`, or `FLOAT`) from dp dimensions:
    - `FLOAT`: `(heightDp < 480 && widthDp < 600) || min(widthDp, heightDp) < 320`
    - `EXPANDED`: `widthDp >= 600` (outside Float mode)
    - `COMPACT`: all other sizes
  - Also exposes `resolveNonFloat(widthDp)` so exiting Float mode on a small window falls back to `EXPANDED` or `COMPACT` cleanly.
- **`FloatModeController` (`com.example.ui.layout.FloatModeController`)**:
  - Pure Kotlin state holder exposing `isFloat: StateFlow<Boolean>`.
  - Re-evaluates automatic Float status on `onWindowSize(widthDp, heightDp)`. Manual `toggle()` and `exitFloat()` overrides persist across resizes while the automatic policy result stays unchanged, and clear automatically whenever a resize flips the automatic decision.
- **`MainLayoutFrame` (`com.example.ui.layout.MainLayoutFrame`)**:
  - Stateless root shell composable rendering the `EXPANDED` `NavigationRail` + `ConnectionHeader`, the `COMPACT` `Scaffold` + bottom `NavigationBar`, or full-window `floatContent` in `FLOAT` mode while preserving the active `AppScreen` underneath.
- **`TouchpadSurface` (`com.example.ui.components.TouchpadSurface`)**:
  - Shared multi-touch `pointerInput` surface, finger-ring `Canvas`, crosshair, and optional gesture-toast overlay used by both `TouchpadScreen` and `FLOAT` mode. Fills all incoming layout constraints with no maximum width cap, ensuring identical gesture and `TouchpadEngine` behavior across all window sizes.
- **`FloatModeOverlay` (`com.example.ui.components.FloatModeOverlay`)**:
  - Lightweight overlay shown over `TouchpadSurface` in `FLOAT` mode, displaying a top-start connection status dot (emerald = connected/streaming, amber = pairing, rose = disconnected) and a top-end exit button (`>= 48dp` touch target) without reading pointer hot-path state.
- **`resolveShortcut` (`com.example.ui.layout.KeyboardShortcuts`)**:
  - Pure key-mapping function returning `ShortcutAction`: `1`–`5` select `TOUCHPAD`, `KEYBOARD`, `MACROS`, `VIRTUAL_SCREEN`, and `SETTINGS`; `F` toggles Float mode; `Esc` closes an open dialog, else exits Float mode, else returns to `TOUCHPAD`.

### 2.5. Data Persistence & Cloud Sync
- **Local Storage**: `PreferencesManager` uses Jetpack `DataStore` to persist sensitivity, acceleration curves, gesture bindings, and theme preferences.
- **Cloud Storage**: `CloudMacrosRepository` and `PcProfilesRepository` persist user macro decks and saved target PC profiles to Firebase Firestore, protected under user-scoped security rules (`users/{userId}/...`).
- **Authentication**: `AuthManager` implements Google Sign-In through Android Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
