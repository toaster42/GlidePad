package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
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

@Composable
fun VirtualPreviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val virtualScreen by viewModel.virtualScreen.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Virtual PC Desktop",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Interactive cursor simulation and live HID packet stream.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentEmerald.copy(alpha = 0.15f))
                    .border(1.dp, AccentEmerald, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${stats.lastLatencyMs} ms • ${stats.currentHz} Hz",
                    color = AccentEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtual Monitor Display
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, SurfaceDark700),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Canvas rendering desktop UI elements and the mouse pointer
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Taskbar at bottom
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, h - 26.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(w, 26.dp.toPx())
                    )

                    // Mock desktop window 1
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(w * 0.15f, h * 0.12f),
                        size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.55f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                    // Window title bar
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(w * 0.15f, h * 0.12f),
                        size = androidx.compose.ui.geometry.Size(w * 0.5f, 16.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )

                    // Mock desktop window 2
                    drawRoundRect(
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.42f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.52f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )

                    // Draw Click Ripple if active
                    virtualScreen.clickRippleX?.let { rx ->
                        virtualScreen.clickRippleY?.let { ry ->
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.35f),
                                radius = 18.dp.toPx(),
                                center = Offset(rx * w, ry * h)
                            )
                        }
                    }

                    // Draw PC Mouse Pointer (White cursor arrow with shadow)
                    val px = virtualScreen.cursorX * w
                    val py = virtualScreen.cursorY * h

                    val cursorPath = Path().apply {
                        moveTo(px, py)
                        lineTo(px, py + 18.dp.toPx())
                        lineTo(px + 5.dp.toPx(), py + 14.dp.toPx())
                        lineTo(px + 10.dp.toPx(), py + 22.dp.toPx())
                        lineTo(px + 13.dp.toPx(), py + 20.dp.toPx())
                        lineTo(px + 8.dp.toPx(), py + 12.dp.toPx())
                        lineTo(px + 14.dp.toPx(), py + 12.dp.toPx())
                        close()
                    }

                    drawPath(path = cursorPath, color = Color.Black, style = Stroke(width = 3.dp.toPx()))
                    drawPath(path = cursorPath, color = Color.White)
                }

                // Recent gesture popup overlay
                virtualScreen.recentGestureNotification?.let { notif ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark900.copy(alpha = 0.9f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Gesture: $notif",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live HID Packet Logs
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ListAlt,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Live HID Packet Stream (${stats.packetsSent} packets sent)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (stats.recentLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Touchpad movements and keystrokes will stream here.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(stats.recentLogs) { logLine ->
                        Text(
                            text = logLine,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (logLine.contains("MOUSE")) NeonCyan else if (logLine.contains("KEY") || logLine.contains("MACRO")) ElectricBlue else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
