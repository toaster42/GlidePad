package com.example.model

enum class AccelerationProfile(val displayName: String, val description: String) {
    LINEAR(
        "Linear (1:1)",
        "Direct constant speed without speed boosting. Ideal for precision design and gaming."
    ),
    DYNAMIC(
        "Dynamic Smooth",
        "Balanced curve that boosts speed during quick finger flicks while retaining slow control."
    ),
    PRECISION(
        "Precision Pro",
        "Dampens micro-movements for pixel-perfect accuracy with aggressive acceleration for rapid travel across large screens."
    )
}

enum class HapticLevel(val displayName: String) {
    OFF("Off"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Crisp")
}

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
