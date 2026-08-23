package com.example.ui.editor

import android.graphics.Bitmap
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
 * Modal Modular de Edición Avanzada estilo CapCut para dispositivos móviles.
 * Orquesta componentes independientes:
 * - [VideoEditorHeader]: Barra superior y exportación.
 * - [AspectRatioSelectorRow]: Selección 1-Tap de relación de aspecto y modo de ajuste.
 * - [VideoEditorPreviewPlayer]: Monitor de video con desenfoque de fondo.
 * - [VideoEditorPlaybackControls]: Controles de transporte, división y captura HD.
 * - [VideoEditorFilmstripScrubber]: Línea de tiempo visual interactiva.
 * - [VideoEditorSplitConfirmDialog] & [VideoEditorProcessingOverlay]: Modales auxiliares.
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

    var selectedAspectRatio by remember { mutableStateOf(AspectRatioOption.ORIGINAL) }
    var selectedFitMode by remember { mutableStateOf(AspectRatioFitMode.BLUR_BACKGROUND) }
    var showSplitConfirmDialog by remember { mutableStateOf(false) }

    var filmstripBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    var sliderRange by remember(durationMs) {
        mutableStateOf(0f..durationMs.toFloat())
    }

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

    fun togglePlayPause() {
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
                    // 1. Barra Superior
                    VideoEditorHeader(
                        video = video,
                        selectedAspectRatio = selectedAspectRatio,
                        selectedFitMode = selectedFitMode,
                        isProcessing = isProcessing,
                        onDismiss = onDismiss,
                        onExportClick = {
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
                        }
                    )

                    // 2. Selector de Aspect Ratio
                    AspectRatioSelectorRow(
                        selectedAspectRatio = selectedAspectRatio,
                        selectedFitMode = selectedFitMode,
                        onSelectAspectRatio = { selectedAspectRatio = it },
                        onSelectFitMode = { selectedFitMode = it }
                    )

                    // 3. Monitor Central de Reproducción
                    VideoEditorPreviewPlayer(
                        videoPath = video.filePath,
                        startMs = startMs,
                        endMs = endMs,
                        currentPlaybackMs = currentPlaybackMs,
                        isPlaying = isPlaying,
                        selectedAspectRatio = selectedAspectRatio,
                        selectedFitMode = selectedFitMode,
                        filmstripBitmaps = filmstripBitmaps,
                        onVideoViewReady = { videoViewInstance = it },
                        onTogglePlayPause = { togglePlayPause() },
                        formatMs = { formatMs(it) },
                        modifier = Modifier.weight(1f)
                    )

                    // 4. Barra de Controles y Herramientas Rápidas
                    VideoEditorPlaybackControls(
                        isPlaying = isPlaying,
                        onRewind1s = {
                            val target = (currentPlaybackMs - 1000L).coerceAtLeast(startMs)
                            currentPlaybackMs = target
                            videoViewInstance?.seekTo(target.toInt())
                        },
                        onTogglePlayPause = { togglePlayPause() },
                        onForward1s = {
                            val target = (currentPlaybackMs + 1000L).coerceAtMost(endMs)
                            currentPlaybackMs = target
                            videoViewInstance?.seekTo(target.toInt())
                        },
                        onSplitClick = {
                            if (currentPlaybackMs > 500L && currentPlaybackMs < durationMs - 500L) {
                                showSplitConfirmDialog = true
                            }
                        },
                        onExtractThumbnailClick = {
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
                        }
                    )

                    // 5. Línea de Tiempo Scrubber
                    VideoEditorFilmstripScrubber(
                        startMs = startMs,
                        endMs = endMs,
                        currentPlaybackMs = currentPlaybackMs,
                        durationMs = durationMs,
                        filmstripBitmaps = filmstripBitmaps,
                        sliderRange = sliderRange,
                        onSliderRangeChange = { range ->
                            sliderRange = range
                            startMs = range.start.roundToLong().coerceIn(0L, durationMs - 500L)
                            endMs = range.endInclusive.roundToLong().coerceIn(startMs + 500L, durationMs)
                            currentPlaybackMs = startMs
                            videoViewInstance?.seekTo(startMs.toInt())
                        },
                        formatMs = { formatMs(it) }
                    )
                }

                // Diálogo de Confirmación para Dividir Video
                if (showSplitConfirmDialog) {
                    VideoEditorSplitConfirmDialog(
                        currentPlaybackMs = currentPlaybackMs,
                        durationMs = durationMs,
                        formatMs = { formatMs(it) },
                        onDismiss = { showSplitConfirmDialog = false },
                        onConfirmSplit = {
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
                        }
                    )
                }

                // Overlay de Procesamiento
                if (isProcessing) {
                    VideoEditorProcessingOverlay(
                        title = processingTitle,
                        subtitle = processingSubtitle
                    )
                }
            }
        }
    }
}
