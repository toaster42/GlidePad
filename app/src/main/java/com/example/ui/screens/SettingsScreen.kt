package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.AccelerationProfile
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.HapticLevel
import com.example.model.TouchpadSettings
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark600
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settings: TouchpadSettings,
    modifier: Modifier = Modifier
) {
    var selectedGestureForEdit by remember { mutableStateOf<GestureType?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Preferences & Tuning",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Fine-tune mouse sensitivity, acceleration curves, and gestures.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Section 1: Mouse Sensitivity & Pointer Speed
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pointer Sensitivity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fx", settings.pointerSensitivity),
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Controls baseline cursor travel speed across the PC screen.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val newVal = (settings.pointerSensitivity - 0.05f).coerceAtLeast(0.2f)
                                viewModel.setPointerSensitivity(newVal)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark700)
                                .testTag("sensitivity_minus_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                        }

                        Slider(
                            value = settings.pointerSensitivity,
                            onValueChange = { viewModel.setPointerSensitivity(it) },
                            valueRange = 0.2f..4.0f,
                            steps = 37,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = SurfaceDark700
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .testTag("sensitivity_slider")
                        )

                        IconButton(
                            onClick = {
                                val newVal = (settings.pointerSensitivity + 0.05f).coerceAtMost(4.0f)
                                viewModel.setPointerSensitivity(newVal)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark700)
                                .testTag("sensitivity_plus_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                        }
                    }

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "0.8x" to 0.8f,
                            "1.25x" to 1.25f,
                            "1.75x" to 1.75f,
                            "2.5x" to 2.5f
                        ).forEach { (label, value) ->
                            val isSelected = kotlin.math.abs(settings.pointerSensitivity - value) < 0.04f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceDark700)
                                    .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPointerSensitivity(value) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Pointer Acceleration & Curves
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pointer Acceleration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dynamic speed scaling based on finger velocity.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Acceleration Profiles
                    AccelerationProfile.values().forEach { profile ->
                        val isSelected = settings.accelerationProfile == profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricBlue.copy(alpha = 0.12f) else SurfaceDark700.copy(alpha = 0.5f))
                                .border(1.dp, if (isSelected) ElectricBlue.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAccelerationProfile(profile) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setAccelerationProfile(profile) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricBlue,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = profile.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Acceleration Multiplier (visible when not Linear)
                    AnimatedVisibility(visible = settings.accelerationProfile != AccelerationProfile.LINEAR) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Acceleration Factor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", settings.accelerationMultiplier),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                            }
                            Slider(
                                value = settings.accelerationMultiplier,
                                onValueChange = { viewModel.setAccelerationMultiplier(it) },
                                valueRange = 0.2f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricBlue,
                                    activeTrackColor = ElectricBlue,
                                    inactiveTrackColor = SurfaceDark700
                                ),
                                modifier = Modifier.testTag("acceleration_slider")
                            )
                        }
                    }

                    // Dynamic Live Graph Visualization of Acceleration Curve
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Curve Response Preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark900)
                            .border(1.dp, SurfaceDark700, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        AccelerationCurveCanvas(
                            profile = settings.accelerationProfile,
                            multiplier = settings.accelerationMultiplier,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // In-settings Interactive Test Track
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Interactive Test Bed (Glide your finger here)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    InteractiveTestPad(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    )
                }
            }
        }

        // Section 3: Scrolling & Two-Finger Navigation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scroll & Navigation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust two-finger scroll speed and direction.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scroll Sensitivity",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.1fx", settings.scrollSensitivity),
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = settings.scrollSensitivity,
                        onValueChange = { viewModel.setScrollSensitivity(it) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceDark700
                        ),
                        modifier = Modifier.testTag("scroll_sensitivity_slider")
                    )

                    SettingToggleRow(
                        title = "Natural Scrolling",
                        subtitle = "Content moves smoothly in the direction of finger motion",
                        checked = settings.naturalScroll,
                        onCheckedChange = { viewModel.setNaturalScroll(it) },
                        tag = "natural_scroll_switch"
                    )

                    SettingToggleRow(
                        title = "Tap to Click",
                        subtitle = "1-finger tap performs a Left Click",
                        checked = settings.tapToClick,
                        onCheckedChange = { viewModel.setTapToClick(it) },
                        tag = "tap_click_switch"
                    )

                    SettingToggleRow(
                        title = "Two-Finger Right Click",
                        subtitle = "2-finger tap displays contextual menu",
                        checked = settings.twoFingerRightClick,
                        onCheckedChange = { viewModel.setTwoFingerRightClick(it) },
                        tag = "two_finger_right_switch"
                    )

                    SettingToggleRow(
                        title = "Drag Lock",
                        subtitle = "Keep dragging even when briefly lifting finger",
                        checked = settings.dragLock,
                        onCheckedChange = { viewModel.setDragLock(it) },
                        tag = "drag_lock_switch"
                    )
                }
            }
        }

        // Section 4: Haptic Feedback
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Haptic Tactile Feedback",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticLevel.values().forEach { level ->
                            val isSelected = settings.hapticLevel == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else SurfaceDark700)
                                    .border(1.dp, if (isSelected) AccentPurple else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setHapticLevel(level) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AccentPurple else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Customizable Gestures Directory
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gesture Customization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assign desktop shortcuts and macros to multi-touch gestures.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GestureType.values().forEach { gesture ->
                        val currentAction = settings.gestures[gesture] ?: GestureAction.NONE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGestureForEdit = gesture }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gesture.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = gesture.description,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceDark700)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentAction.title,
                                        fontSize = 11.sp,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (gesture != GestureType.values().last()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SurfaceDark700)
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Reset to Default Settings
        item {
            Button(
                onClick = { viewModel.resetSettingsToDefault() },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_defaults_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings to Defaults", color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Gesture Action Picker Modal Dialog
    selectedGestureForEdit?.let { gesture ->
        AlertDialog(
            onDismissRequest = { selectedGestureForEdit = null },
            title = {
                Text(
                    text = "Assign Action to ${gesture.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(GestureAction.values().size) { index ->
                        val action = GestureAction.values()[index]
                        val isSelected = (settings.gestures[gesture] ?: GestureAction.NONE) == action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setGestureAction(gesture, action)
                                    selectedGestureForEdit = null
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = action.title,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGestureForEdit = null }) {
                    Text("Close", color = NeonCyan)
                }
            },
            containerColor = SurfaceDark800,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceDark700
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun AccelerationCurveCanvas(
    profile: AccelerationProfile,
    multiplier: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw grid lines
        drawLine(SurfaceDark700, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = 1f)
        drawLine(SurfaceDark700, Offset(width * 0.5f, 0f), Offset(width * 0.5f, height), strokeWidth = 1f)

        val path = Path()
        val steps = 60

        for (i in 0..steps) {
            val progress = i.toFloat() / steps // 0..1 (hand speed)
            val outputSpeed = when (profile) {
                AccelerationProfile.LINEAR -> progress
                AccelerationProfile.DYNAMIC -> {
                    if (progress < 0.3f) progress * 0.85f
                    else 0.25f + (progress - 0.3f).pow(1.3f) * (1.0f + (multiplier - 1.0f) * 0.5f)
                }
                AccelerationProfile.PRECISION -> {
                    if (progress < 0.35f) progress * 0.5f
                    else 0.17f + (progress - 0.35f).pow(1.8f) * (1.2f + (multiplier - 1.0f) * 0.6f)
                }
            }.coerceIn(0f, 1.2f)

            val x = progress * width
            val y = height - (outputSpeed / 1.2f) * height

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun InteractiveTestPad(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    var lastTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var touchActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark900)
            .border(1.dp, if (touchActive) NeonCyan else SurfaceDark600, RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lastX = offset.x
                        lastY = offset.y
                        lastTimeMs = System.currentTimeMillis()
                        touchActive = true
                    },
                    onDragEnd = {
                        touchActive = false
                    },
                    onDragCancel = {
                        touchActive = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val now = System.currentTimeMillis()
                        val dt = (now - lastTimeMs).coerceAtLeast(1)
                        viewModel.touchpadEngine.processPointerMove(dragAmount.x, dragAmount.y, dt)
                        lastTimeMs = now
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (touchActive) "Tracking Velocity & Acceleration..." else "Swipe here to feel sensitivity",
            fontSize = 11.sp,
            color = if (touchActive) NeonCyan else TextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}
