package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingStatus
import com.example.ui.theme.RecordActiveRed
import java.util.Locale

/**
 * Barra superior modular de la pantalla principal con indicador de estado en tiempo real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTopBar(
    status: RecordingStatus,
    elapsedSeconds: Int,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val channel = com.example.model.ReleaseChannel.getCurrentChannel()
                VortexAppLogo(
                    size = 38.dp,
                    channel = channel,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vortex Studio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = channel.getBadgeColor().copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, channel.getBadgeColor().copy(alpha = 0.5f)),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = channel.tag,
                                color = channel.getBadgeColor(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    val statusText = when (status) {
                        RecordingStatus.RECORDING -> "Grabando (${formatSeconds(elapsedSeconds)})"
                        RecordingStatus.PAUSED -> "En pausa"
                        RecordingStatus.COUNTDOWN -> "Iniciando..."
                        RecordingStatus.SAVING -> "Guardando video..."
                        else -> "Listo para capturar"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (status == RecordingStatus.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(RecordActiveRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = if (status == RecordingStatus.RECORDING) RecordActiveRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_videos_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private fun formatSeconds(totalSec: Int): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
