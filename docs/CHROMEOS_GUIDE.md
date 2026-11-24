# GlidePad ChromeOS & Googlebook OS Integration Guide

This guide covers running, deploying, and utilizing GlidePad on Chromebooks, Chromeboxes, and ChromeOS convertibles.

---

## 1. Overview

GlidePad is optimized for ChromeOS and large-screen Android devices as a dedicated wireless controller:

- **External Host Controller**: Use your Chromebook's touchscreen or convertible display as a precision wireless touchpad, keyboard, and macro deck to control external Windows, macOS, Linux, or presentation computers and smart TVs over local Wi-Fi UDP (`:8089`) or Bluetooth HID.
- **Desktop & Floating Multitasking**: Adapts seamlessly from full-screen desktop windows down to compact floating corner windows alongside other ChromeOS apps.

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

## 3. Desktop Multi-Window, Freeform Resizing & Float Mode

GlidePad supports dynamic multi-window and freeform resizing across ChromeOS and Android desktop windowing:

- **Default Window Bounds**: `900dp` x `650dp` (comfortable desktop aspect ratio).
- **Minimum Window Bounds**: `240dp` x `180dp` (supports ChromeOS Float windows, splitscreen tiling, and compact freeform sizes).
- **Adaptive Layouts**:
  - **Compact (`COMPACT`, `width < 600dp` outside Float mode)**: Displays the standard bottom `NavigationBar` with the full controller shell.
  - **Expanded (`EXPANDED`, `width >= 600dp` outside Float mode)**: Automatically switches to a vertical Material 3 `NavigationRail` on the left edge.
  - **Float (`FLOAT`)**: Switches to a minimal, full-window touchpad surface.
- **Full-Bleed Dynamic Touchpad Sizing**: The touchpad surface dynamically fills all available width and height in every layout (`COMPACT`, `EXPANDED`, and `FLOAT`) with no maximum width cap.

### Float Window Mode

Float mode turns GlidePad into an always-handy, distraction-free floating touchpad that resizes live with its window:

- **Triggering via ChromeOS or Freeform Resizing**:
  - Float the GlidePad window on ChromeOS using **`Search + Z`** or the **window caption menu**, or resize the window in Android freeform mode.
- **Automatic Size Threshold**:
  - GlidePad enters Float mode automatically whenever `(height < 480dp && width < 600dp) || min(width, height) < 320dp`.
  - Any manual toggle or exit overrides the current state and stays active until a subsequent window resize flips the automatic threshold decision.
- **Manual Controls**:
  - **Enter / Toggle**: Click the **Float** toggle chip in the Touchpad screen's quick-controls bar, or press **`F`** on a physical keyboard.
  - **Exit**: Click the **Exit** overlay button in the top-right corner of Float mode, or press **`Esc`**.
- **Float Layout Behavior**:
  - The touchpad surface fills the entire window dynamically (`240dp x 180dp` minimum and upward) while retaining identical multi-touch gesture and kinematic behavior.
  - Navigation chrome, the connection header, quick-controls bar, and physical button bar are hidden, leaving only a top-left **connection status dot** (emerald/green = connected/streaming, amber = pairing, rose = disconnected) and a top-right **exit button** overlay.
  - Your previously active screen is preserved underneath and restored when you exit Float mode.

---

## 4. Physical Keyboard Shortcuts

When running on ChromeOS or Android with a physical keyboard:

- `1` : Switch to **Touchpad** screen
- `2` : Switch to **Keyboard** screen
- `3` : Switch to **Quick Macros** screen
- `4` : Switch to **Live Preview** screen
- `5` : Switch to **Settings & Tuning** screen
- `F` : Toggle **Float** mode
- `Esc` : Dismiss open connection/modal dialog; otherwise exit **Float** mode if active; otherwise return to **Touchpad**
