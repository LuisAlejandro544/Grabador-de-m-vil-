package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecordingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "RecordingsRepository"
    }

    private fun getRecordingsDirectory(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val recordingsDir = File(moviesDir, "Recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }

    suspend fun loadSavedRecordings(): List<RecordedVideo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<RecordedVideo>()
        val dir = getRecordingsDirectory()
        val files = dir.listFiles { file ->
            file.isFile && (file.extension.equals("mp4", ignoreCase = true) || file.extension.equals("mkv", ignoreCase = true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        for (file in files) {
            if (file.length() <= 0) continue
            var durationMs = 0L
            var width = 1080
            var height = 1920

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                width = widthStr?.toIntOrNull() ?: 1080
                height = heightStr?.toIntOrNull() ?: 1920
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract metadata for ${file.name}: ${e.message}")
            }

            list.add(
                RecordedVideo(
                    id = file.absolutePath,
                    filePath = file.absolutePath,
                    title = file.nameWithoutExtension,
                    durationMs = durationMs,
                    fileSizeBytes = file.length(),
                    dateModified = file.lastModified(),
                    width = width,
                    height = height
                )
            )
        }
        list
    }

    suspend fun deleteRecording(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}")
            false
        }
    }

    suspend fun renameRecording(filePath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            val cleanName = newName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(file.parentFile, "$cleanName.mp4")
            if (targetFile.exists() && targetFile.absolutePath != file.absolutePath) {
                return@withContext false
            }
            file.renameTo(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming file: ${e.message}")
            false
        }
    }

    fun createShareIntent(filePath: String): Intent? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating share intent: ${e.message}")
            null
        }
    }
}
