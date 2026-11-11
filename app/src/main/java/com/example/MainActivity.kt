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
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConnectionHeader
import com.example.ui.dialogs.ConnectionDialog
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MacrosScreen
import com.example.ui.screens.ReceiverDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TouchpadScreen
import com.example.ui.screens.VirtualPreviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

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
 * Implements canonical responsive layout switching:
 * - On compact handheld screens (< 600.dp): uses a bottom [NavigationBar] and [Scaffold].
 * - On expanded screens (>= 600.dp, e.g. Chromebooks, landscape tablets, ChromeOS ARCVM):
 *   uses a left-aligned [NavigationRail] with keyboard shortcut navigation (1-6, Esc).
 *
 * @param viewModel The shared [MainViewModel] supplying state and event dispatching.
 */
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()

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

    // Predictive back handler: Navigate back to Touchpad if on another screen
    if (currentScreen != AppScreen.TOUCHPAD) {
        BackHandler {
            viewModel.selectScreen(AppScreen.TOUCHPAD)
        }
    }

    val navItems = listOf(
        AppScreen.TOUCHPAD to Icons.Default.TouchApp,
        AppScreen.KEYBOARD to Icons.Default.Keyboard,
        AppScreen.MACROS to Icons.Default.Bolt,
        AppScreen.VIRTUAL_SCREEN to Icons.Default.Computer,
        AppScreen.RECEIVER to Icons.Default.LaptopChromebook,
        AppScreen.SETTINGS to Icons.Default.Tune
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onKeyEvent { event ->
                // Support ChromeOS physical keyboard shortcuts
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            if (showConnectionDialog) {
                                showConnectionDialog = false
                                true
                            } else if (currentScreen != AppScreen.TOUCHPAD) {
                                viewModel.selectScreen(AppScreen.TOUCHPAD)
                                true
                            } else {
                                false
                            }
                        }
                        Key.One -> { viewModel.selectScreen(AppScreen.TOUCHPAD); true }
                        Key.Two -> { viewModel.selectScreen(AppScreen.KEYBOARD); true }
                        Key.Three -> { viewModel.selectScreen(AppScreen.MACROS); true }
                        Key.Four -> { viewModel.selectScreen(AppScreen.VIRTUAL_SCREEN); true }
                        Key.Five -> { viewModel.selectScreen(AppScreen.RECEIVER); true }
                        Key.Six -> { viewModel.selectScreen(AppScreen.SETTINGS); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
    ) {
        // Detect wide / desktop layout (e.g. Chromebooks, landscape tablets, freeform desktop windows)
        val isExpandedLayout = maxWidth >= 600.dp

        if (isExpandedLayout) {
            // Adaptive Desktop Layout: Vertical NavigationRail on left, main content on right
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = SurfaceDark800,
                    modifier = Modifier
                        .fillMaxHeight()
                        .border(width = 0.5.dp, color = SurfaceDark700)
                        .testTag("desktop_navigation_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LaptopChromebook,
                                    contentDescription = "GlidePad ChromeOS",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "GLIDEPAD",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                ) {
                    navItems.forEach { (screen, icon) ->
                        val isSelected = currentScreen == screen
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.selectScreen(screen) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) NeonCyan else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    color = if (isSelected) NeonCyan else TextMuted
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                                selectedIconColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                selectedTextColor = NeonCyan,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_tab_${screen.name.lowercase()}")
                        )
                    }
                }

                // Desktop Main Stage
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ConnectionHeader(
                        stats = stats,
                        onOpenConnectionDialog = { showConnectionDialog = true },
                        onOpenSettings = { viewModel.selectScreen(AppScreen.SETTINGS) }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("desktop_content_stage")
                    ) {
                        when (currentScreen) {
                            AppScreen.TOUCHPAD -> TouchpadScreen(viewModel = viewModel, settings = settings)
                            AppScreen.KEYBOARD -> KeyboardScreen(viewModel = viewModel)
                            AppScreen.MACROS -> MacrosScreen(viewModel = viewModel)
                            AppScreen.VIRTUAL_SCREEN -> VirtualPreviewScreen(viewModel = viewModel)
                            AppScreen.RECEIVER -> ReceiverDashboardScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, settings = settings)
                        }
                    }
                }
            }
        } else {
            // Compact Handheld Mobile Layout: Standard bottom NavigationBar
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceDark900),
                topBar = {
                    ConnectionHeader(
                        stats = stats,
                        onOpenConnectionDialog = { showConnectionDialog = true },
                        onOpenSettings = { viewModel.selectScreen(AppScreen.SETTINGS) }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = SurfaceDark800,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .border(width = 0.5.dp, color = SurfaceDark700)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        navItems.forEach { (screen, icon) ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectScreen(screen) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) NeonCyan else TextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 9.sp,
                                        color = if (isSelected) NeonCyan else TextMuted,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = NeonCyan.copy(alpha = 0.15f),
                                    selectedIconColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = NeonCyan,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        AppScreen.TOUCHPAD -> TouchpadScreen(viewModel = viewModel, settings = settings)
                        AppScreen.KEYBOARD -> KeyboardScreen(viewModel = viewModel)
                        AppScreen.MACROS -> MacrosScreen(viewModel = viewModel)
                        AppScreen.VIRTUAL_SCREEN -> VirtualPreviewScreen(viewModel = viewModel)
                        AppScreen.RECEIVER -> ReceiverDashboardScreen(viewModel = viewModel)
                        AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, settings = settings)
                    }
                }
            }
        }
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            viewModel = viewModel,
            onDismiss = { showConnectionDialog = false }
        )
    }
}
