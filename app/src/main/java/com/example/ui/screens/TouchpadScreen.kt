package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.engine.TouchpadEngine
import com.example.model.TouchpadSettings
import com.example.ui.components.TouchpadSurface
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Primary touchpad screen composable wired to [MainViewModel].
 *
 * @param viewModel Shared [MainViewModel] providing gesture dispatch, sensitivity updates, and haptics.
 * @param settings Active [TouchpadSettings] configuring sensitivity, ballistics, and gestures.
 * @param modifier Composable layout modifier.
 * @param onToggleFloat Callback invoked when the user taps the Float mode toggle button.
 */
@Composable
fun TouchpadScreen(
    viewModel: MainViewModel,
    settings: TouchpadSettings,
    modifier: Modifier = Modifier,
    onToggleFloat: () -> Unit = {}
) {
    TouchpadScreenContent(
        engine = viewModel.touchpadEngine,
        settings = settings,
        onSensitivityChange = { viewModel.setPointerSensitivity(it) },
        onButtonHaptic = { viewModel.hapticHelper.click(settings.hapticLevel) },
        modifier = modifier,
        onToggleFloat = onToggleFloat
    )
}

/**
 * Stateless-host touchpad screen content backed directly by [TouchpadEngine].
 *
 * Renders the quick controls bar (drag lock, sensitivity popover, Float toggle), the full-width
 * [TouchpadSurface], and the optional physical click/scroll bar.
 *
 * @param engine [TouchpadEngine] handling pointer movement, scrolling, taps, swipes, and button state.
 * @param settings Active [TouchpadSettings] configuring sensitivity and bar visibility.
 * @param onSensitivityChange Callback invoked when the quick sensitivity slider changes.
 * @param onButtonHaptic Callback invoked when a physical virtual mouse button is pressed.
 * @param modifier Composable layout modifier.
 * @param onToggleFloat Callback invoked when the user taps the Float mode toggle button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchpadScreenContent(
    engine: TouchpadEngine,
    settings: TouchpadSettings,
    onSensitivityChange: (Float) -> Unit,
    onButtonHaptic: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFloat: () -> Unit = {}
) {
    var showQuickSensSlider by remember { mutableStateOf(false) }
    val isDragLockEngaged by engine.isDragging.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Controls Bar atop Trackpad
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("touchpad_quick_controls"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Drag Lock Status
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDragLockEngaged) AccentAmber.copy(alpha = 0.2f) else SurfaceDark800)
                    .border(1.dp, if (isDragLockEngaged) AccentAmber else SurfaceDark700, RoundedCornerShape(8.dp))
                    .clickable {
                        if (isDragLockEngaged) {
                            engine.stopDrag()
                        } else {
                            engine.startDrag()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("drag_lock_quick_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDragLockEngaged) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isDragLockEngaged) AccentAmber else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isDragLockEngaged) "Drag Locked" else "Drag Lock",
                    fontSize = 11.sp,
                    color = if (isDragLockEngaged) AccentAmber else TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Sensitivity Badge (Tap to toggle instant slider)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark800)
                    .border(1.dp, SurfaceDark700, RoundedCornerShape(8.dp))
                    .clickable { showQuickSensSlider = !showQuickSensSlider }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("quick_sensitivity_badge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%.2fx %s",
                        settings.pointerSensitivity,
                        settings.accelerationProfile.displayName.substringBefore(" ")
                    ),
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Float Mode Toggle Button
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark800)
                    .border(1.dp, SurfaceDark700, RoundedCornerShape(8.dp))
                    .clickable { onToggleFloat() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .semantics { contentDescription = "Enter float mode" }
                    .testTag("float_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Float",
                    fontSize = 11.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Expandable Quick Sensitivity Slider Popover
        AnimatedVisibility(visible = showQuickSensSlider) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sensitivity Fine-Tuning",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            String.format(Locale.US, "%.2fx", settings.pointerSensitivity),
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.pointerSensitivity,
                        onValueChange = onSensitivityChange,
                        valueRange = 0.2f..4.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceDark700
                        )
                    )
                }
            }
        }

        // Main Touchpad Area
        TouchpadSurface(
            engine = engine,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            showGestureHints = true,
            surfaceTestTag = "main_touchpad_surface"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Physical Buttons Bar (Left, Scroll Strip, Right)
        if (settings.showPhysicalButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("physical_buttons_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Mouse Button
                MouseButtonCard(
                    label = "LEFT CLICK",
                    weight = 2.4f,
                    testTag = "mouse_left_button",
                    modifier = Modifier.weight(2.4f),
                    onPressChange = { pressed ->
                        engine.setPhysicalButtonState(0, pressed)
                        if (pressed) onButtonHaptic()
                    }
                )

                // Dedicated Scroll Strip
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark800)
                        .border(1.dp, SurfaceDark700, RoundedCornerShape(14.dp))
                        .pointerInput(engine) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                engine.processScroll(dragAmount.y, 0f)
                            }
                        }
                        .testTag("scroll_wheel_strip"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Scroll Wheel",
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SCROLL",
                            fontSize = 9.sp,
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right Mouse Button
                MouseButtonCard(
                    label = "RIGHT CLICK",
                    weight = 2.4f,
                    testTag = "mouse_right_button",
                    modifier = Modifier.weight(2.4f),
                    onPressChange = { pressed ->
                        engine.setPhysicalButtonState(1, pressed)
                        if (pressed) onButtonHaptic()
                    }
                )
            }
        }
    }
}

/**
 * Tactile virtual mouse button card supporting press-and-hold state reporting for left/right click bars.
 *
 * @param label Button title displayed in the center of the card (e.g., `LEFT CLICK`, `RIGHT CLICK`).
 * @param weight Layout weight proportion within the bottom mouse button bar.
 * @param testTag UI test tag identifier for automated UI testing.
 * @param onPressChange Callback invoked with `true` on finger down and `false` upon release/cancel.
 * @param modifier Optional [Modifier] applied to the button box.
 */
@Composable
fun MouseButtonCard(
    label: String,
    weight: Float,
    testTag: String,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnPressChange by rememberUpdatedState(onPressChange)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPressed) NeonCyan.copy(alpha = 0.25f) else SurfaceDark800)
            .border(
                1.dp,
                if (isPressed) NeonCyan else SurfaceDark700,
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        currentOnPressChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        currentOnPressChange(false)
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) NeonCyan else TextPrimary,
            letterSpacing = 0.5.sp
        )
    }
}
