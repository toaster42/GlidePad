package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.model.HapticLevel
import com.example.model.TouchpadSettings
import com.example.ui.screens.TouchpadScreenContent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchpadScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testSettings(
        hapticLevel: HapticLevel = HapticLevel.MEDIUM,
        accelerationProfile: AccelerationProfile = AccelerationProfile.LINEAR,
        pointerSensitivity: Float = 1.0f
    ): TouchpadSettings = TouchpadSettings(
        pointerSensitivity = pointerSensitivity,
        accelerationProfile = accelerationProfile,
        tapToClick = true,
        showPhysicalButtons = true,
        hapticLevel = hapticLevel
    )

    @Test
    @Config(qualifiers = "w600dp-h800dp")
    fun `DEF-003 drag lock chip stays synced with TouchpadEngine isDragging across disposal`() {
        val sentButtons = mutableListOf<Byte>()
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { btn, _, _, _ -> sentButtons.add(btn) },
            onTriggerAction = {}
        )
        var showTouchpad by mutableStateOf(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(480.dp, 640.dp)) {
                    if (showTouchpad) {
                        TouchpadScreenContent(
                            engine = engine,
                            settings = testSettings(),
                            onSensitivityChange = {},
                            onButtonHaptic = {}
                        )
                    }
                }
            }
        }

        // Initially unlocked
        composeTestRule.onNodeWithText("Drag Lock").assertIsDisplayed()
        assertFalse(engine.isDragging.value)

        // Engage Drag Lock
        composeTestRule.onNodeWithTag("drag_lock_quick_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(engine.isDragging.value)
        composeTestRule.onNodeWithText("Drag Locked").assertIsDisplayed()
        assertEquals(1.toByte(), sentButtons.last())

        // Simulate entering Float mode or switching tabs (disposes TouchpadScreenContent)
        showTouchpad = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drag_lock_quick_button").assertDoesNotExist()
        assertTrue(engine.isDragging.value)

        // Return to TouchpadScreenContent and verify Drag Locked state is preserved
        showTouchpad = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Drag Locked").assertIsDisplayed()

        // Tap again to disengage
        composeTestRule.onNodeWithTag("drag_lock_quick_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(engine.isDragging.value)
        composeTestRule.onNodeWithText("Drag Lock").assertIsDisplayed()
        assertEquals(0.toByte(), sentButtons.last())
    }

    @Test
    @Config(qualifiers = "w240dp-h480dp")
    fun `DEF-004 quick controls remain legible and clickable at 240dp window width`() {
        var floatToggled = 0
        val engine = TouchpadEngine(
            settings = testSettings(
                accelerationProfile = AccelerationProfile.PRECISION,
                pointerSensitivity = 1.25f
            ),
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = {}
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(240.dp, 480.dp)) {
                    TouchpadScreenContent(
                        engine = engine,
                        settings = testSettings(
                            accelerationProfile = AccelerationProfile.PRECISION,
                            pointerSensitivity = 1.25f
                        ),
                        onSensitivityChange = {},
                        onButtonHaptic = {},
                        onToggleFloat = { floatToggled++ }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val dragChip = composeTestRule.onNodeWithTag("drag_lock_quick_button")
        val sensChip = composeTestRule.onNodeWithTag("quick_sensitivity_badge")
        val floatChip = composeTestRule.onNodeWithTag("float_toggle_button")

        dragChip.assertIsDisplayed()
        sensChip.assertIsDisplayed()
        floatChip.assertIsDisplayed()
        composeTestRule.onNodeWithText("Drag Lock").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.25x Precision").assertIsDisplayed()
        composeTestRule.onNodeWithText("Float").assertIsDisplayed()

        val dragBounds = dragChip.getUnclippedBoundsInRoot()
        val sensBounds = sensChip.getUnclippedBoundsInRoot()
        val floatBounds = floatChip.getUnclippedBoundsInRoot()

        assertTrue("drag chip right <= 240.dp, was ${dragBounds.right}", dragBounds.right <= 240.5.dp)
        assertTrue("sens chip right <= 240.dp, was ${sensBounds.right}", sensBounds.right <= 240.5.dp)
        assertTrue("float chip right <= 240.dp, was ${floatBounds.right}", floatBounds.right <= 240.5.dp)
        assertTrue("float chip width >= 48.dp, was ${floatBounds.width}", floatBounds.width >= 48.dp)
        assertTrue("float chip height >= 48.dp, was ${floatBounds.height}", floatBounds.height >= 48.dp)

        floatChip.performClick()
        assertEquals(1, floatToggled)

        dragChip.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Drag Locked").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h800dp")
    fun `DEF-005 MouseButtonCard invokes updated haptic callback after settings change`() {
        val engine = TouchpadEngine(
            settings = testSettings(),
            onSendMouse = { _, _, _, _ -> },
            onTriggerAction = {}
        )
        var activeHapticLevel by mutableStateOf(HapticLevel.LIGHT)
        val invokedLevels = mutableListOf<HapticLevel>()

        composeTestRule.setContent {
            MyApplicationTheme {
                val levelSnapshot = activeHapticLevel
                Box(modifier = Modifier.size(480.dp, 640.dp)) {
                    TouchpadScreenContent(
                        engine = engine,
                        settings = testSettings(hapticLevel = levelSnapshot),
                        onSensitivityChange = {},
                        onButtonHaptic = { invokedLevels.add(levelSnapshot) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("mouse_left_button").performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertEquals(listOf(HapticLevel.LIGHT), invokedLevels)

        // Update haptic level while TouchpadScreenContent remains composed
        activeHapticLevel = HapticLevel.STRONG
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mouse_left_button").performTouchInput { click() }
        composeTestRule.onNodeWithTag("mouse_right_button").performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(HapticLevel.LIGHT, HapticLevel.STRONG, HapticLevel.STRONG),
            invokedLevels
        )
    }
}
