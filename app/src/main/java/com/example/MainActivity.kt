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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConnectionHeader
import com.example.ui.dialogs.ConnectionDialog
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MacrosScreen
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                val items = listOf(
                    AppScreen.TOUCHPAD to Icons.Default.TouchApp,
                    AppScreen.KEYBOARD to Icons.Default.Keyboard,
                    AppScreen.MACROS to Icons.Default.Bolt,
                    AppScreen.VIRTUAL_SCREEN to Icons.Default.Computer,
                    AppScreen.SETTINGS to Icons.Default.Tune
                )

                items.forEach { (screen, icon) ->
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
                                fontSize = 10.sp,
                                color = if (isSelected) NeonCyan else TextMuted
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
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, settings = settings)
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
