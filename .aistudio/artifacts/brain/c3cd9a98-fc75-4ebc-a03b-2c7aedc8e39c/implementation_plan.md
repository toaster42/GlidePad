# ChromeOS & Googlebook OS Compatibility Architecture

An architectural plan to adapt GlidePad for ChromeOS, Google Chromebooks, and large-screen multi-window desktop environments. This enables GlidePad to function seamlessly both as a high-precision remote controller and as a native in-app receiver on Chromebooks, featuring desktop window resizing, adaptive side NavigationRail navigation, optional touchscreen manifests, and physical input handling.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the interactive clarification interview:

- **Confirmed Decision 1 (Dual-Role Device Operation)**: GlidePad on ChromeOS will support dual operating modes:
  1. *Controller Mode*: The Chromebook controls external PCs, TVs, or presentations via Wi-Fi/Bluetooth.
  2. *In-App Receiver Mode*: The Chromebook acts as the host receiver, allowing phones or other devices to connect, stream trackpad coordinates, and control the Chromebook with live HUD telemetry.
- **Confirmed Decision 2 (Adaptive Multi-Window Desktop Layout)**: The interface will adapt dynamically between compact mobile layouts and expanded Chromebook desktop windows using a vertical Material 3 `NavigationRail` alongside wide trackpad and split-pane control surfaces.
- **Confirmed Decision 3 (Play Store Hardware Manifest Accessibility)**: Explicitly mark `android.hardware.touchscreen`, `android.hardware.faketouch`, and `android.hardware.bluetooth` as optional (`android:required="false"`) so the app is installable across all ChromeOS laptops, convertibles, and desktop Chromeboxes.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms GlidePad into a native ChromeOS desktop citizen. The app runs smoothly in resizable freeform windows, supports physical mouse hover and hardware keyboard shortcuts, adapts to laptop aspect ratios with an ergonomic side `NavigationRail`, and includes a built-in *In-App Receiver* dashboard for receiving remote trackpad streams directly on ChromeOS.
- **Target Audience / Persona**: Chromebook users, students, developers, and presenters who use ChromeOS laptops/convertibles and want to either remote-control desktop rigs or use their Chromebook as the display/receiver station.
- **Key Value**: Unlocks universal ChromeOS compatibility and Play Store discoverability, eliminating touch-hardware restrictions and providing a desktop-native input experience.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Freeform Window Resizing & Desktop Maximization**:
   - The user opens GlidePad on ChromeOS. The window starts with a comfortable desktop aspect ratio (e.g., 900x650dp) and can be freely resized, snapped to splitscreen, or maximized.
   - When window width is `>= 600dp` (Expanded/Medium window class), the bottom `NavigationBar` transitions into a sleek, vertical Material 3 `NavigationRail` on the left edge.
   - In Expanded mode, settings and preview screens utilize two-pane layouts to prevent awkward horizontal whitespace stretching.

2. **In-App Receiver Mode (Chromebook as Target Host)**:
   - A dedicated "Host Receiver" tab or toggle in the Connection header allows the Chromebook to listen for incoming UDP packets on port 8089.
   - Displays the Chromebook's local IP, active connection status, real-time packet rate (Hz), latency graph, and a virtual canvas with visual pointer coordinates.

3. **Physical Mouse & Keyboard Integration**:
   - On ChromeOS with a physical trackpad or mouse attached, the trackpad canvas responds to mouse cursor hover and scroll wheel deltas without blocking native window management.
   - Hardware keyboard shortcuts (e.g., `Esc` to disconnect, `1-4` for quick tab switching, `Space` for presentation advance) are supported.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech Cyberpunk Dark UI consistent with GlidePad's neon aesthetic (`#0A0E17` obsidian background with `#00F0FF` electric cyan and `#7000FF` purple accents).
- **NavigationRail**:
  - Anchored to the left edge with translucent glassmorphic backdrop (`SurfaceDark900` with 1dp border).
  - Displays branded header badge ("GLIDEPAD OS"), active neon pill indicators, and a persistent connection health pill.
- **Trackpad Canvas on Large Screens**:
  - Centers with an ergonomic bounding box (e.g., max width 960dp) flanked by secondary macro panels or quick actions, preventing unergonomic arm strain on ultra-wide screens.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Android Manifest Hardware Feature Declarations**
  - *Chosen Approach*: Add `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`, `<uses-feature android:name="android.hardware.faketouch" android:required="false" />`, and set `android:resizeableActivity="true"`.
  - *Why*: Standard Android manifest defaults implicitly require a touchscreen when touch gestures are used. Without this, Google Play filters out non-touch Chromebooks and Chromeboxes.
  - *Alternatives Considered*: Leaving manifest defaults untouched (would block up to 50% of ChromeOS device installs on Play Store).

