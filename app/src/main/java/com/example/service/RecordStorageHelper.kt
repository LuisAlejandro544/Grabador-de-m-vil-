package com.example.service

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor modular de almacenamiento y escaneo en MediaStore para grabaciones de video.
 */
object RecordStorageHelper {
    private const val TAG = "RecordStorageHelper"

    /**
     * Prepara el archivo de salida para la grabación de pantalla.
     * Prioriza la carpeta pública Movies/ScreenRecorder y cuenta con fallback seguro a cache/files privados.
     */
    fun prepareOutputFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "OBS_REC_$timeStamp.mp4"

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val recordingDir = File(moviesDir, "ScreenRecorder")

        val targetDir = if (recordingDir.exists() || recordingDir.mkdirs()) {
            recordingDir
        } else {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "ScreenRecorder")
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            fallbackDir
        }

        val outputFile = File(targetDir, fileName)
        Log.d(TAG, "Archivo de grabación preparado en: ${outputFile.absolutePath}")
        return outputFile
    }

    /**
     * Escanea el archivo MP4 generado para que aparezca de inmediato en la Galería del sistema y en la app.
     */
    fun scanFileToMediaStore(context: Context, file: File, onScanCompleted: ((String?, String?) -> Unit)? = null) {
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.d(TAG, "Video escaneado e indexado con éxito en MediaStore: $path -> $uri")
                onScanCompleted?.invoke(path, uri?.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando archivo en MediaScanner: ${e.message}", e)
        }
    }
}
