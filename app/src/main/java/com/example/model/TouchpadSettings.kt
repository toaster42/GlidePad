package com.example.model

/**
 * Mathematical velocity curve applied by [com.example.engine.TouchpadEngine] to pointer movements.
 *
 * @property displayName User-facing name in settings.
 * @property description Detailed explanation of the kinematic response.
 */
enum class AccelerationProfile(val displayName: String, val description: String) {
    /** 1:1 constant velocity scaling without non-linear curves. Ideal for drawing or precision work. */
    LINEAR(
        "Linear (1:1)",
        "Direct constant speed without speed boosting. Ideal for precision design and gaming."
    ),

    /** Sigmoidal curve with natural micro-damping and progressive speed scaling for flicks. */
    DYNAMIC(
        "Dynamic Smooth",
        "Balanced curve that boosts speed during quick finger flicks while retaining slow control."
    ),

    /** Strong low-speed damping for pixel-perfect targeting and rapid ramp for multi-monitor travel. */
    PRECISION(
        "Precision Pro",
        "Dampens micro-movements for pixel-perfect accuracy with aggressive acceleration for rapid travel across large screens."
    )
}

/**
 * Vibration feedback intensity level for physical clicks, taps, and gesture recognition.
 *
 * @property displayName User-facing level title.
 */
enum class HapticLevel(val displayName: String) {
    OFF("Off"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Crisp")
}

/**
 * Comprehensive user configuration for touchpad behavior, sensitivity, and gesture actions.
 *
 * @property pointerSensitivity Base cursor sensitivity multiplier (0.2x to 4.0x).
 * @property accelerationProfile Selected acceleration curve profile.
 * @property accelerationMultiplier Gain multiplier for non-linear speed boosting.
 * @property scrollSensitivity Multiplier for two-finger scroll speed.
 * @property naturalScroll If true, inverted direction mimics moving content directly (macOS style).
 * @property smoothInertia If true, enables ballistic inertial deceleration on finger lift.
 * @property tapToClick If true, 1-finger quick tap triggers left mouse click.
 * @property dragLock If true, double-tap latch holds mouse button down until explicit release tap.
 * @property twoFingerRightClick If true, 2-finger tap triggers right click context menu.
 * @property hapticLevel Vibration feedback strength.
 * @property showPhysicalButtons If true, displays virtual left/right/middle click buttons below trackpad.
 * @property showScrollStrip If true, displays dedicated vertical scroll bar strip.
 * @property gestures Map associating detected [GestureType] multi-finger movements to [GestureAction] triggers.
 */
data class TouchpadSettings(
    val pointerSensitivity: Float = 1.25f,
    val accelerationProfile: AccelerationProfile = AccelerationProfile.DYNAMIC,
    val accelerationMultiplier: Float = 1.20f,
    val scrollSensitivity: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val smoothInertia: Boolean = true,
    val tapToClick: Boolean = true,
    val dragLock: Boolean = false,
    val twoFingerRightClick: Boolean = true,
    val hapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val showPhysicalButtons: Boolean = true,
    val showScrollStrip: Boolean = true,
    val gestures: Map<GestureType, GestureAction> = defaultGestures()
) {
    companion object {
        /**
         * Returns the standard default multi-finger gesture bindings.
         */
        fun defaultGestures(): Map<GestureType, GestureAction> = mapOf(
            GestureType.ONE_FINGER_TAP to GestureAction.LEFT_CLICK,
            GestureType.ONE_FINGER_DOUBLE_TAP_DRAG to GestureAction.DRAG_DROP,
            GestureType.TWO_FINGER_TAP to GestureAction.RIGHT_CLICK,
            GestureType.TWO_FINGER_SCROLL to GestureAction.SCROLL,
            GestureType.TWO_FINGER_PINCH to GestureAction.ZOOM,
            GestureType.THREE_FINGER_TAP to GestureAction.MIDDLE_CLICK,
            GestureType.THREE_FINGER_SWIPE_UP to GestureAction.TASK_VIEW,
            GestureType.THREE_FINGER_SWIPE_DOWN to GestureAction.SHOW_DESKTOP,
            GestureType.THREE_FINGER_SWIPE_LEFT to GestureAction.CYCLE_APPS_BACKWARD,
            GestureType.THREE_FINGER_SWIPE_RIGHT to GestureAction.CYCLE_APPS_FORWARD,
            GestureType.FOUR_FINGER_TAP to GestureAction.ACTION_CENTER,
            GestureType.FOUR_FINGER_SWIPE_LEFT to GestureAction.SWITCH_DESKTOP_LEFT,
            GestureType.FOUR_FINGER_SWIPE_RIGHT to GestureAction.SWITCH_DESKTOP_RIGHT
        )
    }
}
