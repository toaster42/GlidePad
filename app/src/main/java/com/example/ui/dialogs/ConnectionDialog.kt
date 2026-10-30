package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.ConnectionMode
import com.example.service.NetworkStreamManager
import com.example.service.WifiConnectionTestState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRose
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectionDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentStats by viewModel.stats.collectAsState()
    var selectedTabIndex by remember {
        mutableIntStateOf(
            when (currentStats.connectionMode) {
                ConnectionMode.BLUETOOTH_HID -> 0
                ConnectionMode.LOCAL_WIFI -> 1
                ConnectionMode.SIMULATION -> 2
            }
        )
    }

    var pcIpInput by remember { mutableStateOf(viewModel.networkManager.pcIpAddress) }
    val testState by viewModel.networkManager.testState.collectAsState()
    val pairedDevices = remember { viewModel.bluetoothManager.getBondedDevices() }
    val phoneIp = remember { viewModel.networkManager.getDeviceLocalIp() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Connect to PC",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mode Selector Tabs (Bluetooth, Wi-Fi, Simulator, Guide)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark700)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Bluetooth", "Wi-Fi", "Simulator", "Guide").forEachIndexed { index, label ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else Color.Transparent)
                                .clickable {
                                    selectedTabIndex = index
                                    when (index) {
                                        0 -> viewModel.switchConnectionMode(ConnectionMode.BLUETOOTH_HID)
                                        1 -> viewModel.switchConnectionMode(ConnectionMode.LOCAL_WIFI, pcIpInput)
                                        2 -> viewModel.switchConnectionMode(ConnectionMode.SIMULATION)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SurfaceDark900 else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTabIndex) {
                    0 -> {
                        // Bluetooth HID Tab
                        Text(
                            text = "Bluetooth Low-Latency HID Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No PC client software required! Standard Bluetooth HID operates directly with Windows, macOS, and Linux kernel drivers.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Paired Devices (${pairedDevices.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (pairedDevices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark700)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No paired PCs detected. On your PC, open Bluetooth Settings, select your phone, and pair. No software installation needed!",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.height(120.dp)) {
                                items(pairedDevices) { device ->
                                    @android.annotation.SuppressLint("MissingPermission")
                                    val devName = device.name ?: "Unknown Device"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceDark700)
                                            .clickable {
                                                viewModel.connectToBluetoothDevice(device)
                                                onDismiss()
                                            }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Devices, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = devName, fontSize = 12.sp, color = TextPrimary)
                                        }
                                        Text(text = "Connect", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    1 -> {
                        // Wi-Fi UDP Tab
                        Text(
                            text = "Wi-Fi LAN Connection & Diagnostics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect over local Wi-Fi by running the receiver script on your PC.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Device Network Info Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark700)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("This Phone's Wi-Fi IP:", fontSize = 11.sp, color = TextMuted)
                                Text(phoneIp, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // PC IP Address Input Field
                        OutlinedTextField(
                            value = pcIpInput,
                            onValueChange = {
                                pcIpInput = it.trim()
                                viewModel.networkManager.pcIpAddress = it.trim()
                                viewModel.preferencesManager.setSavedPcIp(it.trim())
                            },
                            label = { Text("PC IP Address (from receiver script)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = SurfaceDark700
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pc_ip_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Ping Test & Auto Detect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.networkManager.testConnection(pcIpInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ping_test_button")
                            ) {
                                if (testState is WifiConnectionTestState.Testing) {
                                    CircularProgressIndicator(
                                        color = SurfaceDark900,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...", fontSize = 11.sp, color = SurfaceDark900, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SurfaceDark900, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Ping", fontSize = 11.sp, color = SurfaceDark900, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.networkManager.discoverPcOnNetwork()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auto_detect_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Detect", fontSize = 11.sp, color = NeonCyan)
                            }
                        }

                        // Diagnostic Feedback Box
                        when (val state = testState) {
                            is WifiConnectionTestState.Success -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentEmerald.copy(alpha = 0.15f))
                                        .border(1.dp, AccentEmerald, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Connected! Latency: ${state.latencyMs}ms", color = AccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("${state.serverName} at ${state.ip}", color = TextPrimary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            is WifiConnectionTestState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentRose.copy(alpha = 0.15f))
                                        .border(1.dp, AccentRose, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRose, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Connection Failed", color = AccentRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(state.message, color = TextPrimary, fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                            is WifiConnectionTestState.Testing -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentAmber.copy(alpha = 0.15f))
                                        .border(1.dp, AccentAmber, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text("Sending handshake to $pcIpInput:8089...", color = AccentAmber, fontSize = 11.sp)
                                }
                            }
                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Connect & Apply Button
                        Button(
                            onClick = {
                                viewModel.switchConnectionMode(ConnectionMode.LOCAL_WIFI, pcIpInput)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Wi-Fi Connection", color = SurfaceDark900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Receiver Script Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GlidePad Receiver", NetworkStreamManager.PYTHON_RECEIVER_SCRIPT)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Python receiver script to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Python Receiver Script", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    2 -> {
                        // Simulator Tab
                        Text(
                            text = "Interactive Virtual PC Simulator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore gestures, mouse sensitivity, and acceleration curves immediately on the built-in virtual desktop monitor in the app.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.switchConnectionMode(ConnectionMode.SIMULATION)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Activate Live Simulator", color = SurfaceDark900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    3 -> {
                        // PC Guide & Architecture Tab
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Wi-Fi Troubleshooting Steps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Install requirements on PC:\n   pip install pynput\n\n2. Run the script on PC:\n   python glidepad_receiver.py\n\n3. Look at your PC terminal:\n   The script prints 'ENTER THIS IP IN GLIDEPAD APP: X.X.X.X'. Enter that exact IP in the Wi-Fi tab.\n\n4. Tap 'Test Ping' to verify instant round-trip response.\n\n5. Windows Firewall:\n   If Windows Defender Firewall shows a popup, click 'Allow Access' for Python on Private Networks.",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NeonCyan)
            }
        },
        containerColor = SurfaceDark800,
        shape = RoundedCornerShape(16.dp)
    )
}
