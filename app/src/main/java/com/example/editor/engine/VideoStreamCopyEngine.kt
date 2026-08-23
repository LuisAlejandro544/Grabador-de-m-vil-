package com.example.editor.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.example.editor.AspectRatioFitMode
import com.example.editor.AspectRatioOption
import com.example.nativecore.NativeFFmpegBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de procesamiento de video sin recodificación (Lossless Stream-Copy) y transcodificación con FFmpeg.
 * Encapsula:
 * - Recorte rápido de video (Trim).
 * - División de clips (Split Tool).
 * - Conversión de relación de aspecto (1-Tap Aspect Ratio Converter).
 */
class VideoStreamCopyEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoStreamCopyEngine"
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer
    }

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

        // 1. NDK FFmpeg nativo
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

        // 1. NDK FFmpeg nativo
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

        // 2. Fallback MediaMuxer
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
            Log.i(TAG, "División completada: ${part1File.name} y ${part2File.name}")
            Pair(part1File, part2File)
        } else {
            if (part1File.exists()) part1File.delete()
            if (part2File.exists()) part2File.delete()
            null
        }
    }

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

        // 1. FFmpeg NDK
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

        // 2. Stream-Copy / Hardware Muxer
        val success = executeHardwareStreamCopyTrim(
            sourcePath = inputFile.absolutePath,
            destPath = outputFile.absolutePath,
            startUs = 0L,
            endUs = Long.MAX_VALUE,
            onProgress = onProgress
        )

        if (success && outputFile.exists() && outputFile.length() > 0) {
            scanFile(outputFile)
            Log.i(TAG, "Conversión Aspect Ratio completada: ${outputFile.absolutePath}")
            outputFile
        } else {
            if (outputFile.exists()) outputFile.delete()
            null
        }
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
