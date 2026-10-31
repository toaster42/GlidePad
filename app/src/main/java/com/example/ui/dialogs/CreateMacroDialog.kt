package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.model.CloudMacro
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateMacroDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("KEYBOARD") }
    var shortcutText by remember { mutableStateOf("Ctrl, Shift, Esc") }
    var targetPayload by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val actionTypes = listOf(
        "KEYBOARD" to "Shortcut",
        "URL" to "Open URL",
        "SYSTEM_CMD" to "Command"
    )

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "New Cloud Macro",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Action Type", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actionTypes.forEach { (typeId, typeLabel) ->
                        val isSelected = actionType == typeId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else SurfaceDark700)
                                .clickable { actionType = typeId }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SurfaceDark900 else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Button Label", fontSize = 12.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 40) label = it },
                    placeholder = { Text("e.g. Build Project", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceDark700
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("macro_label_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Description / Subtitle", fontSize = 12.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 80) description = it },
                    placeholder = { Text("e.g. Ctrl + Shift + B", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceDark700
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("macro_desc_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                when (actionType) {
                    "KEYBOARD" -> {
                        Text("Keys (Comma-separated)", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = shortcutText,
                            onValueChange = { shortcutText = it },
                            placeholder = { Text("e.g. Ctrl, Alt, Delete", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceDark700
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("macro_keys_input")
                        )
                    }
                    "URL" -> {
                        Text("Web URL", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetPayload,
                            onValueChange = { targetPayload = it },
                            placeholder = { Text("https://mycompany.atlassian.net", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceDark700
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("macro_url_input")
                        )
                    }
                    "SYSTEM_CMD" -> {
                        Text("System Command (terminal, calc, lock)", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetPayload,
                            onValueChange = { targetPayload = it },
                            placeholder = { Text("e.g. terminal or calc", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceDark700
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("macro_cmd_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        Toast.makeText(context, "Please enter a macro label", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val keys = if (actionType == "KEYBOARD") {
                        shortcutText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    } else emptyList()

                    val macro = CloudMacro(
                        label = label.trim(),
                        description = description.trim(),
                        shortcutKeys = keys,
                        actionType = actionType,
                        targetPayload = targetPayload.trim()
                    )

                    isSaving = true
                    viewModel.saveCloudMacro(macro) { success, error ->
                        isSaving = false
                        if (success) {
                            Toast.makeText(context, "Macro saved to cloud!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Error: ${error ?: "Save failed"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("confirm_create_macro_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = SurfaceDark800, modifier = Modifier.height(16.dp))
                } else {
                    Text("Add to Cloud Deck", color = SurfaceDark800, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark800,
        shape = RoundedCornerShape(16.dp)
    )
}
