package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.GestureAction
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun KeyboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val ctrlHeld by viewModel.ctrlHeld.collectAsState()
    val shiftHeld by viewModel.shiftHeld.collectAsState()
    val altHeld by viewModel.altHeld.collectAsState()
    val winHeld by viewModel.winHeld.collectAsState()

    var textToSend by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Direct Text Input & Send Row
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textToSend,
                    onValueChange = { textToSend = it },
                    placeholder = { Text("Type here to send directly to PC...", fontSize = 12.sp, color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_input_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceDark700,
                        cursorColor = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.sendTextDirect(textToSend)
                        textToSend = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("send_text_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = SurfaceDark900, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Media & Navigation Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark800)
                .border(1.dp, SurfaceDark700, RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.executeGestureAction(GestureAction.MUTE) }) {
                Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.executeGestureAction(GestureAction.VOLUME_DOWN) }) {
                Icon(Icons.Default.VolumeDown, contentDescription = "Vol Down", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.executeGestureAction(GestureAction.VOLUME_UP) }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Vol Up", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.executeGestureAction(GestureAction.PLAY_PAUSE) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { viewModel.sendKeyPress("Esc", 0x29) }) {
                Text("ESC", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { viewModel.sendKeyPress("Del", 0x4C) }) {
                Text("DEL", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Function Keys (Scrollable row)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val fKeys = listOf(
                "F1" to 0x3A.toByte(), "F2" to 0x3B.toByte(), "F3" to 0x3C.toByte(), "F4" to 0x3D.toByte(),
                "F5" to 0x3E.toByte(), "F6" to 0x3F.toByte(), "F7" to 0x40.toByte(), "F8" to 0x41.toByte(),
                "F9" to 0x42.toByte(), "F10" to 0x43.toByte(), "F11" to 0x44.toByte(), "F12" to 0x45.toByte()
            )
            items(fKeys.size) { idx ->
                val (fLabel, fCode) = fKeys[idx]
                KeyButton(
                    label = fLabel,
                    width = 46.dp,
                    height = 36.dp,
                    onClick = { viewModel.sendKeyPress(fLabel, fCode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Number Row
        val numRow = listOf(
            "`" to 0x35.toByte(), "1" to 0x1E.toByte(), "2" to 0x1F.toByte(), "3" to 0x20.toByte(),
            "4" to 0x21.toByte(), "5" to 0x22.toByte(), "6" to 0x23.toByte(), "7" to 0x24.toByte(),
            "8" to 0x25.toByte(), "9" to 0x26.toByte(), "0" to 0x27.toByte(), "-" to 0x2D.toByte(),
            "=" to 0x2E.toByte()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            numRow.forEach { (label, code) ->
                KeyButton(
                    label = label,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    onClick = { viewModel.sendKeyPress(label, code) }
                )
            }
            KeyButton(
                label = "⌫",
                width = 38.dp,
                height = 42.dp,
                onClick = { viewModel.sendKeyPress("Backspace", 0x2A.toByte()) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // QWERTY Row 1
        val qRow = listOf(
            "Tab" to 0x2B.toByte(), "Q" to 0x14.toByte(), "W" to 0x1A.toByte(), "E" to 0x08.toByte(),
            "R" to 0x15.toByte(), "T" to 0x17.toByte(), "Y" to 0x1C.toByte(), "U" to 0x18.toByte(),
            "I" to 0x0C.toByte(), "O" to 0x12.toByte(), "P" to 0x13.toByte(), "[" to 0x2F.toByte(),
            "]" to 0x30.toByte()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            qRow.forEach { (label, code) ->
                KeyButton(
                    label = label,
                    modifier = if (label == "Tab") Modifier.width(42.dp) else Modifier.weight(1f),
                    height = 42.dp,
                    onClick = { viewModel.sendKeyPress(label, code) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ASDF Row 2
        val aRow = listOf(
            "A" to 0x04.toByte(), "S" to 0x16.toByte(), "D" to 0x07.toByte(), "F" to 0x09.toByte(),
            "G" to 0x0A.toByte(), "H" to 0x0B.toByte(), "J" to 0x0D.toByte(), "K" to 0x0E.toByte(),
            "L" to 0x0F.toByte(), ";" to 0x33.toByte(), "'" to 0x34.toByte()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            aRow.forEach { (label, code) ->
                KeyButton(
                    label = label,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    onClick = { viewModel.sendKeyPress(label, code) }
                )
            }
            KeyButton(
                label = "⏎",
                width = 44.dp,
                height = 42.dp,
                onClick = { viewModel.sendKeyPress("Enter", 0x28.toByte()) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ZXCV Row 3
        val zRow = listOf(
            "Z" to 0x1D.toByte(), "X" to 0x1B.toByte(), "C" to 0x06.toByte(), "V" to 0x19.toByte(),
            "B" to 0x05.toByte(), "N" to 0x11.toByte(), "M" to 0x10.toByte(), "," to 0x36.toByte(),
            "." to 0x37.toByte(), "/" to 0x38.toByte()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            KeyButton(
                label = "Shift",
                width = 46.dp,
                height = 42.dp,
                isToggled = shiftHeld,
                onClick = { viewModel.toggleModifier("Shift") }
            )
            zRow.forEach { (label, code) ->
                KeyButton(
                    label = label,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    onClick = { viewModel.sendKeyPress(label, code) }
                )
            }
            KeyButton(
                label = "Shift",
                width = 46.dp,
                height = 42.dp,
                isToggled = shiftHeld,
                onClick = { viewModel.toggleModifier("Shift") }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Modifiers & Space & Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                label = "Ctrl",
                width = 44.dp,
                height = 42.dp,
                isToggled = ctrlHeld,
                onClick = { viewModel.toggleModifier("Ctrl") }
            )
            KeyButton(
                label = "Win",
                width = 40.dp,
                height = 42.dp,
                isToggled = winHeld,
                onClick = { viewModel.toggleModifier("Win") }
            )
            KeyButton(
                label = "Alt",
                width = 40.dp,
                height = 42.dp,
                isToggled = altHeld,
                onClick = { viewModel.toggleModifier("Alt") }
            )
            KeyButton(
                label = "Space",
                modifier = Modifier.weight(1f),
                height = 42.dp,
                onClick = { viewModel.sendKeyPress("Space", 0x2C.toByte()) }
            )
            KeyButton(
                label = "Alt",
                width = 40.dp,
                height = 42.dp,
                isToggled = altHeld,
                onClick = { viewModel.toggleModifier("Alt") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Directional Arrow Cluster
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark800),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyButton(label = "Home", width = 52.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("Home", 0x4A.toByte()) })
                KeyButton(label = "PgUp", width = 52.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("PgUp", 0x4B.toByte()) })
                KeyButton(label = "▲", width = 52.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("Up", 0x52.toByte()) })
                KeyButton(label = "PgDn", width = 52.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("PgDn", 0x4E.toByte()) })
                KeyButton(label = "End", width = 52.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("End", 0x4D.toByte()) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                KeyButton(label = "◀", width = 58.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("Left", 0x50.toByte()) })
                Spacer(modifier = Modifier.width(12.dp))
                KeyButton(label = "▼", width = 58.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("Down", 0x51.toByte()) })
                Spacer(modifier = Modifier.width(12.dp))
                KeyButton(label = "▶", width = 58.dp, height = 38.dp, onClick = { viewModel.sendKeyPress("Right", 0x4F.toByte()) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    isToggled: Boolean = false,
    onClick: () -> Unit
) {
    val finalMod = if (width != null) modifier.width(width) else modifier

    Box(
        modifier = finalMod
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToggled) NeonCyan.copy(alpha = 0.25f) else SurfaceDark800)
            .border(
                1.dp,
                if (isToggled) NeonCyan else SurfaceDark700,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 3) 10.sp else 12.sp,
            fontWeight = if (isToggled) FontWeight.Bold else FontWeight.Medium,
            color = if (isToggled) NeonCyan else TextPrimary
        )
    }
}
