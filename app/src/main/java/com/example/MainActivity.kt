package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.components.ConnectionHeader
import com.example.ui.components.FloatModeOverlay
import com.example.ui.components.TouchpadSurface
import com.example.ui.dialogs.ConnectionDialog
import com.example.ui.layout.LayoutMode
import com.example.ui.layout.MainLayoutFrame
import com.example.ui.layout.ShortcutAction
import com.example.ui.layout.WindowLayoutPolicy
import com.example.ui.layout.resolveShortcut
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MacrosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TouchpadScreen
import com.example.ui.screens.VirtualPreviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark900

/**
 * Primary Android Activity hosting the GlidePad user interface and lifecycle bindings.
 *
 * Configures edge-to-edge drawing, sets up Compose theming, and hosts [MainAppContent].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

/**
 * Root Composable orchestrating the adaptive layout across form factors.
 *
 * Implements canonical responsive layout switching via [MainLayoutFrame] and [WindowLayoutPolicy]:
 * - On compact handheld screens (< 600.dp): uses a bottom navigation bar and scaffold.
 * - On expanded screens (>= 600.dp, e.g. Chromebooks, landscape tablets, ChromeOS ARCVM):
 *   uses a left-aligned navigation rail with keyboard shortcut navigation (1-5, F, Esc).
 * - In Float mode (automatic for small windows or manual toggle): renders a full-window touchpad
 *   surface with minimal status/exit chrome.
 *
 * @param viewModel The shared [MainViewModel] supplying state and event dispatching.
 */
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isFloat by viewModel.isFloatMode.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }

    // Request Bluetooth permissions gracefully on Android 12+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }

    // Predictive back handler: Exit Float first, otherwise navigate back to Touchpad
    if (isFloat) {
        BackHandler {
            viewModel.exitFloatMode()
        }
    } else if (currentScreen != AppScreen.TOUCHPAD) {
        BackHandler {
            viewModel.selectScreen(AppScreen.TOUCHPAD)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .onKeyEvent { event ->
                // Support ChromeOS physical keyboard shortcuts in bubbling phase so text fields keep priority,
                // while ignoring modified chords (Ctrl/Alt/Meta) that text fields leave unconsumed.
                if (
                    event.type == KeyEventType.KeyDown &&
                    !event.isCtrlPressed &&
                    !event.isAltPressed &&
                    !event.isMetaPressed
                ) {
                    when (
                        val action = resolveShortcut(
                            key = event.key,
                            isFloat = isFloat,
                            isDialogOpen = showConnectionDialog,
                            currentScreen = currentScreen
                        )
                    ) {
                        is ShortcutAction.Select -> {
                            viewModel.selectScreen(action.screen)
                            true
                        }
                        ShortcutAction.ToggleFloat -> {
                            viewModel.toggleFloatMode()
                            true
                        }
                        ShortcutAction.ExitFloat -> {
                            viewModel.exitFloatMode()
                            true
                        }
                        ShortcutAction.CloseDialog -> {
                            showConnectionDialog = false
                            true
                        }
                        ShortcutAction.GoToTouchpad -> {
                            viewModel.selectScreen(AppScreen.TOUCHPAD)
                            true
                        }
                        ShortcutAction.None -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
    ) {
        val density = LocalDensity.current
        val isImeVisible = WindowInsets.ime.getBottom(density) > 0

        LaunchedEffect(maxWidth, maxHeight, isImeVisible) {
            if (!isImeVisible) {
                viewModel.onWindowSize(maxWidth.value, maxHeight.value)
            }
        }

        val layoutMode = if (isFloat) {
            LayoutMode.FLOAT
        } else {
            WindowLayoutPolicy.resolveNonFloat(maxWidth.value)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            MainLayoutFrame(
                layoutMode = layoutMode,
                currentScreen = currentScreen,
                onSelectScreen = viewModel::selectScreen,
                header = {
                    ConnectionHeader(
                        stats = stats,
                        onOpenConnectionDialog = { showConnectionDialog = true },
                        onOpenSettings = { viewModel.selectScreen(AppScreen.SETTINGS) }
                    )
                },
                floatContent = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TouchpadSurface(
                            engine = viewModel.touchpadEngine,
                            modifier = Modifier.fillMaxSize(),
                            showGestureHints = false,
                            surfaceTestTag = "float_touchpad_surface"
                        )
                        FloatModeOverlay(
                            status = stats.connectionStatus,
                            onExitFloat = viewModel::exitFloatMode
                        )
                    }
                },
                screenContent = { screen ->
                    when (screen) {
                        AppScreen.TOUCHPAD -> TouchpadScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onToggleFloat = viewModel::toggleFloatMode
                        )
                        AppScreen.KEYBOARD -> KeyboardScreen(viewModel = viewModel)
                        AppScreen.MACROS -> MacrosScreen(viewModel = viewModel)
                        AppScreen.VIRTUAL_SCREEN -> VirtualPreviewScreen(viewModel = viewModel)
                        AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, settings = settings)
                    }
                }
            )
        }
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            viewModel = viewModel,
            onDismiss = { showConnectionDialog = false }
        )
    }
}
