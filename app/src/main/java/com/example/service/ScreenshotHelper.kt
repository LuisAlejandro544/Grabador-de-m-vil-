package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Asistente modular para captura de instantáneas (Screenshots) durante la grabación o uso de la app.
 */
object ScreenshotHelper {
    private const val TAG = "ScreenshotHelper"

    /**
     * Guarda un mapa de bits en el almacenamiento de imágenes (Pictures/Screenshots) y lo indexa en MediaStore.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "OBS_SHOT_$timeStamp.png"

            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val screenshotsDir = File(picturesDir, "Screenshots")

            val targetDir = if (screenshotsDir.exists() || screenshotsDir.mkdirs()) {
                screenshotsDir
            } else {
                val fallback = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: File(context.filesDir, "Screenshots")
                if (!fallback.exists()) fallback.mkdirs()
                fallback
            }

            val imageFile = File(targetDir, fileName)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            RecordStorageHelper.scanFileToMediaStore(context, imageFile)
            Log.i(TAG, "Captura de pantalla guardada exitosamente: ${imageFile.absolutePath}")

            try {
                onSuccess(imageFile)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "📸 Captura guardada en Imágenes", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                onSuccess(imageFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando captura de pantalla: ${e.message}", e)
            try {
                onError(e.message ?: "Error desconocido")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error al guardar captura: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e2: Exception) {
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Extrae un fotograma de alta calidad desde el video actualmente en grabación o grabado.
     */
    fun captureFrameFromVideo(
        context: Context,
        videoFile: File,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (!videoFile.exists() || videoFile.length() == 0L) {
                onError("El archivo de video aún no está disponible para capturar")
                return
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val frame = retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()

            if (frame != null) {
                saveBitmapToGallery(context, frame, onSuccess, onError)
            } else {
                onError("No se pudo obtener el frame del video")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo frame del video: ${e.message}", e)
            onError(e.message ?: "Fallo al procesar frame")
        }
    }
}
