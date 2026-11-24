package com.example.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted

private val navItems = listOf(
    AppScreen.TOUCHPAD to Icons.Default.TouchApp,
    AppScreen.KEYBOARD to Icons.Default.Keyboard,
    AppScreen.MACROS to Icons.Default.Bolt,
    AppScreen.VIRTUAL_SCREEN to Icons.Default.Computer,
    AppScreen.SETTINGS to Icons.Default.Tune
)

/**
 * Stateless responsive layout shell rendering GlidePad across [LayoutMode.FLOAT],
 * [LayoutMode.EXPANDED], and [LayoutMode.COMPACT] window classes.
 *
 * @param layoutMode Active [LayoutMode] deciding whether to show Float touchpad-only chrome,
 *   the desktop [NavigationRail], or the handheld bottom [NavigationBar].
 * @param currentScreen Currently selected [AppScreen] destination.
 * @param onSelectScreen Callback invoked when the user taps a navigation destination.
 * @param header Header composable (e.g. connection status bar) rendered in non-Float modes.
 * @param floatContent Full-window touchpad and minimal overlay rendered exclusively in [LayoutMode.FLOAT].
 * @param screenContent Destination screen body rendered for [currentScreen] in non-Float modes.
 * @param modifier Composable layout modifier applied to the root layout container.
 */
@Composable
fun MainLayoutFrame(
    layoutMode: LayoutMode,
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    header: @Composable () -> Unit,
    floatContent: @Composable () -> Unit,
    screenContent: @Composable (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    when (layoutMode) {
        LayoutMode.FLOAT -> {
            Box(modifier = modifier.fillMaxSize()) {
                floatContent()
            }
        }

        LayoutMode.EXPANDED -> {
            // Adaptive Desktop Layout: Vertical NavigationRail on left, main content on right
            Row(modifier = modifier.fillMaxSize()) {
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
                            onClick = { onSelectScreen(screen) },
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
                    header()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("desktop_content_stage")
                    ) {
                        screenContent(currentScreen)
                    }
                }
            }
        }

        LayoutMode.COMPACT -> {
            // Compact Handheld Mobile Layout: Standard bottom NavigationBar
            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(SurfaceDark900),
                topBar = {
                    header()
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
                                onClick = { onSelectScreen(screen) },
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
                    screenContent(currentScreen)
                }
            }
        }
    }
}
