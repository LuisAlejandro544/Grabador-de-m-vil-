package com.example.editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de extracción de fotogramas y generación de miniaturas en HD y filmstrip para la línea de tiempo.
 */
class VideoThumbnailEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoThumbnailEngine"
    }

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
            val frameBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
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
            Log.i(TAG, "Miniatura HD extraída: ${thumbFile.absolutePath}")
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
                    val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
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

    private fun getPicturesOutputDir(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenDir = File(picturesDir, "ScreenRecorder")
        if (!screenDir.exists()) screenDir.mkdirs()
        return if (screenDir.exists()) screenDir else context.filesDir
    }

    private fun scanFile(file: File, mime: String = "image/jpeg") {
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
