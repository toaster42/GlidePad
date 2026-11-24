package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.example.engine.TouchpadEngine
import com.example.model.AccelerationProfile
import com.example.model.ConnectionStatus
import com.example.model.TouchpadSettings
import com.example.ui.components.FloatModeOverlay
import com.example.ui.components.TouchpadSurface
import com.example.ui.screens.TouchpadScreenContent
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchpadSurfaceUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testSettings(): TouchpadSettings = TouchpadSettings(
        pointerSensitivity = 1.0f,
        accelerationProfile = AccelerationProfile.LINEAR,
        tapToClick = true,
        showPhysicalButtons = true
    )

    @Test
    @Config(qualifiers = "w600dp-h600dp")
    fun `AC-10 float touchpad surface drag emits nonzero dx dy and tap triggers left click`() {
        val sentEvents = mutableListOf<Triple<Byte, Int, Int>>()
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { btn, dx, dy, _ ->
                sentEvents.add(Triple(btn, dx, dy))
            },
            onTriggerAction = {}
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(400.dp, 300.dp)) {
                    TouchpadSurface(
                        engine = engine,
                        modifier = Modifier.fillMaxSize(),
                        showGestureHints = false,
                        surfaceTestTag = "float_touchpad_surface"
                    )
                    FloatModeOverlay(
                        status = ConnectionStatus.CONNECTED,
                        onExitFloat = {}
                    )
                }
            }
        }

        // Gesture hints must be hidden when showGestureHints = false
        composeTestRule.onNodeWithText("1-Finger: Cursor").assertDoesNotExist()

        // 1-finger drag must pass through FloatModeOverlay and emit nonzero dx/dy
        composeTestRule.onNodeWithTag("float_touchpad_surface").performTouchInput {
            down(center)
            moveBy(Offset(100f, 50f))
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Expected nonzero dx or dy from drag on float_touchpad_surface, got: $sentEvents",
            sentEvents.any { (_, dx, dy) -> dx != 0 || dy != 0 }
        )

        // 1-finger tap must trigger left click (btn bit 1 set)
        sentEvents.clear()
        composeTestRule.onNodeWithTag("float_touchpad_surface").performTouchInput {
            click()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Expected left-click mouse report (bit 1 set), got: $sentEvents",
            sentEvents.any { (btn, _, _) -> (btn.toInt() and 1) != 0 }
        )
    }

    @Test
    @Config(qualifiers = "w1600dp-h900dp")
    fun `AC-5 main touchpad surface expands beyond 1000dp inside 1600x900dp host`() {
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = {}
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(1600.dp, 900.dp)) {
                    TouchpadScreenContent(
                        engine = engine,
                        settings = testSettings(),
                        onSensitivityChange = {},
                        onButtonHaptic = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("touchpad_quick_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("physical_buttons_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("1-Finger: Cursor").assertIsDisplayed()

        val bounds = composeTestRule.onNodeWithTag("main_touchpad_surface").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected main_touchpad_surface width > 1000.dp, was ${bounds.width}",
            bounds.width > 1000.dp
        )
    }

    @Test
    @Config(qualifiers = "w600dp-h800dp")
    fun `AC-6 float toggle button invokes onToggleFloat and meets 48dp touch target and a11y`() {
        var toggleCount = 0
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = {}
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(480.dp, 640.dp)) {
                    TouchpadScreenContent(
                        engine = engine,
                        settings = testSettings(),
                        onSensitivityChange = {},
                        onButtonHaptic = {},
                        onToggleFloat = { toggleCount++ }
                    )
                }
            }
        }

        val toggleNode = composeTestRule.onNodeWithTag("float_toggle_button")
        toggleNode.assertIsDisplayed()
        toggleNode.assertContentDescriptionEquals("Enter float mode")
        val toggleBounds = toggleNode.getUnclippedBoundsInRoot()
        assertTrue("float_toggle_button width >= 48.dp, was ${toggleBounds.width}", toggleBounds.width >= 48.dp)
        assertTrue("float_toggle_button height >= 48.dp, was ${toggleBounds.height}", toggleBounds.height >= 48.dp)
        toggleNode.performClick()
        assertEquals(1, toggleCount)
    }

    @Test
    fun `AC-6 float exit button invokes onExitFloat and meets 48dp touch target and a11y`() {
        var exitCount = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(300.dp, 220.dp)) {
                    FloatModeOverlay(
                        status = ConnectionStatus.CONNECTED,
                        onExitFloat = { exitCount++ }
                    )
                }
            }
        }

        val exitNode = composeTestRule.onNodeWithTag("float_exit_button")
        exitNode.assertIsDisplayed()
        exitNode.assertContentDescriptionEquals("Exit float mode")
        val exitBounds = exitNode.getUnclippedBoundsInRoot()
        assertTrue("float_exit_button width >= 48.dp, was ${exitBounds.width}", exitBounds.width >= 48.dp)
        assertTrue("float_exit_button height >= 48.dp, was ${exitBounds.height}", exitBounds.height >= 48.dp)
        exitNode.performClick()
        assertEquals(1, exitCount)
    }

    @Test
    fun `AC-6 float status dot exposes ConnectionStatus label as contentDescription`() {
        var status by mutableStateOf(ConnectionStatus.DISCONNECTED)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(300.dp, 220.dp)) {
                    FloatModeOverlay(
                        status = status,
                        onExitFloat = {}
                    )
                }
            }
        }

        for (nextStatus in ConnectionStatus.entries) {
            status = nextStatus
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("float_status_dot")
                .assertContentDescriptionEquals(nextStatus.label)
        }
    }

    @Test
    @Config(qualifiers = "w600dp-h600dp")
    fun `AC-4 touchpad surface dynamically fills host constraints from 400x300dp to 320x240dp`() {
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = {}
        )
        var hostWidth by mutableStateOf(400.dp)
        var hostHeight by mutableStateOf(300.dp)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(hostWidth, hostHeight)) {
                    TouchpadSurface(
                        engine = engine,
                        modifier = Modifier.fillMaxSize(),
                        surfaceTestTag = "main_touchpad_surface"
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialBounds = composeTestRule.onNodeWithTag("main_touchpad_surface").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected initial width ~400.dp (+/-1.dp), was ${initialBounds.width}",
            abs(initialBounds.width.value - 400f) <= 1f
        )
        assertTrue(
            "Expected initial height ~300.dp (+/-1.dp), was ${initialBounds.height}",
            abs(initialBounds.height.value - 300f) <= 1f
        )

        hostWidth = 320.dp
        hostHeight = 240.dp
        composeTestRule.waitForIdle()

        val resizedBounds = composeTestRule.onNodeWithTag("main_touchpad_surface").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected resized width ~320.dp (+/-1.dp), was ${resizedBounds.width}",
            abs(resizedBounds.width.value - 320f) <= 1f
        )
        assertTrue(
            "Expected resized height ~240.dp (+/-1.dp), was ${resizedBounds.height}",
            abs(resizedBounds.height.value - 240f) <= 1f
        )
    }
}
