package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.TouchpadSettings
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
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
import kotlin.math.abs

/**
 * Transient contact point tracked on the multitouch surface for visual finger tracking indicators.
 *
 * @property id Pointer contact ID.
 * @property x Local horizontal coordinate in pixels.
 * @property y Local vertical coordinate in pixels.
 */
data class TouchPoint(val id: Long, val x: Float, val y: Float)

/**
 * Primary touchpad surface composable with multitouch gesture recognition and haptic feedback.
 *
 * @param viewModel Shared [MainViewModel] providing gesture dispatch and pointer telemetry.
 * @param settings Active [TouchpadSettings] configuring sensitivity, ballistics, and gestures.
 * @param modifier Composable layout modifier.
 */
@Composable
fun TouchpadScreen(
    viewModel: MainViewModel,
    settings: TouchpadSettings,
    modifier: Modifier = Modifier
) {
    val activePointers = remember { mutableStateListOf<TouchPoint>() }
    var gestureToast by remember { mutableStateOf<String?>(null) }
    var showQuickSensSlider by remember { mutableStateOf(false) }
    var isDragLockEngaged by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Controls Bar atop Trackpad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1000.dp)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Lock Status
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDragLockEngaged) AccentAmber.copy(alpha = 0.2f) else SurfaceDark800)
                    .border(1.dp, if (isDragLockEngaged) AccentAmber else SurfaceDark700, RoundedCornerShape(8.dp))
                    .clickable {
                        isDragLockEngaged = !isDragLockEngaged
                        if (isDragLockEngaged) {
                            viewModel.touchpadEngine.startDrag()
                        } else {
                            viewModel.touchpadEngine.stopDrag()
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
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Sensitivity Badge (Tap to toggle instant slider)
            Row(
                modifier = Modifier
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
                    text = String.format(Locale.US, "%.2fx %s", settings.pointerSensitivity, settings.accelerationProfile.displayName.substringBefore(" ")),
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Expandable Quick Sensitivity Slider Popover
        AnimatedVisibility(visible = showQuickSensSlider) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
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
                        Text("Sensitivity Fine-Tuning", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(String.format(Locale.US, "%.2fx", settings.pointerSensitivity), fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.pointerSensitivity,
                        onValueChange = { viewModel.setPointerSensitivity(it) },
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 1000.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark800)
                .border(1.5.dp, SurfaceDark700, RoundedCornerShape(20.dp))
                .testTag("main_touchpad_surface")
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var lastMoveTime = System.currentTimeMillis()
                        var gestureTotalDx = 0f
                        var gestureTotalDy = 0f
                        var maxFingerCount = 0
                        var touchStartTime = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            val now = System.currentTimeMillis()

                            // Update active pointer positions for rendering visual rings
                            activePointers.clear()
                            pointers.forEach { p ->
                                if (p.pressed) {
                                    activePointers.add(TouchPoint(p.id.value, p.position.x, p.position.y))
                                }
                            }

                            val downPointers = pointers.filter { it.pressed }

                            if (downPointers.size > maxFingerCount) {
                                maxFingerCount = downPointers.size
                            }

                            if (downPointers.isEmpty()) {
                                // All fingers lifted: evaluate gestures
                                val duration = now - touchStartTime
                                val totalMoveDistance = kotlin.math.sqrt(gestureTotalDx * gestureTotalDx + gestureTotalDy * gestureTotalDy)

                                if (duration < 350 && totalMoveDistance < 25f) {
                                    // Taps
                                    when (maxFingerCount) {
                                        1 -> viewModel.touchpadEngine.handleOneFingerTap()
                                        2 -> viewModel.touchpadEngine.handleTwoFingerTap()
                                        3 -> viewModel.touchpadEngine.handleThreeFingerTap()
                                        4 -> viewModel.touchpadEngine.handleFourFingerTap()
                                    }
                                } else if (maxFingerCount >= 3 && totalMoveDistance > 80f) {
                                    // 3 or 4 finger Swipes
                                    viewModel.touchpadEngine.handleSwipe(maxFingerCount, gestureTotalDx, gestureTotalDy)
                                }

                                maxFingerCount = 0
                                gestureTotalDx = 0f
                                gestureTotalDy = 0f
                                touchStartTime = 0L
                            } else {
                                if (touchStartTime == 0L) {
                                    touchStartTime = now
                                    lastMoveTime = now
                                }

                                val deltaTimeMs = (now - lastMoveTime).coerceAtLeast(1)

                                when (downPointers.size) {
                                    1 -> {
                                        // 1-finger movement: Accelerated mouse cursor
                                        val change = downPointers[0]
                                        val dx = change.positionChange().x
                                        val dy = change.positionChange().y
                                        gestureTotalDx += dx
                                        gestureTotalDy += dy

                                        viewModel.touchpadEngine.processPointerMove(dx, dy, deltaTimeMs)
                                        change.consume()
                                    }
                                    2 -> {
                                        // 2-finger movement: Smooth scrolling
                                        val avgDy = (downPointers[0].positionChange().y + downPointers[1].positionChange().y) / 2f
                                        val avgDx = (downPointers[0].positionChange().x + downPointers[1].positionChange().x) / 2f
                                        gestureTotalDx += avgDx
                                        gestureTotalDy += avgDy

                                        viewModel.touchpadEngine.processScroll(avgDy, avgDx)
                                        downPointers.forEach { it.consume() }
                                    }
                                    else -> {
                                        // 3 or 4 fingers tracking for swipe gesture
                                        val avgDy = downPointers.map { it.positionChange().y }.average().toFloat()
                                        val avgDx = downPointers.map { it.positionChange().x }.average().toFloat()
                                        gestureTotalDx += avgDx
                                        gestureTotalDy += avgDy
                                        downPointers.forEach { it.consume() }
                                    }
                                }
                                lastMoveTime = now
                            }
                        }
                    }
                }
        ) {
            // Touchpad decorative grid lines and subtle center accent
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Center crosshair marker
                drawLine(
                    color = SurfaceDark700.copy(alpha = 0.6f),
                    start = Offset(cx - 16f, cy),
                    end = Offset(cx + 16f, cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = SurfaceDark700.copy(alpha = 0.6f),
                    start = Offset(cx, cy - 16f),
                    end = Offset(cx, cy + 16f),
                    strokeWidth = 1.5f
                )

                // Render glowing finger touch rings
                activePointers.forEach { pt ->
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.15f),
                        radius = 42.dp.toPx(),
                        center = Offset(pt.x, pt.y)
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.4f),
                        radius = 24.dp.toPx(),
                        center = Offset(pt.x, pt.y)
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(pt.x, pt.y)
                    )
                }
            }

            // Gesture Notification Banner overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = gestureToast != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark900.copy(alpha = 0.9f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = gestureToast ?: "",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Gesture Helper guide at bottom of touchpad
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "1-Finger: Cursor",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "2-Finger: Scroll & Right-Click",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "3-Finger: Task View",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Physical Buttons Bar (Left, Scroll Strip, Middle, Right)
        if (settings.showPhysicalButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1000.dp)
                    .height(68.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Mouse Button
                MouseButtonCard(
                    label = "LEFT CLICK",
                    weight = 2.4f,
                    testTag = "mouse_left_button",
                    onPressChange = { pressed ->
                        viewModel.touchpadEngine.setPhysicalButtonState(0, pressed)
                        if (pressed) viewModel.hapticHelper.click(settings.hapticLevel)
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
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.touchpadEngine.processScroll(dragAmount.y, 0f)
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
                    onPressChange = { pressed ->
                        viewModel.touchpadEngine.setPhysicalButtonState(1, pressed)
                        if (pressed) viewModel.hapticHelper.click(settings.hapticLevel)
                    }
                )
            }
        }
    }
}

@Composable
fun MouseButtonCard(
    label: String,
    weight: Float,
    testTag: String,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

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
                        onPressChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPressChange(false)
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
