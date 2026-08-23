package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.example.model.RecordedVideo
import com.example.nativecore.NativeFFmpegBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de procesamiento y edición rápida de video móvil estilo CapCut / Premiere Rush.
 * Implementa:
 * 1. Recorte rápido de video (Stream Copy / Lossless Trim) a nivel de contenedor MP4 sin recodificación
 *    con fallback a MediaMuxer nativo de Android si el puente C++ no está activo.
 * 2. Extractor de Miniaturas en HD (Thumbnail Grabber) en resolución nativa y guardado en Picture/Movies.
 */
class VideoEditorManager(private val context: Context) {

    companion object {
        private const val TAG = "VideoEditorManager"
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer
    }

    /**
     * Recorta un segmento de video instantáneamente sin renderizado ni pérdida de calidad (Stream Copy).
     * @param sourcePath Ruta del video original
     * @param startMs Tiempo de inicio en milisegundos
     * @param endMs Tiempo final en milisegundos
     * @param onProgress Callback de progreso (0.0f a 1.0f)
     * @return El archivo resultante [File] o null en caso de error
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

        // 1. Intentar primero con el puente NDK FFmpeg nativo (ultra-rápido)
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
                    Log.i(TAG, "Recorte FFmpeg completado con éxito: ${outputFile.absolutePath}")
                    return@withContext outputFile
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Fallo en NativeFFmpegBridge.trimVideo, procediendo con MediaMuxer: ${e.message}")
            }
        }

        // 2. Fallback por MediaExtractor + MediaMuxer (Acelerado por HW sin transcodificar)
        val successMuxer = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = outputFile.absolutePath,
            startUs = startMs * 1000L,
            endUs = endMs * 1000L,
            onProgress = onProgress
        )

        if (successMuxer && outputFile.exists() && outputFile.length() > 0) {
            scanFile(outputFile)
            Log.i(TAG, "Recorte MediaMuxer Stream-Copy completado: ${outputFile.absolutePath}")
            outputFile
        } else {
            Log.e(TAG, "Error al recortar video")
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    /**
     * Extrae un fotograma exacto en alta definición (HD / 1080p / 4K) en formato JPEG/PNG.
     * @param sourcePath Ruta del video original
     * @param timeMs Tiempo en milisegundos del fotograma
     * @param highQuality Si es true guarda en calidad máxima (100)
     * @return El archivo de imagen generado o null
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
        count: Int = 10
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
            Log.w(TAG, "Error generando filmstrip de línea de tiempo: ${e.message}")
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
        bitmaps
    }

    /**
     * Realiza un recorte directo (Stream Copy) a nivel de paquetes de bits usando MediaExtractor y MediaMuxer.
     * Esencial para evitar re-compresión, ahorrando 100% de CPU y preservando calidad 1:1 original.
     */
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

            // Procesar cada pista
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
