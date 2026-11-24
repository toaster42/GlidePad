package com.example.layout

import com.example.ui.layout.FloatModeController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FloatModeController] automatic transitions and manual override lifecycle.
 */
class FloatModeControllerTest {

    @Test
    fun initialState_isFalse() {
        val controller = FloatModeController()
        assertFalse(controller.isFloat.value)
    }

    @Test
    fun onWindowSize_tracksAutoFloatWhenNoOverrideActive() {
        val controller = FloatModeController()

        controller.onWindowSize(1280f, 800f)
        assertFalse(controller.isFloat.value)

        controller.onWindowSize(400f, 300f)
        assertTrue(controller.isFloat.value)

        controller.onWindowSize(600f, 480f)
        assertFalse(controller.isFloat.value)
    }

    @Test
    fun toggle_flipsStateAndPreservesOverrideWhileAutoUnchanged() {
        val controller = FloatModeController()
        controller.onWindowSize(1280f, 800f) // auto = false
        assertFalse(controller.isFloat.value)

        controller.toggle() // manual override = true
        assertTrue(controller.isFloat.value)

        // Resize within non-float domain keeps auto=false, so override stays true
        controller.onWindowSize(411f, 891f)
        assertTrue(controller.isFloat.value)

        // Toggle back off while still in auto=false domain
        controller.toggle()
        assertFalse(controller.isFloat.value)
    }

    @Test
    fun exitFloat_forcesFalseAndPersistsWhileAutoRemainsTrue() {
        val controller = FloatModeController()
        controller.onWindowSize(400f, 300f) // auto = true
        assertTrue(controller.isFloat.value)

        controller.exitFloat() // manual override = false
        assertFalse(controller.isFloat.value)

        // Resize within auto=true domain keeps auto=true, so manual false override stays active
        controller.onWindowSize(300f, 700f)
        assertFalse(controller.isFloat.value)

        // Calling exitFloat again stays false
        controller.exitFloat()
        assertFalse(controller.isFloat.value)
    }

    @Test
    fun onWindowSize_clearsManualOverrideWhenAutoFlips() {
        val controller = FloatModeController()

        // Start in large window (auto=false), manually enter Float
        controller.onWindowSize(1000f, 700f)
        controller.toggle()
        assertTrue(controller.isFloat.value)

        // Resize to small window: auto flips false -> true, clearing override (result = true)
        controller.onWindowSize(360f, 300f)
        assertTrue(controller.isFloat.value)

        // Resize back to large window: since override was cleared, auto=false takes effect
        controller.onWindowSize(1000f, 700f)
        assertFalse(controller.isFloat.value)

        // Now start in small window (auto=true), manually exit Float
        controller.onWindowSize(360f, 300f)
        assertTrue(controller.isFloat.value)
        controller.exitFloat()
        assertFalse(controller.isFloat.value)

        // Resize to large window (auto flips true -> false, clearing override -> false)
        controller.onWindowSize(800f, 600f)
        assertFalse(controller.isFloat.value)

        // Resize back to small window: since override was cleared, auto=true takes effect
        controller.onWindowSize(360f, 300f)
        assertTrue(controller.isFloat.value)
    }
}
