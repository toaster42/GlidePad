package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TouchpadEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import kotlin.math.sqrt

/**
 * Transient contact point tracked on the multitouch surface for visual finger tracking indicators.
 *
 * @property id Pointer contact ID.
 * @property x Local horizontal coordinate in pixels.
 * @property y Local vertical coordinate in pixels.
 */
data class TouchPoint(val id: Long, val x: Float, val y: Float)

/**
 * Reusable multitouch trackpad surface backing both standard and Float-mode layouts.
 *
 * Captures 1- to 4-finger pointer streams, delegates pointer kinematics and gesture recognition
 * to [engine], and renders active finger rings, a center crosshair, an optional gesture toast,
 * and optional bottom gesture hints.
 *
 * @param engine [TouchpadEngine] receiving pointer deltas, scrolling, taps, and swipes.
 * @param modifier Composable layout modifier applied to the root surface container.
 * @param showGestureHints Whether to display the bottom gesture helper row.
 * @param surfaceTestTag Test tag applied to the surface container for UI verification.
 */
@Composable
fun TouchpadSurface(
    engine: TouchpadEngine,
    modifier: Modifier = Modifier,
    showGestureHints: Boolean = true,
    surfaceTestTag: String = "main_touchpad_surface"
) {
    val activePointers = remember { mutableStateListOf<TouchPoint>() }
    var gestureToast by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark800)
            .border(1.5.dp, SurfaceDark700, RoundedCornerShape(20.dp))
            .testTag(surfaceTestTag)
            .pointerInput(engine) {
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
                            val totalMoveDistance = sqrt(gestureTotalDx * gestureTotalDx + gestureTotalDy * gestureTotalDy)

                            if (duration < 350 && totalMoveDistance < 25f) {
                                // Taps
                                when (maxFingerCount) {
                                    1 -> engine.handleOneFingerTap()
                                    2 -> engine.handleTwoFingerTap()
                                    3 -> engine.handleThreeFingerTap()
                                    4 -> engine.handleFourFingerTap()
                                }
                            } else if (maxFingerCount >= 3 && totalMoveDistance > 80f) {
                                // 3 or 4 finger Swipes
                                engine.handleSwipe(maxFingerCount, gestureTotalDx, gestureTotalDy)
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

                                    engine.processPointerMove(dx, dy, deltaTimeMs)
                                    change.consume()
                                }
                                2 -> {
                                    // 2-finger movement: Smooth scrolling
                                    val avgDy = (downPointers[0].positionChange().y + downPointers[1].positionChange().y) / 2f
                                    val avgDx = (downPointers[0].positionChange().x + downPointers[1].positionChange().x) / 2f
                                    gestureTotalDx += avgDx
                                    gestureTotalDy += avgDy

                                    engine.processScroll(avgDy, avgDx)
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
        AnimatedVisibility(
            visible = gestureToast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
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

        if (showGestureHints) {
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
    }
}
