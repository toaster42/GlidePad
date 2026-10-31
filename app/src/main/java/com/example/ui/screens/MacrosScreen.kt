package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.CloudMacro
import com.example.ui.dialogs.CreateMacroDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.MacroItem
import com.example.ui.theme.AccentAmber
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
fun MacrosScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val cloudMacros by viewModel.cloudMacros.collectAsState()

    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0: Wi-Fi PC Power & Launch, 1: Keyboard Shortcuts

    var showUrlDialog by remember { mutableStateOf(false) }
    var showWolDialog by remember { mutableStateOf(false) }
    var showCreateMacroDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("https://") }
    var wolMacInput by remember { mutableStateOf(viewModel.preferencesManager.getSavedMacAddress()) }

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
                    text = "Power & Macro Deck",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Launch apps, control power, send clipboard & trigger shortcuts.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Switcher (Wi-Fi Power vs Shortcuts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark800)
                .border(1.dp, SurfaceDark700, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Wi-Fi PC Tools", "Desktop Shortcuts").forEachIndexed { idx, title ->
                val isSelected = selectedCategoryIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonCyan else Color.Transparent)
                        .clickable { selectedCategoryIndex = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SurfaceDark900 else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedCategoryIndex == 0) {
            // Wi-Fi PC Power & Tools Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Remote App Launchers
                Text(
                    text = "Remote PC Launchers (Wi-Fi)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PowerActionCard(
                        title = "Terminal / CMD",
                        subtitle = "Open command line",
                        icon = Icons.Default.Terminal,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.sendSystemCmdToPc("terminal", "Launched Terminal on PC") }
                    )
                    PowerActionCard(
                        title = "File Explorer",
                        subtitle = "Open files / Finder",
                        icon = Icons.Default.FolderOpen,
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.sendSystemCmdToPc("explorer", "Opened File Explorer") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PowerActionCard(
                        title = "Task Manager",
                        subtitle = "System monitor",
                        icon = Icons.Default.ViewCarousel,
                        color = AccentPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.sendSystemCmdToPc("taskmgr", "Opened Task Manager") }
                    )
                    PowerActionCard(
                        title = "Open Web URL",
                        subtitle = "Launch link on PC",
                        icon = Icons.Default.Language,
                        color = AccentEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { showUrlDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Clipboard & System Power
                Text(
                    text = "Clipboard & Power Control",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Send Phone Clipboard to PC
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipMgr.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    viewModel.sendClipboardTextToPc(text)
                                    Toast.makeText(context, "Sent phone clipboard to PC (${text.take(20)}...)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Phone clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Phone clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Push Clipboard to PC", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Instant paste phone copied text onto PC", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lock PC
                    PowerActionCard(
                        title = "Lock PC",
                        subtitle = "Lock desktop",
                        icon = Icons.Default.Lock,
                        color = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.sendSystemCmdToPc("lock", "Locked PC") }
                    )

                    // Wake-on-LAN
                    PowerActionCard(
                        title = "Wake-on-LAN",
                        subtitle = "Power on PC over Wi-Fi",
                        icon = Icons.Default.PowerSettingsNew,
                        color = AccentEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { showWolDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        } else {
            // Desktop Shortcuts & Custom Cloud Decks
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Macro Decks",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                        Text(
                            text = if (currentUser != null) "Cloud Synced (${cloudMacros.size} custom)" else "Local Shortcuts",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            if (currentUser == null) {
                                Toast.makeText(context, "Sign in with Google in Settings to sync custom macros", Toast.LENGTH_SHORT).show()
                            } else {
                                showCreateMacroDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("add_custom_macro_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SurfaceDark900, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Macro", color = SurfaceDark900, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Custom Cloud Synced Macros
                    items(cloudMacros) { macro ->
                        CloudMacroCard(
                            macro = macro,
                            onClick = { viewModel.executeCloudMacro(macro) },
                            onDelete = { viewModel.deleteCloudMacro(macro.id) { } }
                        )
                    }

                    // Default System Shortcuts
                    items(viewModel.defaultMacros) { macro ->
                        MacroCard(
                            macro = macro,
                            onClick = { viewModel.executeMacro(macro) }
                        )
                    }
                }
            }
        }
    }

    // Modal: Open URL on PC
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Open Webpage on PC", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Type or select any URL to launch immediately on your PC's default browser:", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SurfaceDark700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("YouTube" to "https://youtube.com", "GitHub" to "https://github.com", "Google" to "https://google.com").forEach { (name, url) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceDark700)
                                    .clickable { customUrlInput = url }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, fontSize = 10.sp, color = NeonCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            viewModel.openUrlOnPc(customUrlInput)
                            showUrlDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Open on PC", color = SurfaceDark900, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark800,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal: Wake-on-LAN
    if (showWolDialog) {
        AlertDialog(
            onDismissRequest = { showWolDialog = false },
            title = { Text("Wake-on-LAN (Power On PC)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Broadcasts a standard Magic Packet to power on your PC over Wi-Fi/Ethernet. Enter your PC's network MAC address:", fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wolMacInput,
                        onValueChange = { wolMacInput = it },
                        placeholder = { Text("e.g. 00:1A:2B:3C:4D:5E", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = SurfaceDark700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wolMacInput.isNotBlank()) {
                            viewModel.sendWakeOnLan(wolMacInput)
                            showWolDialog = false
                            Toast.makeText(context, "Sent Wake-on-LAN packet!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Text("Send Magic Packet", color = SurfaceDark900, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWolDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark800,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCreateMacroDialog) {
        CreateMacroDialog(
            viewModel = viewModel,
            onDismiss = { showCreateMacroDialog = false }
        )
    }
}

@Composable
fun PowerActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun MacroCard(
    macro: MacroItem,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (macro.id) {
        "copy" -> Icons.Default.ContentCopy
        "paste" -> Icons.Default.ContentPaste
        "cut" -> Icons.Default.ContentCut
        "undo" -> Icons.Default.Undo
        "redo" -> Icons.Default.Redo
        "save" -> Icons.Default.Save
        "select_all" -> Icons.Default.SelectAll
        "desktop" -> Icons.Default.DesktopWindows
        "task_mgr" -> Icons.Default.ViewCarousel
        "screenshot" -> Icons.Default.Screenshot
        "lock_pc" -> Icons.Default.Lock
        else -> Icons.Default.Bolt
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("macro_${macro.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = macro.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = macro.description,
                    fontSize = 11.sp,
                    color = ElectricBlue
                )
            }
        }
    }
}

@Composable
fun CloudMacroCard(
    macro: CloudMacro,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("cloud_macro_${macro.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (macro.actionType) {
                        "URL" -> Icons.Default.Language
                        "SYSTEM_CMD" -> Icons.Default.Terminal
                        else -> Icons.Default.Bolt
                    },
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (macro.description.isNotBlank()) macro.description else macro.actionType,
                    fontSize = 10.sp,
                    color = ElectricBlue
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
