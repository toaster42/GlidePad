package com.example.model

/**
 * Multi-touch gesture patterns recognized on the touchpad surface.
 *
 * @property label Descriptive title shown in settings.
 * @property description Detailed gesture instruction.
 */
enum class GestureType(val label: String, val description: String) {
    ONE_FINGER_TAP("1-Finger Tap", "Single quick tap with one finger"),
    ONE_FINGER_DOUBLE_TAP_DRAG("1-Finger Double Tap & Hold", "Double tap and slide to drag selection or windows"),
    TWO_FINGER_TAP("2-Finger Tap", "Tap with two fingers simultaneously"),
    TWO_FINGER_SCROLL("2-Finger Drag", "Move two fingers vertically or horizontally to scroll"),
    TWO_FINGER_PINCH("2-Finger Pinch / Spread", "Pinch or expand two fingers to zoom"),
    THREE_FINGER_TAP("3-Finger Tap", "Tap with three fingers simultaneously"),
    THREE_FINGER_SWIPE_UP("3-Finger Swipe Up", "Slide three fingers upwards"),
    THREE_FINGER_SWIPE_DOWN("3-Finger Swipe Down", "Slide three fingers downwards"),
    THREE_FINGER_SWIPE_LEFT("3-Finger Swipe Left", "Slide three fingers to the left"),
    THREE_FINGER_SWIPE_RIGHT("3-Finger Swipe Right", "Slide three fingers to the right"),
    FOUR_FINGER_TAP("4-Finger Tap", "Tap with four fingers simultaneously"),
    FOUR_FINGER_SWIPE_LEFT("4-Finger Swipe Left", "Slide four fingers to the left"),
    FOUR_FINGER_SWIPE_RIGHT("4-Finger Swipe Right", "Slide four fingers to the right")
}

/**
 * Host operating system actions triggered upon gesture recognition.
 *
 * @property title Human-readable action label.
 * @property iconName Material icon identifier for UI menus.
 */
enum class GestureAction(val title: String, val iconName: String) {
    NONE("None", "close"),
    LEFT_CLICK("Left Click", "mouse"),
    RIGHT_CLICK("Right Click", "mouse"),
    MIDDLE_CLICK("Middle Click", "mouse"),
    DOUBLE_CLICK("Double Click", "touch_app"),
    DRAG_DROP("Drag & Drop", "pan_tool"),
    SCROLL("Scroll", "swap_vert"),
    ZOOM("Zoom In/Out", "zoom_in"),
    SHOW_DESKTOP("Show Desktop (Win+D)", "desktop_windows"),
    TASK_VIEW("Task View / App Switcher (Win+Tab)", "view_carousel"),
    CYCLE_APPS_FORWARD("Next App (Alt+Tab)", "tab"),
    CYCLE_APPS_BACKWARD("Previous App (Alt+Shift+Tab)", "tab_unselected"),
    SWITCH_DESKTOP_LEFT("Desktop Left (Ctrl+Win+Left)", "arrow_back"),
    SWITCH_DESKTOP_RIGHT("Desktop Right (Ctrl+Win+Right)", "arrow_forward"),
    SEARCH("Search (Win+S)", "search"),
    ACTION_CENTER("Action Center (Win+A)", "notifications"),
    SCREENSHOT("Screenshot (Win+Shift+S)", "screenshot"),
    PLAY_PAUSE("Play / Pause", "play_arrow"),
    MUTE("Mute Audio", "volume_off"),
    VOLUME_UP("Volume Up", "volume_up"),
    VOLUME_DOWN("Volume Down", "volume_down"),
    LOCK_PC("Lock PC (Win+L)", "lock")
}
