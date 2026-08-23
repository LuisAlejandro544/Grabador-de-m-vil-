package com.example.ui.editor

import android.graphics.Bitmap
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.editor.VideoEditorManager
import com.example.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Modal Completo de Edición Rápida estilo CapCut / Premiere Rush para dispositivos móviles.
 * Integra:
 * 1. Reproductor en tiempo real con scrubber y marcadores de recorte interactivos.
 * 2. Línea de tiempo visual (Filmstrip) con doble cabezal de entrada (In) y salida (Out).
 * 3. Recorte ultra-rápido sin renderizar (Lossless Stream Copy).
 * 4. Extractor de Miniaturas HD con captura en fotograma exacto.
 */
@Composable
fun VideoEditorDialog(
    video: RecordedVideo,
    onDismiss: () -> Unit,
    onVideoEdited: (File) -> Unit,
    onThumbnailExtracted: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorManager = remember { VideoEditorManager(context) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingTitle by remember { mutableStateOf("Procesando...") }
    var processingProgress by remember { mutableFloatStateOf(0.0f) }

    val durationMs = remember(video) {
        if (video.durationMs > 0) video.durationMs else 10000L
    }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(durationMs) }
    var currentPlaybackMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    var filmstripBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var capturedThumbPreview by remember { mutableStateOf<Bitmap?>(null) }

    // Cargar tira de miniaturas de la línea de tiempo
    LaunchedEffect(video.filePath) {
        withContext(Dispatchers.IO) {
            filmstripBitmaps = editorManager.generateTimelineFilmstrip(video.filePath, count = 12)
        }
    }

    // Bucle para actualizar la posición de reproducción
    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            videoViewInstance?.let { vv ->
                if (vv.isPlaying) {
                    val pos = vv.currentPosition.toLong()
                    currentPlaybackMs = pos
                    if (pos >= endMs) {
                        vv.seekTo(startMs.toInt())
                        currentPlaybackMs = startMs
                    }
                }
            }
            delay(50)
        }
    }

    fun formatMs(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        val millis = (ms % 1000) / 100
        return String.format("%02d:%02d.%01d", minutes, seconds, millis)
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F12))
                .testTag("video_editor_dialog"),
            color = Color(0xFF0F0F12)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Barra Superior estilo CapCut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .testTag("editor_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Vortex Clip Editor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${video.width}x${video.height} • ${video.formattedDuration()}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9EA4)
                                )
                            }
                        }

                        // Botón de Exportar Recorte Rápido
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    processingTitle = "Recortando clip al instante..."
                                    processingProgress = 0.1f
                                    val result = editorManager.trimVideoFast(
                                        sourcePath = video.filePath,
                                        startMs = startMs,
                                        endMs = endMs,
                                        onProgress = { p -> processingProgress = p }
                                    )
                                    isProcessing = false
                                    if (result != null) {
                                        onVideoEdited(result)
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("editor_export_trim_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar Clip",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Visor Central de Video (Player Monitor)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(video.filePath)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            seekTo(startMs.toInt())
                                        }
                                        setOnCompletionListener {
                                            isPlaying = false
                                            seekTo(startMs.toInt())
                                        }
                                        videoViewInstance = this
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        videoViewInstance?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    }
                            )

                            // Botón Central flotante Play/Pause
                            if (!isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable {
                                            videoViewInstance?.let { vv ->
                                                vv.start()
                                                isPlaying = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            // Medidor de tiempo flotante en esquina superior
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMs(currentPlaybackMs),
                                        color = Color(0xFF00E676),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = " / ${formatMs(endMs - startMs)}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // 3. Controles de Reproducción y Recorte Fino
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Retroceder 1s
                        IconButton(
                            onClick = {
                                val target = (currentPlaybackMs - 1000L).coerceAtLeast(startMs)
                                currentPlaybackMs = target
                                videoViewInstance?.seekTo(target.toInt())
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E24))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "-1s",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(
                            onClick = {
                                videoViewInstance?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2979FF))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Avanzar 1s
                        IconButton(
                            onClick = {
                                val target = (currentPlaybackMs + 1000L).coerceAtMost(endMs)
                                currentPlaybackMs = target
                                videoViewInstance?.seekTo(target.toInt())
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E24))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "+1s",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Botón de Extraer Miniatura HD
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    processingTitle = "Extrayendo miniatura en HD..."
                                    processingProgress = 0.5f
                                    val thumb = editorManager.extractThumbnailHD(
                                        sourcePath = video.filePath,
                                        timeMs = currentPlaybackMs,
                                        highQuality = true
                                    )
                                    isProcessing = false
                                    if (thumb != null) {
                                        onThumbnailExtracted(thumb)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E24),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("editor_extract_thumb_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFD54F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Extraer Miniatura HD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 4. Línea de Tiempo Visual (Timeline Filmstrip & Dual Slider)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Línea de Tiempo (Recorte sin renderizado)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Duración: ${formatMs(endMs - startMs)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filmstrip con cabezales de recorte interactivos
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                // Tira de miniaturas de fondo
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (filmstripBitmaps.isNotEmpty()) {
                                        filmstripBitmaps.forEach { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF22222B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Cargando fotogramas...",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Sombra fuera del rango recortado (Zona izquierda no seleccionada)
                                val leftRatio = (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                val rightRatio = ((durationMs - endMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (leftRatio > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(leftRatio.coerceAtLeast(0.001f))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                        )
                                    }

                                    // Zona activa seleccionada con marco verde Neón
                                    val activeRatio = ((endMs - startMs).toFloat() / durationMs.toFloat()).coerceIn(0.01f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(activeRatio)
                                            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                    )

                                    if (rightRatio > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(rightRatio.coerceAtLeast(0.001f))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                        )
                                    }
                                }

                                // Indicador de aguja de reproducción actual (Playhead Cursor)
                                val playheadRatio = (currentPlaybackMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(playheadRatio)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selector de rango deslizable dual
                            var sliderRange by remember(durationMs) {
                                mutableStateOf(0f..durationMs.toFloat())
                            }

                            RangeSlider(
                                value = sliderRange,
                                onValueChange = { range ->
                                    sliderRange = range
                                    startMs = range.start.roundToLong().coerceIn(0L, durationMs - 500L)
                                    endMs = range.endInclusive.roundToLong().coerceIn(startMs + 500L, durationMs)
                                    currentPlaybackMs = startMs
                                    videoViewInstance?.seekTo(startMs.toInt())
                                },
                                valueRange = 0f..durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E676),
                                    activeTrackColor = Color(0xFF00E676),
                                    inactiveTrackColor = Color(0xFF2E2E38)
                                ),
                                modifier = Modifier.testTag("editor_range_slider")
                            )

                            // Marcadores de tiempo inferior
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "In: ${formatMs(startMs)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E676),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Out: ${formatMs(endMs)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF5252),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 5. Herramientas Rápidas de Edición estilo CapCut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditorToolButton(
                            icon = Icons.Default.ContentCut,
                            label = "Recortar In/Out",
                            onClick = {
                                // Resetear a la selección
                                currentPlaybackMs = startMs
                                videoViewInstance?.seekTo(startMs.toInt())
                            }
                        )
                        EditorToolButton(
                            icon = Icons.Default.CameraAlt,
                            label = "Foto Frame HD",
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    processingTitle = "Extrayendo miniatura HD..."
                                    val thumb = editorManager.extractThumbnailHD(
                                        sourcePath = video.filePath,
                                        timeMs = currentPlaybackMs,
                                        highQuality = true
                                    )
                                    isProcessing = false
                                    if (thumb != null) onThumbnailExtracted(thumb)
                                }
                            }
                        )
                        EditorToolButton(
                            icon = Icons.Default.Speed,
                            label = "Stream Copy",
                            badge = "Sin Re-encode",
                            onClick = {
                                // Informativo
                            }
                        )
                    }
                }

                // Overlay de Procesamiento / Progreso
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00E676))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = processingTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Operación ultra-rápida sin pérdida de calidad",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E24)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray
        )
        if (badge != null) {
            Text(
                text = badge,
                fontSize = 9.sp,
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
