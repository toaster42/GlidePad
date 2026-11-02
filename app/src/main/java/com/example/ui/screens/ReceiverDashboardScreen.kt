package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ReceiverDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val receiver = viewModel.inAppReceiverManager
    val isRunning by receiver.isRunning.collectAsState()
    val hostIp by receiver.hostIp.collectAsState()
    val serverPort by receiver.serverPort.collectAsState()
    val pps by receiver.packetsPerSecond.collectAsState()
    val totalPackets by receiver.totalPacketsReceived.collectAsState()
    val lastClientIp by receiver.lastClientIp.collectAsState()
    val activeClients by receiver.activeClients.collectAsState()
    val cursorX by receiver.cursorNormalizedX.collectAsState()
    val cursorY by receiver.cursorNormalizedY.collectAsState()
    val lastButton by receiver.lastButtonState.collectAsState()
    val lastWheel by receiver.lastWheelDelta.collectAsState()
    val lastSummary by receiver.lastReceivedSummary.collectAsState()
    val logs by receiver.recentPacketsLog.collectAsState()

    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("receiver_header_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan.copy(alpha = 0.25f), SurfaceDark700)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LaptopChromebook,
                                    contentDescription = "ChromeOS Host",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ChromeOS Host Receiver",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Built-in low latency UDP server (:8089)",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isRunning) AccentEmerald.copy(alpha = 0.15f) else SurfaceDark700
                                )
                                .border(
                                    1.dp,
                                    if (isRunning) AccentEmerald.copy(alpha = pulseAlpha) else Color.Gray.copy(alpha = 0.4f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) AccentEmerald else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "ACTIVE" else "STANDBY",
                                    color = if (isRunning) AccentEmerald else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleInAppReceiver() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFD32F2F) else NeonCyan,
                                contentColor = if (isRunning) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("receiver_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "Stop Receiver" else "Start Receiver",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                receiver.refreshHostIp()
                                Toast.makeText(context, "Network IP refreshed: ${receiver.hostIp.value}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp).testTag("receiver_refresh_ip_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh IP", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh IP", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 2. IP Connection Box (For Remote Controllers to Connect)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                modifier = Modifier.fillMaxWidth().testTag("receiver_ip_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENTER THIS IP IN GLIDEPAD CONTROLLER:",
                        color = AccentAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hostIp,
                            color = NeonCyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("GlidePad Host IP", hostIp))
                                Toast.makeText(context, "IP $hostIp copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_host_ip_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", tint = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UDP Port: $serverPort • Remote Clients: ${activeClients.size} active",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 3. Real-Time Telemetry Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Throughput PPS
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("THROUGHPUT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$pps Hz",
                            color = if (pps > 0) AccentEmerald else TextSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("packets/sec", color = TextMuted, fontSize = 10.sp)
                    }
                }

                // Total Packets
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("TOTAL RECEIVED", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalPackets",
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("frames delivered", color = TextMuted, fontSize = 10.sp)
                    }
                }

                // Last Action
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("LAST CLIENT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastClientIp ?: "None",
                            color = if (lastClientIp != null) AccentAmber else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = if (lastButton != 0) "Btn: $lastButton" else if (lastWheel != 0) "Wheel: $lastWheel" else "Idle",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 4. Live Visual Pointer HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().height(220.dp).testTag("receiver_hud_canvas")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE INCOMING POINTER HUD",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "X: ${(cursorX * 100).toInt()}% | Y: ${(cursorY * 100).toInt()}%",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070B12))
                            .border(1.dp, SurfaceDark700, RoundedCornerShape(10.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle coordinate grid
                            val gridColor = Color(0xFF142132)
                            drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), 1f)
                            drawLine(gridColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 1f)
                            drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), 1f)
                            drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)

                            // Pointer target position
                            val targetPx = Offset(cursorX * w, cursorY * h)

                            // Outer radar ping
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.25f),
                                radius = 22.dp.toPx(),
                                center = targetPx
                            )

                            // Inner dot
                            drawCircle(
                                color = if (lastButton != 0) AccentRose else NeonCyan,
                                radius = if (lastButton != 0) 9.dp.toPx() else 6.dp.toPx(),
                                center = targetPx
                            )

                            // Crosshair lines
                            val chLen = 14.dp.toPx()
                            drawLine(NeonCyan.copy(alpha = 0.6f), Offset(targetPx.x - chLen, targetPx.y), Offset(targetPx.x + chLen, targetPx.y), 1.5f)
                            drawLine(NeonCyan.copy(alpha = 0.6f), Offset(targetPx.x, targetPx.y - chLen), Offset(targetPx.x, targetPx.y + chLen), 1.5f)
                        }

                        // Status notification overlay
                        Text(
                            text = lastSummary,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // 5. Diagnostics Log
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EVENT LOG",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { viewModel.resetReceiverStats() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Stats", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (logs.isEmpty()) {
                        Text(
                            text = "No events yet. Start the receiver and connect from your phone.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        logs.takeLast(6).forEach { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.contains("Error")) AccentRose else TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
