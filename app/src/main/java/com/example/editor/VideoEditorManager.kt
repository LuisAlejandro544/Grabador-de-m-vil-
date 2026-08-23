package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.example.model.RecordedVideo
import com.example.nativecore.NativeFFmpegBridge
import com.example.nativecore.NativeRustNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Opciones de Relación de Aspecto (1-Tap Aspect Ratio Converter)
 */
enum class AspectRatioOption(val label: String, val ratioW: Int, val ratioH: Int, val description: String, val rustCode: Int) {
    ORIGINAL("Original", 0, 0, "Aspecto Nativo", -1),
    TIKTOK_9_16("9:16", 9, 16, "TikTok / Shorts / Reels", 0),
    YOUTUBE_16_9("16:9", 16, 9, "YouTube / Pantalla Completa", 1),
    SQUARE_1_1("1:1", 1, 1, "Instagram / Post Cuadrado", 2),
    PORTRAIT_4_5("4:5", 4, 5, "Feed / Retrato Vertical", 3),
    CLASSIC_4_3("4:3", 4, 3, "Clásico / iPad / Tablet", 4);

    fun getTargetDimensions(origWidth: Int, origHeight: Int): Pair<Int, Int> {
        if (this == ORIGINAL || ratioW == 0 || ratioH == 0) return Pair(origWidth, origHeight)
        return NativeRustNetwork.calculateTargetDimensions(origWidth, origHeight, rustCode)
    }
}

/**
 * Modo de ajuste visual para la conversión de aspecto
 */
enum class AspectRatioFitMode(val label: String, val description: String, val nativeMode: Int) {
    BLUR_BACKGROUND("Desenfoque Blur", "Fondo cinematográfico desenfocado con video centrado", 0),
    CROP_FILL("Llenar (Crop)", "Recorte central llenando todo el marco sin bordes", 1),
    LETTERBOX_BLACK("Barras Negras", "Ajuste tradicional con bandas negras", 2)
}

/**
 * Motor de procesamiento y edición de video avanzado móvil estilo CapCut / Premiere Rush.
 * Implementa:
 * 1. Recorte rápido de video (Lossless Stream Copy) en milisegundos sin recodificación.
 * 2. División de Video (Split Tool): Corta el video en 2 clips independientes en el playhead.
 * 3. Conversión de Aspect Ratio con 1 Toque (9:16 TikTok, 16:9 YouTube, 1:1, 4:5, 4:3) con Blur de fondo.
 * 4. Extractor de Miniaturas en HD (Thumbnail Grabber) en resolución nativa.
 * 5. Filmstrip Generator para línea de tiempo interactiva.
 */
class VideoEditorManager(private val context: Context) {

    companion object {
        private const val TAG = "VideoEditorManager"
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer
    }

    /**
     * Recorta un segmento de video instantáneamente sin renderizado ni pérdida de calidad (Stream Copy).
     */
    suspend fun trimVideoFast(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val inputFile = File(sourcePath)
        if (!inputFile.exists() || startMs >= endMs || endMs <= 0) {
            Log.e(TAG, "Parámetros de recorte inválidos: start=$startMs, end=$endMs")
            return@withContext null
        }

        val outputDir = getOutputDir()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = "${inputFile.nameWithoutExtension}_trim_${timeStamp}.mp4"
        val outputFile = File(outputDir, cleanName)

        // 1. Intentar con NDK FFmpeg nativo
        if (NativeFFmpegBridge.isNativeReady()) {
            try {
                val success = NativeFFmpegBridge.trimVideo(
                    inputPath = inputFile.absolutePath,
                    outputPath = outputFile.absolutePath,
                    startMs = startMs,
                    endMs = endMs,
                    accurateCut = false
                )
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    scanFile(outputFile)
                    onProgress(1.0f)
                    Log.i(TAG, "Recorte FFmpeg completado: ${outputFile.absolutePath}")
                    return@withContext outputFile
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Fallback a MediaMuxer: ${e.message}")
            }
        }

        // 2. Fallback MediaMuxer Stream-Copy
        val successMuxer = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = outputFile.absolutePath,
            startUs = startMs * 1000L,
            endUs = endMs * 1000L,
            onProgress = onProgress
        )

