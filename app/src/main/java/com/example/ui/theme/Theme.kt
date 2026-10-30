package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SurfaceDark900,
    primaryContainer = DeepCyan,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlue,
    onSecondary = SurfaceDark900,
    secondaryContainer = SurfaceDark700,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentPurple,
    onTertiary = TextPrimary,
    background = SurfaceDark900,
    onBackground = TextPrimary,
    surface = SurfaceDark800,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark700,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceDark600
)

private val LightColorScheme = darkColorScheme(
    // Trackpad app is optimized for OLED dark mode by default for low latency and zero eye fatigue
    primary = NeonCyan,
    onPrimary = SurfaceDark900,
    primaryContainer = DeepCyan,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlue,
    onSecondary = SurfaceDark900,
    background = SurfaceDark900,
    surface = SurfaceDark800,
    outline = SurfaceDark600
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark for touchpad usability
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.statusBarColor = SurfaceDark900.toArgb()
                it.navigationBarColor = SurfaceDark900.toArgb()
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
