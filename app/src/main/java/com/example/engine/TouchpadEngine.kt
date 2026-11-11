package com.example.engine

import com.example.model.AccelerationProfile
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.TouchpadSettings
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Core kinematics and gesture engine for GlidePad trackpad operations.
 *
 * Translates raw multi-touch screen coordinates into filtered, accelerated mouse cursor movements,
 * natural two-finger scrolling, and multi-finger desktop gesture invocations. Employs sub-pixel
 * remainder accumulation to prevent cursor stair-stepping or rounding truncation during low-speed
 * precision positioning.
 *
 * @param settings Current [TouchpadSettings] configuration controlling sensitivity, curves, and mappings.
 * @param onSendMouse Callback dispatched when computed mouse reports are ready for network/HID transport.
 * @param onTriggerAction Callback dispatched when multi-finger gestures match configured [GestureAction] rules.
 */
class TouchpadEngine(
    private var settings: TouchpadSettings,
    private val onSendMouse: (buttons: Byte, dx: Int, dy: Int, wheel: Int) -> Unit,
    private val onTriggerAction: (GestureAction) -> Unit
) {
    // Sub-pixel accumulator to prevent jitter during slow precision movements
    private var subPixelRemainderX = 0f
    private var subPixelRemainderY = 0f
    private var subPixelRemainderWheel = 0f

    // Current mouse button bitmask (1 = Left, 2 = Right, 4 = Middle)
    private var currentButtonsMask: Byte = 0
    private var isDragging = false

    /**
     * Updates active engine settings without resetting sub-pixel accumulator state.
     *
     * @param newSettings Updated [TouchpadSettings] reflecting latest user adjustments.
     */
    fun updateSettings(newSettings: TouchpadSettings) {
        this.settings = newSettings
    }

    /**
     * Computes the accelerated mouse displacement based on user sensitivity and acceleration settings.
     */
    fun processPointerMove(rawDx: Float, rawDy: Float, deltaTimeMs: Long) {
        if (rawDx == 0f && rawDy == 0f) return

        val dt = (deltaTimeMs.coerceAtLeast(1)).toFloat() / 1000f // in seconds
        val distance = sqrt(rawDx * rawDx + rawDy * rawDy)
        val speedPxPerSec = distance / dt

        // Base sensitivity multiplier
        val sensitivity = settings.pointerSensitivity

        // Calculate acceleration multiplier based on selected profile
        val accelerationFactor = when (settings.accelerationProfile) {
            AccelerationProfile.LINEAR -> {
                // 1:1 Direct constant response
                1.0f
            }
            AccelerationProfile.DYNAMIC -> {
                // Smooth ergonomic velocity curve
                val normalizedSpeed = speedPxPerSec / 350f
                if (normalizedSpeed <= 1.0f) {
                    // Slight micro-damping at low speeds for control
                    0.90f + 0.10f * normalizedSpeed
                } else {
                    // Smooth progressive curve for quick hand flicks
                    1.0f + (normalizedSpeed - 1.0f) * 0.40f * settings.accelerationMultiplier
                }.coerceIn(0.70f, 4.0f)
            }
            AccelerationProfile.PRECISION -> {
                // Precision Pro: Heavy micro-damping below 250px/s, snappy high-speed ramp
                if (speedPxPerSec < 250f) {
                    val ratio = (speedPxPerSec / 250f).coerceIn(0.35f, 1.0f)
                    0.50f + 0.50f * ratio
                } else {
                    val excess = (speedPxPerSec - 250f) / 400f
                    1.0f + excess.pow(1.35f) * 0.60f * settings.accelerationMultiplier
                }.coerceIn(0.40f, 5.0f)
            }
        }

        val effectiveMultiplier = sensitivity * accelerationFactor

        // Calculate total desired displacement including sub-pixel remainder from previous frame
        val targetDx = rawDx * effectiveMultiplier + subPixelRemainderX
        val targetDy = rawDy * effectiveMultiplier + subPixelRemainderY

        val sendDx = targetDx.toInt()
        val sendDy = targetDy.toInt()

        subPixelRemainderX = targetDx - sendDx
        subPixelRemainderY = targetDy - sendDy

        if (sendDx != 0 || sendDy != 0) {
            val buttons = if (isDragging) (currentButtonsMask.toInt() or 1).toByte() else currentButtonsMask
            onSendMouse(buttons, sendDx, sendDy, 0)
        }
    }

    /**
     * Processes two-finger scrolling with sensitivity and natural scroll orientation.
     */
    fun processScroll(rawScrollY: Float, rawScrollX: Float) {
        val directionMultiplier = if (settings.naturalScroll) -1.0f else 1.0f
        val totalDelta = rawScrollY * settings.scrollSensitivity * directionMultiplier + subPixelRemainderWheel
        val wheelStep = (totalDelta / 12f).toInt()

        if (wheelStep != 0) {
            subPixelRemainderWheel = totalDelta - (wheelStep * 12f)
            onSendMouse(currentButtonsMask, 0, 0, wheelStep)
        }
    }

    /**
     * Handles a single-finger tap gesture, invoking configured [GestureType.ONE_FINGER_TAP] action.
     */
    fun handleOneFingerTap() {
        if (!settings.tapToClick) return
        val action = settings.gestures[GestureType.ONE_FINGER_TAP] ?: GestureAction.LEFT_CLICK
        triggerGestureAction(action)
    }

    /**
     * Handles a two-finger tap gesture, invoking configured [GestureType.TWO_FINGER_TAP] action (secondary right-click).
     */
    fun handleTwoFingerTap() {
        if (!settings.twoFingerRightClick) return
        val action = settings.gestures[GestureType.TWO_FINGER_TAP] ?: GestureAction.RIGHT_CLICK
        triggerGestureAction(action)
    }

    /**
     * Handles a three-finger tap gesture, invoking configured [GestureType.THREE_FINGER_TAP] action.
     */
    fun handleThreeFingerTap() {
        val action = settings.gestures[GestureType.THREE_FINGER_TAP] ?: GestureAction.MIDDLE_CLICK
        triggerGestureAction(action)
    }

    /**
     * Handles a four-finger tap gesture, invoking configured [GestureType.FOUR_FINGER_TAP] action.
     */
    fun handleFourFingerTap() {
        val action = settings.gestures[GestureType.FOUR_FINGER_TAP] ?: GestureAction.ACTION_CENTER
        triggerGestureAction(action)
    }

    /**
     * Handles a multi-finger directional swipe gesture.
     *
     * @param fingerCount Number of concurrent contact touch points (3 or 4).
     * @param deltaX Total horizontal travel distance in pixels.
     * @param deltaY Total vertical travel distance in pixels.
     */
    fun handleSwipe(fingerCount: Int, deltaX: Float, deltaY: Float) {
        val isHorizontal = abs(deltaX) > abs(deltaY)
        val gestureType = when (fingerCount) {
            3 -> {
                if (isHorizontal) {
                    if (deltaX > 0) GestureType.THREE_FINGER_SWIPE_RIGHT else GestureType.THREE_FINGER_SWIPE_LEFT
                } else {
                    if (deltaY > 0) GestureType.THREE_FINGER_SWIPE_DOWN else GestureType.THREE_FINGER_SWIPE_UP
                }
            }
            4 -> {
                if (isHorizontal) {
                    if (deltaX > 0) GestureType.FOUR_FINGER_SWIPE_RIGHT else GestureType.FOUR_FINGER_SWIPE_LEFT
                } else {
                    return
                }
            }
            else -> return
        }

        val action = settings.gestures[gestureType] ?: GestureAction.NONE
        triggerGestureAction(action)
    }

    /**
     * Engages mouse left-click button drag state for double-tap drag gestures.
     */
    fun startDrag() {
        isDragging = true
        currentButtonsMask = (currentButtonsMask.toInt() or 1).toByte()
        onSendMouse(currentButtonsMask, 0, 0, 0)
    }

    /**
     * Releases active mouse drag state.
     */
    fun stopDrag() {
        if (isDragging) {
            isDragging = false
            currentButtonsMask = (currentButtonsMask.toInt() and 1.inv()).toByte()
            onSendMouse(currentButtonsMask, 0, 0, 0)
        }
    }

    /**
     * Updates persistent state of physical hardware buttons (e.g. left/right mouse button strip).
     *
     * @param buttonIndex 0 for left, 1 for right, 2 for middle.
     * @param isPressed True if button is currently depressed.
     */
    fun setPhysicalButtonState(buttonIndex: Int, isPressed: Boolean) {
        val bit = 1 shl buttonIndex
        currentButtonsMask = if (isPressed) {
            (currentButtonsMask.toInt() or bit).toByte()
        } else {
            (currentButtonsMask.toInt() and bit.inv()).toByte()
        }
        onSendMouse(currentButtonsMask, 0, 0, 0)
    }

    private fun triggerGestureAction(action: GestureAction) {
        if (action == GestureAction.NONE) return
        when (action) {
            GestureAction.LEFT_CLICK -> {
                onSendMouse((currentButtonsMask.toInt() or 1).toByte(), 0, 0, 0)
                onSendMouse(currentButtonsMask, 0, 0, 0)
            }
            GestureAction.RIGHT_CLICK -> {
                onSendMouse((currentButtonsMask.toInt() or 2).toByte(), 0, 0, 0)
                onSendMouse(currentButtonsMask, 0, 0, 0)
            }
            GestureAction.MIDDLE_CLICK -> {
                onSendMouse((currentButtonsMask.toInt() or 4).toByte(), 0, 0, 0)
                onSendMouse(currentButtonsMask, 0, 0, 0)
            }
            GestureAction.DOUBLE_CLICK -> {
                onSendMouse((currentButtonsMask.toInt() or 1).toByte(), 0, 0, 0)
                onSendMouse(currentButtonsMask, 0, 0, 0)
                onSendMouse((currentButtonsMask.toInt() or 1).toByte(), 0, 0, 0)
                onSendMouse(currentButtonsMask, 0, 0, 0)
            }
            else -> {
                onTriggerAction(action)
            }
        }
    }
}
