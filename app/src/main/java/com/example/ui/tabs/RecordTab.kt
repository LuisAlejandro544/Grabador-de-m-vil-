package com.example.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordedVideo
import com.example.model.RecordingConfig
import com.example.model.RecordingStatus
import com.example.ui.components.RecordControlCard
import com.example.ui.components.VideoItemCard

/**
 * Pestaña modular principal de Grabación con controles de captura, guía rápida y grabaciones recientes.
 */
@Composable
fun RecordTab(
    config: RecordingConfig,
    status: RecordingStatus,
    elapsedSeconds: Int,
    countdownNumber: Int,
    videos: List<RecordedVideo>,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onViewAllVideos: () -> Unit,
    onPlayVideo: (RecordedVideo) -> Unit,
    onShareVideo: (RecordedVideo) -> Unit,
    onDeleteVideo: (RecordedVideo) -> Unit,
    onRenameVideo: (RecordedVideo, String) -> Unit,
    onEditVideo: (RecordedVideo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tarjeta central de control de grabación
        item {
            RecordControlCard(
                config = config,
                status = status,
                elapsedSeconds = elapsedSeconds,
                countdownNumber = countdownNumber,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onCancelCountdown = onCancelCountdown,
                onToggleGameMode = onToggleGameMode
            )
        }

        // 2. Tarjeta de Consejos rápidos
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consejos para grabar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Puedes pausar y detener la grabación directamente desde la barra de notificaciones mientras juegas.\n• En la pestaña \"Juegos\" puedes iniciar la grabación y abrir tu juego con un solo toque.\n• Las grabaciones se guardan en alta calidad MP4 compatibles con cualquier reproductor.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Encabezado y lista corta de grabaciones recientes
        if (videos.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grabaciones Recientes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onViewAllVideos) {
                        Text("Ver todas (${videos.size})", fontSize = 13.sp)
                    }
                }
            }

            items(videos.take(3), key = { it.id }) { video ->
                VideoItemCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onShare = { onShareVideo(video) },
                    onDelete = { onDeleteVideo(video) },
                    onRename = { v, name -> onRenameVideo(v, name) },
                    onEdit = { onEditVideo(video) }
                )
            }
        }
    }
}