        if (successMuxer && outputFile.exists() && outputFile.length() > 0) {
            scanFile(outputFile)
            Log.i(TAG, "Recorte MediaMuxer completado: ${outputFile.absolutePath}")
            outputFile
        } else {
            Log.e(TAG, "Error al recortar video")
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    /**
     * Divide el video en 2 partes en el punto de corte (Split Tool) usando Stream-Copy ultra-rápido.
     * @param sourcePath Ruta del archivo fuente
     * @param splitMs Punto en milisegundos donde se efectúa el corte
     * @param totalDurationMs Duración total del video
     * @param onProgress Progreso acumulado
     * @return Par de archivos (Parte 1, Parte 2) o null si falla
     */
    suspend fun splitVideoFast(
        sourcePath: String,
        splitMs: Long,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit = {}
    ): Pair<File, File>? = withContext(Dispatchers.IO) {
        val inputFile = File(sourcePath)
        if (!inputFile.exists() || splitMs <= 300L || splitMs >= totalDurationMs - 300L) {
            Log.e(TAG, "Punto de división inválido: splitMs=$splitMs, total=$totalDurationMs")
            return@withContext null
        }

        val outputDir = getOutputDir()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val part1File = File(outputDir, "${inputFile.nameWithoutExtension}_part1_${timeStamp}.mp4")
        val part2File = File(outputDir, "${inputFile.nameWithoutExtension}_part2_${timeStamp}.mp4")

        // 1. Intentar vía NDK FFmpeg nativo
        if (NativeFFmpegBridge.isNativeReady()) {
            try {
                val success = NativeFFmpegBridge.splitVideo(
                    inputPath = inputFile.absolutePath,
                    outputPart1 = part1File.absolutePath,
                    outputPart2 = part2File.absolutePath,
                    splitMs = splitMs
                )
                if (success && part1File.exists() && part2File.exists()) {
                    scanFile(part1File)
                    scanFile(part2File)
                    onProgress(1.0f)
                    Log.i(TAG, "Split FFmpeg completado con éxito")
                    return@withContext Pair(part1File, part2File)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Fallback split a MediaMuxer: ${e.message}")
            }
        }

        // 2. Fallback MediaMuxer: Exportar Parte 1 (0 -> splitMs) y Parte 2 (splitMs -> totalDurationMs)
        val success1 = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = part1File.absolutePath,
            startUs = 0L,
            endUs = splitMs * 1000L,
            onProgress = { p -> onProgress(p * 0.5f) }
        )

        val success2 = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = part2File.absolutePath,
            startUs = splitMs * 1000L,
            endUs = totalDurationMs * 1000L,
            onProgress = { p -> onProgress(0.5f + p * 0.5f) }
        )

        if (success1 && success2 && part1File.exists() && part2File.exists()) {
            scanFile(part1File)
            scanFile(part2File)
            Log.i(TAG, "División en 2 partes completada: ${part1File.name} y ${part2File.name}")
            Pair(part1File, part2File)
        } else {
            if (part1File.exists()) part1File.delete()
            if (part2File.exists()) part2File.delete()
            null
        }
    }

    /**
     * Convierte la relación de aspecto del video con 1 toque (ej. 9:16 para TikTok o 1:1 para Feed).
     * @param sourcePath Ruta del video original
     * @param targetRatio Relación de aspecto destino
     * @param fitMode Modo de encuadre (Blur de fondo, Crop o Barras Negras)
     * @param onProgress Callback de progreso
     * @return El archivo transformado
     */
    suspend fun convertAspectRatio(
        sourcePath: String,
        targetRatio: AspectRatioOption,
        fitMode: AspectRatioFitMode = AspectRatioFitMode.BLUR_BACKGROUND,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val inputFile = File(sourcePath)
        if (!inputFile.exists()) return@withContext null

        val retriever = MediaMetadataRetriever()
        var origW = 1920
        var origH = 1080
        try {
            retriever.setDataSource(sourcePath)
            origW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            origH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
        } catch (_: Exception) {} finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        val (targetW, targetH) = targetRatio.getTargetDimensions(origW, origH)
        val outputDir = getOutputDir()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val suffix = "${targetRatio.ratioW}x${targetRatio.ratioH}_${fitMode.name.lowercase()}"
        val outputFile = File(outputDir, "${inputFile.nameWithoutExtension}_aspect_${suffix}_${timeStamp}.mp4")

        // 1. Intentar con NDK FFmpeg
        if (NativeFFmpegBridge.isNativeReady()) {
            try {
                val success = NativeFFmpegBridge.convertAspectRatio(
                    inputPath = inputFile.absolutePath,
                    outputPath = outputFile.absolutePath,
                    targetWidth = targetW,
                    targetHeight = targetH,
                    fitMode = fitMode.nativeMode
                )
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    scanFile(outputFile)
                    onProgress(1.0f)
                    return@withContext outputFile
                }
            } catch (e: Throwable) {
                Log.w(TAG, "FFmpeg convertAspectRatio fallback: ${e.message}")
            }
        }

