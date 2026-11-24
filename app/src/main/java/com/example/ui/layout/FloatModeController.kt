package com.example.ui.layout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure Kotlin state controller coordinating automatic size-driven Float mode with manual overrides.
 *
 * Initial automatic Float state is `false` until [onWindowSize] receives the first window metrics.
 * A manual [toggle] or [exitFloat] override persists across window-size updates as long as the
 * automatic [WindowLayoutPolicy.resolve] decision remains unchanged, and clears automatically as
 * soon as a resize flips the automatic Float decision.
 */
class FloatModeController {
    private val _isFloat = MutableStateFlow(false)

    /** Observable Float-mode state consumed by the root UI shell. */
    val isFloat: StateFlow<Boolean> = _isFloat.asStateFlow()

    private var autoFloat: Boolean = false
    private var manualOverride: Boolean? = null

    /**
     * Updates the automatic Float decision from the current window dimensions in dp.
     *
     * Clears any active manual override when the automatic decision flips; preserves the override
     * otherwise.
     */
    @Synchronized
    fun onWindowSize(widthDp: Float, heightDp: Float) {
        val newAuto = WindowLayoutPolicy.resolve(widthDp, heightDp) == LayoutMode.FLOAT
        if (newAuto != autoFloat) {
            autoFloat = newAuto
            manualOverride = null
        }
        _isFloat.value = manualOverride ?: autoFloat
    }

    /**
     * Toggles Float mode manually and records an explicit override until the automatic decision flips.
     */
    @Synchronized
    fun toggle() {
        val next = !_isFloat.value
        manualOverride = next
        _isFloat.value = next
    }

    /**
     * Forces Float mode off and records a manual `false` override until the automatic decision flips.
     */
    @Synchronized
    fun exitFloat() {
        manualOverride = false
        _isFloat.value = false
    }
}
