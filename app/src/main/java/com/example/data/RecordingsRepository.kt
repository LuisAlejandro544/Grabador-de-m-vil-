package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio unificado para gestión y consulta de videos grabados.
 * Explora el almacenamiento público (Movies/ScreenRecorder), directorios privados de la app
 * y la base de datos MediaStore del sistema Android para sincronización inmediata sin demoras.
 */
class RecordingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "RecordingsRepository"
    }

    /**
     * Retorna todas las posibles rutas de almacenamiento donde se guardan grabaciones.
     */
    private fun getStorageDirectories(): List<File> {
        val directories = mutableListOf<File>()

        // 1. Directorio público estándar Movies/ScreenRecorder
        try {
            val publicMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            directories.add(File(publicMovies, "ScreenRecorder"))
            directories.add(publicMovies)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a Movies público: ${e.message}")
        }

        // 2. Directorios en almacenamiento externo específico de la app
        try {
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { moviesDir ->
                directories.add(File(moviesDir, "ScreenRecorder"))
                directories.add(File(moviesDir, "Recordings"))
                directories.add(moviesDir)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a getExternalFilesDir: ${e.message}")
        }

        // 3. Directorio interno privado de la app
        try {
            directories.add(File(context.filesDir, "ScreenRecorder"))
            directories.add(File(context.filesDir, "Recordings"))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a filesDir: ${e.message}")
        }

        return directories.filter { it.exists() && it.isDirectory }
    }

    /**
     * Carga y unifica todas las grabaciones existentes ordenadas por fecha más reciente.
     */
    suspend fun loadSavedRecordings(): List<RecordedVideo> = withContext(Dispatchers.IO) {
        val fileMap = LinkedHashMap<String, File>()

        // 1. Escanear carpetas del sistema de archivos directamente (acceso instantáneo)
        val candidateDirs = getStorageDirectories()
        for (dir in candidateDirs) {
            try {
                val files = dir.listFiles { file ->
                    file.isFile && file.length() > 0 &&
                            (file.extension.equals("mp4", ignoreCase = true) || file.extension.equals("mkv", ignoreCase = true))
                } ?: emptyArray()

                for (file in files) {
                    val canonicalPath = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                    if (!fileMap.containsKey(canonicalPath)) {
                        fileMap[canonicalPath] = file
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error listando archivos en ${dir.absolutePath}: ${e.message}")
            }
        }

        // 2. Consultar MediaStore para indexar videos adicionales registrados por el sistema
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
            val selection = "${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%ScreenRecorder%", "%OBS_REC%")

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol)
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.length() > 0) {
                            val canonicalPath = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                            if (!fileMap.containsKey(canonicalPath)) {
                                fileMap[canonicalPath] = file
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Consulta a MediaStore falló o fue omitida: ${e.message}")
        }

        // 3. Extraer metadatos de video
        val resultList = mutableListOf<RecordedVideo>()
        val sortedFiles = fileMap.values.sortedByDescending { it.lastModified() }

        for (file in sortedFiles) {
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
                Log.w(TAG, "No se pudieron extraer metadatos para ${file.name}: ${e.message}")
            }

            resultList.add(
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

        Log.d(TAG, "Cargadas ${resultList.size} grabaciones en total")
        resultList
    }

    /**
     * Elimina una grabación del sistema de archivos y sincroniza MediaStore para que desaparezca de la galería.
     */
    suspend fun deleteRecording(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val deleted = if (file.exists()) file.delete() else false

            // Notificar a MediaStore para remover el registro de la galería del sistema
            try {
                context.contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Video.Media.DATA} = ?",
                    arrayOf(filePath)
                )
            } catch (_: Exception) {}

            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(filePath),
                arrayOf("video/mp4"),
                null
            )

            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar grabación: ${e.message}")
            false
        }
    }

    /**
     * Renombra una grabación en el sistema de archivos y actualiza MediaStore.
     */
    suspend fun renameRecording(filePath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            val cleanName = newName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(file.parentFile, "$cleanName.mp4")
            if (targetFile.exists() && targetFile.absolutePath != file.absolutePath) {
                return@withContext false
            }
            val renamed = file.renameTo(targetFile)
            if (renamed) {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(filePath, targetFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }
            renamed
        } catch (e: Exception) {
            Log.e(TAG, "Error al renombrar archivo: ${e.message}")
            false
        }
    }

    /**
     * Crea un Intent seguro para compartir el archivo de video.
     */
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
            Log.e(TAG, "Error al crear intent de compartir: ${e.message}")
            null
        }
    }
}