- **Decision 2: Adaptive Navigation Component Architecture**
  - *Chosen Approach*: Responsive composable wrapper checking window width (`BoxWithConstraints` or `calculateWindowSizeClass`) that chooses between `NavigationBar` (compact width `< 600dp`) and `NavigationRail` (expanded width `>= 600dp`).
  - *Why*: Bottom navigation bars on 14"–16" Chromebook displays stretch icons hundreds of pixels apart, causing severe ergonomic degradation. Navigation rails are the Google Material 3 canonical pattern for ChromeOS and desktop layouts.

- **Decision 3: Native In-App Kotlin Receiver vs. External Python Runner**
  - *Chosen Approach*: Implement an in-app Kotlin UDP receiver daemon inside `NetworkStreamManager` for ChromeOS.
  - *Why*: While Windows/macOS/Linux users run `glidepad_receiver_v2_3.py`, ChromeOS users running the Android APK cannot easily execute background Python terminal daemons without developer mode / Crostini Linux container. A built-in Kotlin receiver allows instant zero-config pairing between phones and Chromebooks.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        GlidePad on ChromeOS                            │
│                                                                        │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Window Size Sensing   │  │ Hardware Manifest                     │  │
│  │ (Compact vs Expanded) │  │ • touchscreen: optional               │  │
│  └───────────┬───────────┘  │ • resizeableActivity="true"           │  │
│              │              └───────────────────────────────────────┘  │
│              ▼                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Responsive Shell (MainActivity / Scaffold)                       │  │
│  │                                                                  │  │
│  │  [Width < 600dp]                 [Width >= 600dp]                │  │
│  │  Bottom NavigationBar            Left NavigationRail             │  │
│  │  Full-width Single Pane          Two-Pane Master-Detail          │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│              ┌──────────────────────┴───────────────────────┐          │
│              ▼                                              ▼          │
│  ┌─────────────────────────┐                ┌────────────────────────┐ │
│  │ Controller Engine       │                │ In-App Receiver Engine │ │
│  │ • Touchpad / Keyboard   │                │ • UDP Socket :8089     │ │
│  │ • Physical Key Shortcuts│                │ • Packet Deserializer  │ │
│  │ • Outgoing UDP Streams  │                │ • Live Telemetry Canvas│ │
│  └─────────────────────────┘                └────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Manifest Updates Required

```xml
<!-- Hardware features declared non-mandatory for ChromeOS / desktop Play Store compatibility -->
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />
<uses-feature android:name="android.hardware.faketouch" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />

<!-- Activity multi-window and freeform desktop configuration -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:resizeableActivity="true"
    android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"
    android:windowSoftInputMode="adjustResize" ...>
    <layout
        android:defaultWidth="900dp"
        android:defaultHeight="650dp"
        android:minWidth="420dp"
        android:minHeight="480dp" />
</activity>
```

### Module Refactoring Plan

1. **`AndroidManifest.xml`**:
   - Add `<uses-feature>` declarations marking touchscreen, faketouch, and bluetooth as optional.
   - Declare `android:resizeableActivity="true"`, desktop layout dimensions (`defaultWidth`, `defaultHeight`, `minWidth`, `minHeight`).
   - Add `smallestScreenSize` to `configChanges` to handle smooth window resizing on ChromeOS.

2. **`MainActivity.kt` & Navigation UI**:
   - Introduce responsive layout detection using available constraints (`BoxWithConstraints`).
   - When `maxWidth >= 600.dp`, render a side `NavigationRail` with desktop-optimized layout; when `< 600.dp`, preserve the bottom `NavigationBar`.
   - Add hardware keyboard shortcuts handler (`onKeyEvent`).

3. **`NetworkStreamManager.kt` & `InAppReceiverManager.kt`**:
   - Implement the in-app UDP receiver server capable of listening on port 8089 on localhost / LAN interface.
   - Provide real-time reception telemetry and packet statistics to the UI.

4. **`ReceiverDashboardScreen.kt`**:
   - Add a dedicated In-App Receiver screen displaying local IP, listening port, connected remote client status, packet loss rate, and live coordinates HUD.
