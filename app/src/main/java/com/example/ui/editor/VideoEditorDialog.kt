package com.example.ui.editor

import android.graphics.Bitmap
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.editor.AspectRatioFitMode
import com.example.editor.AspectRatioOption
import com.example.editor.VideoEditorManager
import com.example.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToLong

/**
 * Modal Completo de Edición Avanzada estilo CapCut para dispositivos móviles.
 * Integra:
 * 1. Conversión de Aspect Ratio con 1 Toque (9:16 TikTok, 16:9 YouTube, 1:1 Feed, 4:5, 4:3) con fondo desenfocado Blur.
 * 2. Herramienta de División (Split Tool): Corta el video en 2 partes instantáneamente en el cabezal de reproducción.
 * 3. Recorte ultra-rápido sin pérdida de calidad (Lossless Stream Copy).
 * 4. Extractor de Miniaturas HD con captura en fotograma exacto.
 * 5. Filmstrip de miniaturas dinámico con scrubbing interactivo.
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
    var processingSubtitle by remember { mutableStateOf("Operación ultra-rápida sin pérdida de calidad") }
    var processingProgress by remember { mutableFloatStateOf(0.0f) }

    val durationMs = remember(video) {
        if (video.durationMs > 0) video.durationMs else 10000L
    }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(durationMs) }
    var currentPlaybackMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Estado de Aspect Ratio 1-Tap
    var selectedAspectRatio by remember { mutableStateOf(AspectRatioOption.ORIGINAL) }
    var selectedFitMode by remember { mutableStateOf(AspectRatioFitMode.BLUR_BACKGROUND) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }

    // Estado de División (Split)
    var showSplitConfirmDialog by remember { mutableStateOf(false) }

    var filmstripBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    // Cargar tira de miniaturas
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
                .background(Color(0xFF0D0D11))
                .testTag("video_editor_dialog"),
            color = Color(0xFF0D0D11)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Barra Superior con botones de acción y exportación
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Vortex Video Studio Pro",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (selectedAspectRatio == AspectRatioOption.ORIGINAL)
                                        "${video.width}x${video.height} • ${video.formattedDuration()}"
                                    else
                                        "Formato: ${selectedAspectRatio.label} • ${selectedFitMode.label}",
                                    fontSize = 11.sp,
                                    color = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color(0xFF00E676) else Color(0xFF9E9EA4)
                                )
                            }
                        }

                        // Botón de Guardar / Exportar Clip
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    if (selectedAspectRatio != AspectRatioOption.ORIGINAL) {
                                        processingTitle = "Adaptando formato a ${selectedAspectRatio.label}..."
                                        processingSubtitle = "Generando video optimizado para redes sociales"
                                        val converted = editorManager.convertAspectRatio(
                                            sourcePath = video.filePath,
                                            targetRatio = selectedAspectRatio,
                                            fitMode = selectedFitMode,
                                            onProgress = { p -> processingProgress = p }
                                        )
                                        isProcessing = false
                                        if (converted != null) onVideoEdited(converted)
                                    } else {
                                        processingTitle = "Guardando clip recortado..."
                                        processingSubtitle = "Stream Copy ultra-rápido sin pérdida de calidad"
                                        val result = editorManager.trimVideoFast(
                                            sourcePath = video.filePath,
                                            startMs = startMs,
                                            endMs = endMs,
                                            onProgress = { p -> processingProgress = p }
                                        )
                                        isProcessing = false
                                        if (result != null) onVideoEdited(result)
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color(0xFF2979FF) else Color(0xFF00E676),
                                contentColor = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("editor_export_trim_button")
                        ) {
                            Icon(
                                imageVector = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Icons.Default.FileDownload else Icons.Default.ContentCut,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) "Exportar ${selectedAspectRatio.label}" else "Guardar Clip",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Barra Rápida de Relación de Aspecto (1-Tap Aspect Ratio Selector)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aspect Ratio:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        AspectRatioOption.values().forEach { option ->
                            val isSelected = selectedAspectRatio == option
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAspectRatio = option
                                },
                                label = {
                                    Text(
                                        text = option.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1B1B22),
                                    selectedContainerColor = Color(0xFF00E676),
                                    selectedLabelColor = Color.Black,
                                    labelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("aspect_ratio_chip_${option.name.lowercase()}")
                            )
                        }
                    }

                    // Si hay un Aspect Ratio no original seleccionado, mostrar selector de modo de ajuste (Blur / Crop / Letterbox)
                    AnimatedVisibility(visible = selectedAspectRatio != AspectRatioOption.ORIGINAL) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AspectRatioFitMode.values().forEach { mode ->
                                val isSelected = selectedFitMode == mode
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedFitMode = mode }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) Color(0xFF2979FF) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    color = if (isSelected) Color(0xFF1E2A4A) else Color(0xFF17171E),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when(mode) {
                                                AspectRatioFitMode.BLUR_BACKGROUND -> Icons.Default.BlurOn
                                                AspectRatioFitMode.CROP_FILL -> Icons.Default.AspectRatio
                                                AspectRatioFitMode.LETTERBOX_BLACK -> Icons.Default.Layers
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (isSelected) Color(0xFF64B5F6) else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = mode.label,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Visor Central de Video (Player Monitor Adaptativo)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
                            if (selectedAspectRatio != AspectRatioOption.ORIGINAL && selectedFitMode == AspectRatioFitMode.BLUR_BACKGROUND && filmstripBitmaps.isNotEmpty()) {
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
                            }

                            // Botón Central flotante Play/Pause
                            if (!isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
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

                    // 4. Barra de Controles de Reproducción y Herramientas Rápidas (Split & Foto)
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
                                .size(42.dp)
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

                        // Botón DIVIDIR AQUÍ (Split Tool)
                        Button(
                            onClick = {
                                if (currentPlaybackMs > 500L && currentPlaybackMs < durationMs - 500L) {
                                    showSplitConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF37474F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("editor_split_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallSplit,
                                contentDescription = "Dividir",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dividir ✂️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botón de Extraer Miniatura HD
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    processingTitle = "Extrayendo miniatura en HD..."
                                    processingSubtitle = "Captura en fotograma exacto ${formatMs(currentPlaybackMs)}"
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
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFFFD54F)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Foto HD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 5. Línea de Tiempo Visual (Filmstrip & Dual Slider)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151C)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Línea de Tiempo (Recorte / Selección)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Duración: ${formatMs(endMs - startMs)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Filmstrip con cabezales de recorte interactivos
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
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
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                // Sombra fuera del rango recortado
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

                                // Indicador de aguja de reproducción actual (Playhead)
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

                            Spacer(modifier = Modifier.height(4.dp))

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
                                    text = "Playhead: ${formatMs(currentPlaybackMs)}",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
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
                }

                // Diálogo de Confirmación para Dividir Video (Split)
                if (showSplitConfirmDialog) {
                    Dialog(onDismissRequest = { showSplitConfirmDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CallSplit,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dividir Video en dos partes",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Se cortará el video exactamente en el punto actual (${formatMs(currentPlaybackMs)}):",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Parte 1: 00:00 - ${formatMs(currentPlaybackMs)}\n• Parte 2: ${formatMs(currentPlaybackMs)} - ${formatMs(durationMs)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00E676),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showSplitConfirmDialog = false }) {
                                        Text("Cancelar", color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            showSplitConfirmDialog = false
                                            scope.launch {
                                                isProcessing = true
                                                processingTitle = "Dividiendo video en 2 partes..."
                                                processingSubtitle = "Generando archivos Parte 1 y Parte 2 sin re-encode"
                                                val splitResult = editorManager.splitVideoFast(
                                                    sourcePath = video.filePath,
                                                    splitMs = currentPlaybackMs,
                                                    totalDurationMs = durationMs,
                                                    onProgress = { p -> processingProgress = p }
                                                )
                                                isProcessing = false
                                                if (splitResult != null) {
                                                    onVideoEdited(splitResult.first)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF9800),
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text("Dividir y Guardar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay de Procesamiento / Progreso
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.82f))
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
                                CircularProgressIndicator(
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = processingTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = processingSubtitle,
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
