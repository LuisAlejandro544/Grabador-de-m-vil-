package com.example.ui.editor

import android.graphics.Bitmap
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.editor.AspectRatioFitMode
import com.example.editor.AspectRatioOption

/**
 * Visor Central de Video (Player Monitor Adaptativo) con fondo Blur, controles de reproducción y badge de tiempo.
 */
@Composable
fun VideoEditorPreviewPlayer(
    videoPath: String,
    startMs: Long,
    endMs: Long,
    currentPlaybackMs: Long,
    isPlaying: Boolean,
    selectedAspectRatio: AspectRatioOption,
    selectedFitMode: AspectRatioFitMode,
    filmstripBitmaps: List<Bitmap>,
    onVideoViewReady: (VideoView) -> Unit,
    onTogglePlayPause: () -> Unit,
    formatMs: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Fondo con efecto Blur si está activo
            if (selectedAspectRatio != AspectRatioOption.ORIGINAL &&
                selectedFitMode == AspectRatioFitMode.BLUR_BACKGROUND &&
                filmstripBitmaps.isNotEmpty()
            ) {
                Image(
                    bitmap = filmstripBitmaps.first().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(25.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }

            // Contenedor con aspect ratio reactivo simulado
            val aspectModifier = when (selectedAspectRatio) {
                AspectRatioOption.TIKTOK_9_16 -> Modifier.aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                AspectRatioOption.YOUTUBE_16_9 -> Modifier.aspectRatio(16f / 9f)
                AspectRatioOption.SQUARE_1_1 -> Modifier.aspectRatio(1f)
                AspectRatioOption.PORTRAIT_4_5 -> Modifier.aspectRatio(4f / 5f, matchHeightConstraintsFirst = true)
                AspectRatioOption.CLASSIC_4_3 -> Modifier.aspectRatio(4f / 3f)
                AspectRatioOption.ORIGINAL -> Modifier.fillMaxSize()
            }

            Box(
                modifier = aspectModifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) 1.dp else 0.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(videoPath)
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                seekTo(startMs.toInt())
                            }
                            setOnCompletionListener {
                                seekTo(startMs.toInt())
                            }
                            onVideoViewReady(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onTogglePlayPause() }
                )
            }

            // Botón Central flotante Play/Pause
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Medidor de tiempo flotante en esquina superior
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMs(currentPlaybackMs),
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " / ${formatMs(endMs - startMs)}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
