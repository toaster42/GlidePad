package com.example.ui

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.example.AppScreen
import com.example.MainAppContent
import com.example.MainViewModel
import com.example.engine.TouchpadEngine
import com.example.model.AccelerationProfile
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.DiagnosticStats
import com.example.model.TouchpadSettings
import com.example.ui.components.ConnectionHeader
import com.example.ui.components.FloatModeOverlay
import com.example.ui.components.TouchpadSurface
import com.example.ui.layout.FloatModeController
import com.example.ui.layout.LayoutMode
import com.example.ui.layout.MainLayoutFrame
import com.example.ui.layout.WindowLayoutPolicy
import com.example.ui.screens.TouchpadScreenContent
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs
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
class MainLayoutFrameUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testSettings(): TouchpadSettings = TouchpadSettings(
        pointerSensitivity = 1.0f,
        accelerationProfile = AccelerationProfile.LINEAR,
        tapToClick = true,
        showPhysicalButtons = true
    )

    private fun testStats(): DiagnosticStats = DiagnosticStats(
        connectionMode = ConnectionMode.LOCAL_WIFI,
        connectionStatus = ConnectionStatus.CONNECTED,
        connectedDeviceName = "Test-PC"
    )

    private fun createEngine(): TouchpadEngine = TouchpadEngine(
        settings = testSettings(),
        onSendMouse = { _, _, _, _ -> },
        onTriggerAction = {}
    )

    @Test
    @Config(qualifiers = "w600dp-h600dp")
    fun `AC-3 float mode displays only float touchpad overlay and hides shell chrome`() {
        val engine = createEngine()
        val settings = testSettings()
        val stats = testStats()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(400.dp, 300.dp)) {
                    MainLayoutFrame(
                        layoutMode = LayoutMode.FLOAT,
                        currentScreen = AppScreen.TOUCHPAD,
                        onSelectScreen = {},
                        header = {
                            ConnectionHeader(
                                stats = stats,
                                onOpenConnectionDialog = {},
                                onOpenSettings = {}
                            )
                        },
                        floatContent = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TouchpadSurface(
                                    engine = engine,
                                    modifier = Modifier.fillMaxSize(),
                                    showGestureHints = false,
                                    surfaceTestTag = "float_touchpad_surface"
                                )
                                FloatModeOverlay(
                                    status = stats.connectionStatus,
                                    onExitFloat = {}
                                )
                            }
                        },
                        screenContent = {
                            TouchpadScreenContent(
                                engine = engine,
                                settings = settings,
                                onSensitivityChange = {},
                                onButtonHaptic = {}
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("float_touchpad_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_status_dot").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_exit_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("connection_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("touchpad_quick_controls").assertDoesNotExist()
        composeTestRule.onNodeWithTag("physical_buttons_bar").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `AC-3 expanded mode shows desktop rail with 5 tabs and hides receiver tab`() {
        val engine = createEngine()
        val settings = testSettings()
        val stats = testStats()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(1280.dp, 800.dp)) {
                    MainLayoutFrame(
                        layoutMode = LayoutMode.EXPANDED,
                        currentScreen = AppScreen.TOUCHPAD,
                        onSelectScreen = {},
                        header = {
                            ConnectionHeader(
                                stats = stats,
                                onOpenConnectionDialog = {},
                                onOpenSettings = {}
                            )
                        },
                        floatContent = {},
                        screenContent = {
                            TouchpadScreenContent(
                                engine = engine,
                                settings = settings,
                                onSensitivityChange = {},
                                onButtonHaptic = {}
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("connection_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("touchpad_quick_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("physical_buttons_bar").assertIsDisplayed()

        for (screen in AppScreen.entries) {
            composeTestRule.onNodeWithTag("rail_tab_${screen.name.lowercase()}").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("rail_tab_receiver").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `AC-3 compact mode shows bottom nav bar with 5 tabs and hides receiver tab`() {
        val engine = createEngine()
        val settings = testSettings()
        val stats = testStats()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(412.dp, 915.dp)) {
                    MainLayoutFrame(
                        layoutMode = LayoutMode.COMPACT,
                        currentScreen = AppScreen.TOUCHPAD,
                        onSelectScreen = {},
                        header = {
                            ConnectionHeader(
                                stats = stats,
                                onOpenConnectionDialog = {},
                                onOpenSettings = {}
                            )
                        },
                        floatContent = {},
                        screenContent = {
                            TouchpadScreenContent(
                                engine = engine,
                                settings = settings,
                                onSensitivityChange = {},
                                onButtonHaptic = {}
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("connection_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("touchpad_quick_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("physical_buttons_bar").assertIsDisplayed()

        for (screen in AppScreen.entries) {
            composeTestRule.onNodeWithTag("nav_tab_${screen.name.lowercase()}").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("nav_tab_receiver").assertDoesNotExist()
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w600dp-h600dp")
    fun `AC-4 float touchpad surface tracks host resize from 400x300dp to 320x240dp`() {
        val engine = createEngine()
        val stats = testStats()
        var hostSize by mutableStateOf(400.dp to 300.dp)

        composeTestRule.setContent {
            MyApplicationTheme {
                val (hostW, hostH) = hostSize
                Box(modifier = Modifier.size(hostW, hostH)) {
                    MainLayoutFrame(
                        layoutMode = LayoutMode.FLOAT,
                        currentScreen = AppScreen.TOUCHPAD,
                        onSelectScreen = {},
                        header = {},
                        floatContent = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TouchpadSurface(
                                    engine = engine,
                                    modifier = Modifier.fillMaxSize(),
                                    showGestureHints = false,
                                    surfaceTestTag = "float_touchpad_surface"
                                )
                                FloatModeOverlay(
                                    status = stats.connectionStatus,
                                    onExitFloat = {}
                                )
                            }
                        },
                        screenContent = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialBounds = composeTestRule.onNodeWithTag("float_touchpad_surface").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected initial width ~400.dp (+/-1.dp), was ${initialBounds.width}",
            abs(initialBounds.width.value - 400f) <= 1f
        )
        assertTrue(
            "Expected initial height ~300.dp (+/-1.dp), was ${initialBounds.height}",
            abs(initialBounds.height.value - 300f) <= 1f
        )

        hostSize = 320.dp to 240.dp
        composeTestRule.waitForIdle()

        val resizedBounds = composeTestRule.onNodeWithTag("float_touchpad_surface").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected resized width ~320.dp (+/-1.dp), was ${resizedBounds.width}",
            abs(resizedBounds.width.value - 320f) <= 1f
        )
        assertTrue(
            "Expected resized height ~240.dp (+/-1.dp), was ${resizedBounds.height}",
            abs(resizedBounds.height.value - 240f) <= 1f
        )
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `AC-6 float toggle button enters float mode and float exit button exits via FloatModeController`() {
        val engine = createEngine()
        val settings = testSettings()
        val stats = testStats()
        val controller = FloatModeController().apply {
            onWindowSize(1280f, 800f)
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                val isFloat by controller.isFloat.collectAsState()
                val layoutMode = if (isFloat) LayoutMode.FLOAT else WindowLayoutPolicy.resolveNonFloat(1280f)
                var currentScreen by remember { mutableStateOf(AppScreen.TOUCHPAD) }

                Box(modifier = Modifier.size(1280.dp, 800.dp)) {
                    MainLayoutFrame(
                        layoutMode = layoutMode,
                        currentScreen = currentScreen,
                        onSelectScreen = { currentScreen = it },
                        header = {
                            ConnectionHeader(
                                stats = stats,
                                onOpenConnectionDialog = {},
                                onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                            )
                        },
                        floatContent = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TouchpadSurface(
                                    engine = engine,
                                    modifier = Modifier.fillMaxSize(),
                                    showGestureHints = false,
                                    surfaceTestTag = "float_touchpad_surface"
                                )
                                FloatModeOverlay(
                                    status = stats.connectionStatus,
                                    onExitFloat = controller::exitFloat
                                )
                            }
                        },
                        screenContent = {
                            TouchpadScreenContent(
                                engine = engine,
                                settings = settings,
                                onSensitivityChange = {},
                                onButtonHaptic = {},
                                onToggleFloat = controller::toggle
                            )
                        }
                    )
                }
            }
        }

        assertFalse(controller.isFloat.value)
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()

        composeTestRule.onNodeWithTag("float_toggle_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(controller.isFloat.value)
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_exit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertDoesNotExist()

        composeTestRule.onNodeWithTag("float_exit_button").performClick()
        composeTestRule.waitForIdle()

        assertFalse(controller.isFloat.value)
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertDoesNotExist()
    }

    @Test
    fun `AC-2 MainViewModel delegates window size toggleFloatMode and exitFloatMode to FloatModeController`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        assertFalse(viewModel.isFloatMode.value)

        viewModel.onWindowSize(480f, 400f)
        assertTrue(viewModel.isFloatMode.value)
        assertTrue(viewModel.floatModeController.isFloat.value)

        viewModel.exitFloatMode()
        assertFalse(viewModel.isFloatMode.value)
        assertFalse(viewModel.floatModeController.isFloat.value)

        viewModel.onWindowSize(1280f, 800f)
        assertFalse(viewModel.isFloatMode.value)

        viewModel.toggleFloatMode()
        assertTrue(viewModel.isFloatMode.value)

        viewModel.toggleFloatMode()
        assertFalse(viewModel.isFloatMode.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `AC-7 MainAppContent routes hardware keyboard shortcuts for screens float toggle and escape`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
        assertEquals(AppScreen.TOUCHPAD, viewModel.currentScreen.value)

        val focusTarget = composeTestRule.onAllNodes(isFocusable()).onFirst()
        focusTarget.requestFocus()
        composeTestRule.waitForIdle()

        // Select screen 2 (KEYBOARD) via bubbling key event
        focusTarget.performKeyInput {
            pressKey(Key.Two)
        }
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.KEYBOARD, viewModel.currentScreen.value)

        // Escape returns from non-Touchpad screen to TOUCHPAD
        focusTarget.performKeyInput {
            pressKey(Key.Escape)
        }
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.TOUCHPAD, viewModel.currentScreen.value)

        // F toggles into Float mode
        focusTarget.performKeyInput {
            pressKey(Key.F)
        }
        composeTestRule.waitForIdle()
        assertTrue(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertIsDisplayed()

        // Escape exits Float mode
        focusTarget.performKeyInput {
            pressKey(Key.Escape)
        }
        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `DEF-001 landscape phone with safeDrawing system bar insets stays EXPANDED and does not trigger FLOAT`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        var hostView: View? = null
        var topInsetPx = 0
        var bottomInsetPx = 0

        composeTestRule.setContent {
            hostView = LocalView.current
            with(LocalDensity.current) {
                topInsetPx = 48.dp.roundToPx()
                bottomInsetPx = 48.dp.roundToPx()
            }
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()

        // Dispatch 96dp vertical system bar insets (411 - 96 = 315dp < 320dp if subtracted before measurement)
        composeTestRule.runOnUiThread {
            val view = requireNotNull(hostView)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topInsetPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottomInsetPx))
                .build()
            ViewCompat.dispatchApplyWindowInsets(view.rootView, insets)
        }
        composeTestRule.waitForIdle()

        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h731dp")
    fun `DEF-001 portrait phone with IME insets and adjustResize does not flip KeyboardScreen into FLOAT`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.selectScreen(AppScreen.KEYBOARD)

        var hostView: View? = null
        var statusInsetPx = 0
        var navInsetPx = 0
        var imeInsetPx = 0
        var hostHeight by mutableStateOf(731.dp)

        composeTestRule.setContent {
            hostView = LocalView.current
            with(LocalDensity.current) {
                statusInsetPx = 32.dp.roundToPx()
                navInsetPx = 48.dp.roundToPx()
                imeInsetPx = 340.dp.roundToPx()
            }
            MyApplicationTheme {
                Box(modifier = Modifier.size(411.dp, hostHeight)) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }

        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_input_field").assertIsDisplayed()

        // Step 1: Edge-to-edge IME appears (731 - 340 = 391dp < 480dp if safeDrawing were on BoxWithConstraints)
        composeTestRule.runOnUiThread {
            val view = requireNotNull(hostView)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusInsetPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navInsetPx))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imeInsetPx))
                .build()
            ViewCompat.dispatchApplyWindowInsets(view.rootView, insets)
        }
        composeTestRule.waitForIdle()

        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertDoesNotExist()

        // Step 2: Pre-API-30 adjustResize shrinks window height to 391dp while IME is visible
        hostHeight = 391.dp
        composeTestRule.waitForIdle()

        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertExists()
        composeTestRule.onNodeWithTag("text_input_field").assertExists()
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertDoesNotExist()

        // Step 3: IME closes and window height restores to 731dp
        composeTestRule.runOnUiThread {
            val view = requireNotNull(hostView)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusInsetPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navInsetPx))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                .build()
            ViewCompat.dispatchApplyWindowInsets(view.rootView, insets)
        }
        hostHeight = 731.dp
        composeTestRule.waitForIdle()

        assertFalse(viewModel.isFloatMode.value)
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("float_touchpad_surface").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `DEF-002 modified chords Ctrl+F Alt+Two and Meta+F do not trigger shortcuts while Shift+F still toggles Float`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(AppScreen.TOUCHPAD, viewModel.currentScreen.value)
        assertFalse(viewModel.isFloatMode.value)

        val focusTarget = composeTestRule.onAllNodes(isFocusable()).onFirst()
        focusTarget.requestFocus()
        composeTestRule.waitForIdle()

        // Ctrl+F must not toggle Float
        focusTarget.performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.F)
            keyUp(Key.CtrlLeft)
        }
        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)

        // Alt+Two must not switch to KEYBOARD
        focusTarget.performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.Two)
            keyUp(Key.AltLeft)
        }
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.TOUCHPAD, viewModel.currentScreen.value)

        // Meta+F must not toggle Float
        focusTarget.performKeyInput {
            keyDown(Key.MetaLeft)
            pressKey(Key.F)
            keyUp(Key.MetaLeft)
        }
        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)

        // Also verify modified chords bubbling out of focused OutlinedTextField on KeyboardScreen
        viewModel.selectScreen(AppScreen.KEYBOARD)
        composeTestRule.waitForIdle()
        val textField = composeTestRule.onNodeWithTag("text_input_field")
        textField.requestFocus()
        composeTestRule.waitForIdle()

        textField.performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.F)
            keyUp(Key.CtrlLeft)
            keyDown(Key.AltLeft)
            pressKey(Key.One)
            keyUp(Key.AltLeft)
        }
        composeTestRule.waitForIdle()
        assertFalse(viewModel.isFloatMode.value)
        assertEquals(AppScreen.KEYBOARD, viewModel.currentScreen.value)

        // Shift+F (no Ctrl/Alt/Meta) on root focus target still toggles Float mode
        viewModel.selectScreen(AppScreen.TOUCHPAD)
        composeTestRule.waitForIdle()
        focusTarget.requestFocus()
        composeTestRule.waitForIdle()

        focusTarget.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F)
            keyUp(Key.ShiftLeft)
        }
        composeTestRule.waitForIdle()
        assertTrue(viewModel.isFloatMode.value)
    }
}
