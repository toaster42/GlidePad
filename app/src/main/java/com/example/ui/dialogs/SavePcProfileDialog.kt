package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark800
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SavePcProfileDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var profileName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Save Current PC Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Save your current PC IP (${viewModel.networkManager.pcIpAddress}), sensitivity, and acceleration curve to your Google Account.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { if (it.length <= 50) profileName = it },
                    placeholder = { Text("e.g. Home Desktop or Office Rig", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceDark700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileName.isBlank()) {
                        Toast.makeText(context, "Please enter a profile name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.saveCurrentAsPcProfile(profileName.trim()) { success, error ->
                        isSaving = false
                        if (success) {
                            Toast.makeText(context, "Profile saved to cloud!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Error: ${error ?: "Save failed"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("confirm_save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = SurfaceDark800, modifier = Modifier.height(16.dp))
                } else {
                    Text("Save Profile", color = SurfaceDark800, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark800,
        shape = RoundedCornerShape(16.dp)
    )
}
