package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.engine.BenchmarkState
import com.example.service.NetworkStreamManager
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun BenchmarkDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.benchmarkState.collectAsState()
    val pcIp = viewModel.networkManager.pcIpAddress
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (state !is BenchmarkState.Running) {
                viewModel.resetBenchmark()
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Network Benchmark",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Measure RTT latency, jitter & throughput to $pcIp",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (val currState = state) {
                    is BenchmarkState.Idle -> {
                        Text(
                            text = "This benchmark evaluates Wi-Fi UDP transmission health against '${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_FILENAME}' (v${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_VERSION}) on your PC:\n\n" +
                                    "• 40-packet Latency & RFC 3550 Jitter test\n" +
                                    "• 120Hz high-frequency motion packet burst\n" +
                                    "• Packet loss and out-of-order analysis\n" +
                                    "• Performance grading (A+ to C)",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark900)
                                .border(1.dp, SurfaceDark700, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Host: $pcIp:8089",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricBlue
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Command: python ${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_FILENAME}",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GlidePad Benchmark Receiver", NetworkStreamManager.PYTHON_BENCHMARK_RECEIVER_SCRIPT)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_FILENAME} (v${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_VERSION}) to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_benchmark_script_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Benchmark Script (v${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_VERSION})", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    is BenchmarkState.Running -> {
                        Text(
                            text = currState.stage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { currState.progress },
                            color = NeonCyan,
                            trackColor = SurfaceDark700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currState.currentLatencyMs > 0) {
                                Text("Live RTT: ${currState.currentLatencyMs}ms", fontSize = 11.sp, color = TextSecondary)
                            }
                            if (currState.currentHz > 0) {
                                Text("Rate: ${currState.currentHz.toInt()} Hz", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    is BenchmarkState.Completed -> {
                        val report = currState.report
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark900),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Grade: ${report.grade.take(2)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (report.grade.startsWith("A")) AccentEmerald else AccentAmber
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", report.avgLatencyMs)}ms Avg",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                                Text(
                                    text = report.grade,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = report.recommendation,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid metrics
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MetricRow("Min / P50 / Max Latency", "${report.minLatencyMs}ms / ${report.medianLatencyMs}ms / ${report.maxLatencyMs}ms")
                            MetricRow("P90 / P95 / P99 Latency", "${report.p90LatencyMs}ms / ${report.p95LatencyMs}ms / ${report.p99LatencyMs}ms")
                            MetricRow("RFC 3550 Jitter", "${String.format(Locale.US, "%.2f", report.jitterMs)}ms (σ ${String.format(Locale.US, "%.2f", report.jitterStdDevMs)}ms)")
                            MetricRow("Packet Loss", "${String.format(Locale.US, "%.1f", report.pingLossPct)}% (${report.pingsSent - report.pongsReceived} lost)")
                            MetricRow("Stream Throughput", "${String.format(Locale.US, "%.1f", report.streamMeasuredHz)} Hz")
                            MetricRow("Stream Packets Acked", "${report.streamPacketsAcked} / ${report.streamPacketsSent}")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val summary = buildString {
                                    appendLine("GlidePad Network Benchmark Report")
                                    appendLine("Target: ${report.targetIp}:${report.targetPort}")
                                    appendLine("Grade: ${report.grade}")
                                    appendLine("Min/P50/Max Latency: ${report.minLatencyMs}ms / ${report.medianLatencyMs}ms / ${report.maxLatencyMs}ms")
                                    appendLine("P90/P95/P99 Latency: ${report.p90LatencyMs}ms / ${report.p95LatencyMs}ms / ${report.p99LatencyMs}ms")
                                    appendLine("RFC 3550 Jitter: ${String.format(Locale.US, "%.2f", report.jitterMs)}ms (std dev: ${String.format(Locale.US, "%.2f", report.jitterStdDevMs)}ms)")
                                    appendLine("Packet Loss: ${String.format(Locale.US, "%.1f", report.pingLossPct)}% (${report.pingsSent - report.pongsReceived} lost)")
                                    appendLine("Stream Hz: ${String.format(Locale.US, "%.1f", report.streamMeasuredHz)} Hz (${report.streamPacketsAcked}/${report.streamPacketsSent} acked)")
                                    appendLine("Recommendation: ${report.recommendation}")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GlidePad Benchmark", summary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Benchmark diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_benchmark_summary_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Diagnostic Summary", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    is BenchmarkState.Error -> {
                        Text(
                            text = currState.message,
                            fontSize = 12.sp,
                            color = AccentAmber,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GlidePad Benchmark Receiver", NetworkStreamManager.PYTHON_BENCHMARK_RECEIVER_SCRIPT)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_FILENAME} (v${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_VERSION}) to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_benchmark_script_error_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Benchmark Script (${NetworkStreamManager.BENCHMARK_RECEIVER_SCRIPT_FILENAME})", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is BenchmarkState.Idle, is BenchmarkState.Error -> {
                    Button(
                        onClick = { viewModel.runNetworkBenchmark() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.testTag("start_benchmark_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SurfaceDark900, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Benchmark", color = SurfaceDark900, fontWeight = FontWeight.Bold)
                    }
                }
                is BenchmarkState.Completed -> {
                    Button(
                        onClick = { viewModel.runNetworkBenchmark() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.testTag("rerun_benchmark_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SurfaceDark900, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Again", color = SurfaceDark900, fontWeight = FontWeight.Bold)
                    }
                }
                is BenchmarkState.Running -> {
                    // Running
                }
            }
        },
        dismissButton = {
            if (state !is BenchmarkState.Running) {
                TextButton(
                    onClick = {
                        viewModel.resetBenchmark()
                        onDismiss()
                    }
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        },
        containerColor = SurfaceDark800,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceDark700.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
