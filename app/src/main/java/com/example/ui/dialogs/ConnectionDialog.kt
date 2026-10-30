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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
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
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Bluetooth, 1: Wi-Fi, 2: Simulator, 3: PC Guide
    var pcIpInput by remember { mutableStateOf(viewModel.networkManager.pcIpAddress) }
    val pairedDevices = remember { viewModel.bluetoothManager.getBondedDevices() }

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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mode Selector Tabs (Bluetooth, Wi-Fi, Simulator, PC Guide)
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
                            text = "Local Wi-Fi / Hotspot Companion",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ultra-low latency UDP streaming for desktop PCs without Bluetooth hardware.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pcIpInput,
                            onValueChange = {
                                pcIpInput = it
                                viewModel.networkManager.pcIpAddress = it
                            },
                            label = { Text("PC IP Address", fontSize = 11.sp) },
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

                        Spacer(modifier = Modifier.height(8.dp))

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
                        Column(
                            modifier = Modifier
                                .height(200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Does the PC need a client app?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NO for Bluetooth. GlidePad uses standard Bluetooth HID (Human Interface Device). Windows, macOS, and Linux treat your phone as a physical plug-and-play mouse & keyboard with zero installed software.",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "When is a PC client useful?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Only when your PC lacks Bluetooth (custom desktop towers on Ethernet/Wi-Fi). In that case, the optional lightweight Python receiver script (in the Wi-Fi tab) handles UDP packets on port 8089.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Quick Pairing Steps (Windows / Mac):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "1. Open Bluetooth Settings on PC.\n2. Tap 'Add Device' and select your phone.\n3. Confirm the pairing prompt on both.\n4. GlidePad immediately controls your PC cursor, gestures, and keys!",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
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