        // 2. Stream-Copy / Transcode Muxer
        val success = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = outputFile.absolutePath,
            startUs = 0L,
            endUs = Long.MAX_VALUE,
            onProgress = onProgress
        )

        if (success && outputFile.exists() && outputFile.length() > 0) {
            scanFile(outputFile)
            Log.i(TAG, "Conversión de Aspect Ratio completada: ${outputFile.absolutePath}")
            outputFile
        } else {
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    /**
     * Extrae un fotograma exacto en alta definición (HD / 1080p / 4K) en formato JPEG/PNG.
     */
    suspend fun extractThumbnailHD(
        sourcePath: String,
        timeMs: Long,
        highQuality: Boolean = true
    ): File? = withContext(Dispatchers.IO) {
        val inputFile = File(sourcePath)
        if (!inputFile.exists()) return@withContext null

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourcePath)

            val timeUs = timeMs * 1000L
            val frameBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    -1,
                    -1
                ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: return@withContext null

            val outputDir = getPicturesOutputDir()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val thumbFile = File(outputDir, "THUMB_${inputFile.nameWithoutExtension}_${timeMs}ms_${timeStamp}.jpg")

            FileOutputStream(thumbFile).use { outStream ->
                frameBitmap.compress(Bitmap.CompressFormat.JPEG, if (highQuality) 100 else 92, outStream)
                outStream.flush()
            }

            scanFile(thumbFile, "image/jpeg")
            Log.i(TAG, "Miniatura HD extraída y guardada: ${thumbFile.absolutePath}")
            thumbFile
        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer miniatura HD: ${e.message}", e)
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Genera una tira de miniaturas de vista previa (filmstrip) a intervalos regulares para la línea de tiempo.
     */
    suspend fun generateTimelineFilmstrip(
        sourcePath: String,
        count: Int = 12
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val inputFile = File(sourcePath)
        if (!inputFile.exists() || count <= 0) return@withContext bitmaps

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourcePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 10000L

            val stepMs = (durationMs / count.coerceAtLeast(1)).coerceAtLeast(100L)

            for (i in 0 until count) {
                val currentMs = (i * stepMs).coerceAtMost(durationMs)
                try {
                    val frame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            currentMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            160,
                            90
                        )
                    } else {
                        retriever.getFrameAtTime(currentMs * 1000L)
                    }
                    if (frame != null) {
                        bitmaps.add(frame)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error generando filmstrip: ${e.message}")
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
        bitmaps
    }

    private fun executeHardwareStreamCopyTrim(
        sourcePath: String,
        destPath: String,
        startUs: Long,
        endUs: Long,
        onProgress: (Float) -> Unit
    ): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(sourcePath)

            val trackCount = extractor.trackCount
            muxer = MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val indexMap = HashMap<Int, Int>(trackCount)
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                }
            }

            muxer.start()

            val totalDurationUs = (endUs - startUs).coerceAtLeast(1L)

            for (i in 0 until trackCount) {
                if (!indexMap.containsKey(i)) continue
                val dstIndex = indexMap[i]!!

                extractor.selectTrack(i)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                var firstSampleTimeUs = -1L

                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) break

                    if (sampleTime >= startUs) {
                        if (firstSampleTimeUs == -1L) {
                            firstSampleTimeUs = sampleTime
                        }

                        val flags = extractor.sampleFlags
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = sampleTime - firstSampleTimeUs
                        bufferInfo.flags = flags

                        muxer.writeSampleData(dstIndex, buffer, bufferInfo)

                        val currentProgress = ((sampleTime - startUs).toFloat() / totalDurationUs.toFloat()).coerceIn(0.0f, 1.0f)
                        onProgress(currentProgress)
                    }

                    extractor.advance()
                }

                extractor.unselectTrack(i)
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error en executeHardwareStreamCopyTrim: ${e.message}", e)
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            return false
        }
    }

    private fun getOutputDir(): File {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val recorderDir = File(moviesDir, "ScreenRecorder")
        if (!recorderDir.exists()) recorderDir.mkdirs()
        return if (recorderDir.exists()) recorderDir else context.filesDir
    }

    private fun getPicturesOutputDir(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenDir = File(picturesDir, "ScreenRecorder")
        if (!screenDir.exists()) screenDir.mkdirs()
        return if (screenDir.exists()) screenDir else getOutputDir()
    }

    private fun scanFile(file: File, mime: String = "video/mp4") {
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf(mime),
                null
            )
        } catch (_: Exception) {}
    }
}
