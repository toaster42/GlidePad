package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.ConnectionStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SurfaceDark700
import com.example.ui.theme.SurfaceDark900
import com.example.ui.theme.TextPrimary

/**
 * Minimal chrome overlay rendered above [TouchpadSurface] when GlidePad is in Float mode.
 *
 * Displays only a corner connection status dot and a >=48dp exit button. The root container
 * attaches no touch handlers so all multitouch gestures pass unimpeded to [TouchpadSurface].
 *
 * @param status Current [ConnectionStatus] determining the indicator dot color and semantics label.
 * @param onExitFloat Callback invoked when the user taps the Float exit button.
 * @param modifier Composable layout modifier.
 */
@Composable
fun FloatModeOverlay(
    status: ConnectionStatus,
    onExitFloat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        ConnectionStatus.CONNECTED, ConnectionStatus.STREAMING -> AccentEmerald
        ConnectionStatus.PAIRING -> AccentAmber
        ConnectionStatus.DISCONNECTED -> AccentRose
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
                .testTag("float_status_dot")
                .semantics { contentDescription = status.label }
        )

        IconButton(
            onClick = onExitFloat,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceDark900.copy(alpha = 0.75f))
                .border(1.dp, SurfaceDark700, CircleShape)
                .testTag("float_exit_button")
        ) {
            Icon(
                imageVector = Icons.Default.CloseFullscreen,
                contentDescription = "Exit float mode",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
