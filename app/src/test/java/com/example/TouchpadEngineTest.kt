package com.example

import com.example.engine.TouchpadEngine
import com.example.model.AccelerationProfile
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.TouchpadSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchpadEngineTest {

    @Test
    fun testLinearAccelerationDirectOutput() {
        var sentDx = 0
        var sentDy = 0

        val settings = TouchpadSettings(
            pointerSensitivity = 1.0f,
            accelerationProfile = AccelerationProfile.LINEAR
        )

        val engine = TouchpadEngine(
            settings = settings,
            onSendMouse = { _, dx, dy, _ ->
                sentDx = dx
                sentDy = dy
            },
            onTriggerAction = {}
        )

        engine.processPointerMove(10f, 20f, 16L)
        assertEquals(10, sentDx)
        assertEquals(20, sentDy)
    }

    @Test
    fun testSensitivityMultiplierScaling() {
        var sentDx = 0
        var sentDy = 0

        val settings = TouchpadSettings(
            pointerSensitivity = 2.0f,
            accelerationProfile = AccelerationProfile.LINEAR
        )

        val engine = TouchpadEngine(
            settings = settings,
            onSendMouse = { _, dx, dy, _ ->
                sentDx = dx
                sentDy = dy
            },
            onTriggerAction = {}
        )

        engine.processPointerMove(15f, 10f, 16L)
        assertEquals(30, sentDx)
        assertEquals(20, sentDy)
    }

    @Test
    fun testNaturalScrollDirection() {
        var sentWheel = 0

        val naturalSettings = TouchpadSettings(
            naturalScroll = true,
            scrollSensitivity = 1.0f
        )

        val engine = TouchpadEngine(
            settings = naturalSettings,
            onSendMouse = { _, _, _, wheel ->
                sentWheel = wheel
            },
            onTriggerAction = {}
        )

        engine.processScroll(48f, 0f)
        assertTrue(sentWheel < 0)
    }

    @Test
    fun testOneFingerTapLeftClick() {
        var clickCount = 0
        val settings = TouchpadSettings(
            tapToClick = true,
            gestures = mapOf(GestureType.ONE_FINGER_TAP to GestureAction.LEFT_CLICK)
        )
        val engine = TouchpadEngine(
            settings = settings,
            onSendMouse = { btn, _, _, _ ->
                if ((btn.toInt() and 1) != 0) clickCount++
            },
            onTriggerAction = {}
        )
        engine.handleOneFingerTap()
        assertTrue(clickCount > 0)
    }

    @Test
    fun testGestureTapActionTriggered() {
        var triggeredAction: GestureAction? = null

        val settings = TouchpadSettings(
            gestures = mapOf(GestureType.THREE_FINGER_TAP to GestureAction.TASK_VIEW)
        )

        val engine = TouchpadEngine(
            settings = settings,
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = { action ->
                triggeredAction = action
            }
        )

        engine.handleThreeFingerTap()
        assertEquals(GestureAction.TASK_VIEW, triggeredAction)
    }
}
