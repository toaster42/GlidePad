package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AccelerationProfile
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.HapticLevel
import com.example.model.TouchpadSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("glidepad_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<TouchpadSettings> = _settings.asStateFlow()

    private fun loadSettings(): TouchpadSettings {
        val sensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.25f)
        val profileName = prefs.getString(KEY_ACCEL_PROFILE, AccelerationProfile.DYNAMIC.name)
        val profile = try {
            AccelerationProfile.valueOf(profileName ?: AccelerationProfile.DYNAMIC.name)
        } catch (e: Exception) {
            AccelerationProfile.DYNAMIC
        }
        val multiplier = prefs.getFloat(KEY_ACCEL_MULTIPLIER, 1.20f)
        val scrollSens = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f)
        val natural = prefs.getBoolean(KEY_NATURAL_SCROLL, true)
        val inertia = prefs.getBoolean(KEY_SMOOTH_INERTIA, true)
        val tapClick = prefs.getBoolean(KEY_TAP_TO_CLICK, true)
        val dragLock = prefs.getBoolean(KEY_DRAG_LOCK, false)
        val twoFingerRight = prefs.getBoolean(KEY_TWO_FINGER_RIGHT, true)
        val hapticName = prefs.getString(KEY_HAPTIC_LEVEL, HapticLevel.MEDIUM.name)
        val haptic = try {
            HapticLevel.valueOf(hapticName ?: HapticLevel.MEDIUM.name)
        } catch (e: Exception) {
            HapticLevel.MEDIUM
        }
        val showButtons = prefs.getBoolean(KEY_SHOW_BUTTONS, true)
        val showScroll = prefs.getBoolean(KEY_SHOW_SCROLL_STRIP, true)

        val defaultGestures = TouchpadSettings.defaultGestures().toMutableMap()
        for (gesture in GestureType.values()) {
            val savedActionName = prefs.getString("gesture_${gesture.name}", null)
            if (savedActionName != null) {
                try {
                    defaultGestures[gesture] = GestureAction.valueOf(savedActionName)
                } catch (e: Exception) {
                    // keep default
                }
            }
        }

        return TouchpadSettings(
            pointerSensitivity = sensitivity,
            accelerationProfile = profile,
            accelerationMultiplier = multiplier,
            scrollSensitivity = scrollSens,
            naturalScroll = natural,
            smoothInertia = inertia,
            tapToClick = tapClick,
            dragLock = dragLock,
            twoFingerRightClick = twoFingerRight,
            hapticLevel = haptic,
            showPhysicalButtons = showButtons,
            showScrollStrip = showScroll,
            gestures = defaultGestures
        )
    }

    fun updateSettings(newSettings: TouchpadSettings) {
        prefs.edit().apply {
            putFloat(KEY_SENSITIVITY, newSettings.pointerSensitivity)
            putString(KEY_ACCEL_PROFILE, newSettings.accelerationProfile.name)
            putFloat(KEY_ACCEL_MULTIPLIER, newSettings.accelerationMultiplier)
            putFloat(KEY_SCROLL_SENSITIVITY, newSettings.scrollSensitivity)
            putBoolean(KEY_NATURAL_SCROLL, newSettings.naturalScroll)
            putBoolean(KEY_SMOOTH_INERTIA, newSettings.smoothInertia)
            putBoolean(KEY_TAP_TO_CLICK, newSettings.tapToClick)
            putBoolean(KEY_DRAG_LOCK, newSettings.dragLock)
            putBoolean(KEY_TWO_FINGER_RIGHT, newSettings.twoFingerRightClick)
            putString(KEY_HAPTIC_LEVEL, newSettings.hapticLevel.name)
            putBoolean(KEY_SHOW_BUTTONS, newSettings.showPhysicalButtons)
            putBoolean(KEY_SHOW_SCROLL_STRIP, newSettings.showScrollStrip)

            for ((gesture, action) in newSettings.gestures) {
                putString("gesture_${gesture.name}", action.name)
            }
            apply()
        }
        _settings.value = newSettings
    }

    fun resetToDefaults() {
        val default = TouchpadSettings()
        updateSettings(default)
    }

    companion object {
        private const val KEY_SENSITIVITY = "key_pointer_sensitivity"
        private const val KEY_ACCEL_PROFILE = "key_accel_profile"
        private const val KEY_ACCEL_MULTIPLIER = "key_accel_multiplier"
        private const val KEY_SCROLL_SENSITIVITY = "key_scroll_sensitivity"
        private const val KEY_NATURAL_SCROLL = "key_natural_scroll"
        private const val KEY_SMOOTH_INERTIA = "key_smooth_inertia"
        private const val KEY_TAP_TO_CLICK = "key_tap_to_click"
        private const val KEY_DRAG_LOCK = "key_drag_lock"
        private const val KEY_TWO_FINGER_RIGHT = "key_two_finger_right"
        private const val KEY_HAPTIC_LEVEL = "key_haptic_level"
        private const val KEY_SHOW_BUTTONS = "key_show_buttons"
        private const val KEY_SHOW_SCROLL_STRIP = "key_show_scroll_strip"
    }
}
