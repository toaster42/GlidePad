# GlidePad ChromeOS & Googlebook OS Integration Guide

This guide covers running, deploying, and utilizing GlidePad on Chromebooks, Chromeboxes, and ChromeOS convertibles.

---

## 1. Overview & Dual-Role Capabilities

GlidePad is fully optimized for ChromeOS and large-screen Android devices:

1. **Remote Controller**: The Chromebook functions as an oversized touchpad/keyboard controller to remotely control external computers, smart TVs, or presentation rigs.
2. **In-App Host Receiver**: The Chromebook operates as the host receiver. Android phones can stream trackpad gestures to the Chromebook over the local Wi-Fi network without requiring a terminal or Python background script.

---

## 2. Hardware Manifest Adaptations

Standard Android manifest rules implicitly require touchscreen and Bluetooth hardware. To ensure GlidePad is installable across all ChromeOS laptops, convertibles, and desktop Chromeboxes, `AndroidManifest.xml` explicitly relaxes these requirements:

```xml
<!-- Hardware features declared non-mandatory for ChromeOS / desktop Play Store compatibility -->
<uses-feature
    android:name="android.hardware.touchscreen"
    android:required="false" />
<uses-feature
    android:name="android.hardware.faketouch"
    android:required="false" />
<uses-feature
    android:name="android.hardware.bluetooth"
    android:required="false" />
```

---

## 3. Desktop Multi-Window & Freeform Resizing

GlidePad supports dynamic multi-window resizing:
- **Default Window Bounds**: `900dp` x `650dp` (comfortable desktop aspect ratio).
- **Minimum Window Bounds**: `420dp` x `480dp` (supports splitscreen tiling).
- **Adaptive Navigation**:
  - In compact phone view (`maxWidth < 600dp`): Displays standard bottom `NavigationBar`.
  - In expanded desktop view (`maxWidth >= 600dp`): Automatically switches to a vertical Material 3 `NavigationRail` on the left edge.
- **Trackpad Ergonomics**: The trackpad surface is constrained to a maximum width of `1000dp` (`widthIn(max = 1000.dp)`) to prevent awkward arm-stretch fatigue on ultra-wide desktop monitors.

---

## 4. Physical Keyboard Shortcuts

When running on ChromeOS with a physical keyboard:
- `1` : Switch to **Touchpad** screen
- `2` : Switch to **Keyboard** screen
- `3` : Switch to **Quick Macros** screen
- `4` : Switch to **Live Preview** screen
- `5` : Switch to **Host Receiver** screen
- `6` : Switch to **Settings & Tuning** screen
- `Esc` : Dismiss connection dialog or return to Touchpad

---

## 5. Using the In-App Receiver

1. Open GlidePad on your Chromebook and navigate to the **Host Receiver** tab (or press key `5`).
2. Click **Start Receiver**.
3. The screen will display:
   - Your Chromebook's local IP address (with a one-tap copy button).
   - The listening UDP port (`8089`).
   - Live **Throughput (Hz)**, total delivered packets, and active client count.
   - An interactive **Radar Pointer HUD** showing the incoming pointer coordinates and button clicks in real time.
4. On your mobile phone, open GlidePad and connect to your Chromebook's IP address.
