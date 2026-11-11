package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.HapticLevel

/**
 * Haptic feedback manager providing tactile response for clicks, taps, and gesture recognition.
 *
 * Utilizes [VibratorManager] on Android 12+ (API 31+) and falls back to legacy [Vibrator] on older
 * versions, producing calibrated haptic waveforms according to the user's selected [HapticLevel].
 *
 * @param context Application context used to resolve the system vibrator service.
 */
class HapticFeedbackHelper(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Produces a discrete click sensation for mouse button presses and virtual keyboard strikes.
     *
     * @param level User-configured [HapticLevel] vibration intensity.
     */
    fun click(level: HapticLevel) {
        if (level == HapticLevel.OFF || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (level) {
                HapticLevel.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticLevel.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticLevel.STRONG -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticLevel.OFF -> return
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val duration = when (level) {
                HapticLevel.LIGHT -> 8L
                HapticLevel.MEDIUM -> 18L
                HapticLevel.STRONG -> 32L
                HapticLevel.OFF -> 0L
            }
            if (duration > 0) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    /**
     * Produces a double-tick confirmation waveform upon multi-finger gesture recognition.
     *
     * @param level User-configured [HapticLevel] vibration intensity.
     */
    fun gestureTriggered(level: HapticLevel) {
        if (level == HapticLevel.OFF || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 15, 30, 20), -1)
        }
    }
}
