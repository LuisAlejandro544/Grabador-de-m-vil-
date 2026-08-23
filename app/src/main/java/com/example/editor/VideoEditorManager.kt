package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import com.example.editor.engine.VideoStreamCopyEngine
import com.example.editor.engine.VideoThumbnailEngine
import java.io.File

/**
 * Orquestador principal de edición de video avanzado en Vortex Studio.
 * Desacoplado modularmente:
 * - [VideoStreamCopyEngine]: Recorte Lossless, Split 2-partes y Conversión de Aspect Ratio 1-Tap.
 * - [VideoThumbnailEngine]: Extracción de fotogramas HD y generación de Filmstrip para la línea de tiempo.
 */
class VideoEditorManager(context: Context) {

    private val streamCopyEngine = VideoStreamCopyEngine(context)
    private val thumbnailEngine = VideoThumbnailEngine(context)

    /**
     * Recorta un segmento de video instantáneamente sin renderizado ni pérdida de calidad (Stream Copy).
     */
    suspend fun trimVideoFast(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): File? = streamCopyEngine.trimVideoFast(sourcePath, startMs, endMs, onProgress)

    /**
     * Divide el video en 2 partes en el punto de corte (Split Tool) usando Stream-Copy ultra-rápido.
     */
    suspend fun splitVideoFast(
        sourcePath: String,
        splitMs: Long,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit = {}
    ): Pair<File, File>? = streamCopyEngine.splitVideoFast(sourcePath, splitMs, totalDurationMs, onProgress)

    /**
     * Convierte la relación de aspecto del video con 1 toque (ej. 9:16 para TikTok o 1:1 para Feed).
     */
    suspend fun convertAspectRatio(
        sourcePath: String,
        targetRatio: AspectRatioOption,
        fitMode: AspectRatioFitMode = AspectRatioFitMode.BLUR_BACKGROUND,
        onProgress: (Float) -> Unit = {}
    ): File? = streamCopyEngine.convertAspectRatio(sourcePath, targetRatio, fitMode, onProgress)

    /**
     * Extrae un fotograma exacto en alta definición (HD / 1080p / 4K) en formato JPEG/PNG.
     */
    suspend fun extractThumbnailHD(
        sourcePath: String,
        timeMs: Long,
        highQuality: Boolean = true
    ): File? = thumbnailEngine.extractThumbnailHD(sourcePath, timeMs, highQuality)

    /**
     * Genera una tira de miniaturas de vista previa (filmstrip) a intervalos regulares para la línea de tiempo.
     */
    suspend fun generateTimelineFilmstrip(
        sourcePath: String,
        count: Int = 12
    ): List<Bitmap> = thumbnailEngine.generateTimelineFilmstrip(sourcePath, count)
}
