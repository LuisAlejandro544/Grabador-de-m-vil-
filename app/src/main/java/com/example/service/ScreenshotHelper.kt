package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.data.SettingsRepository
import com.example.model.ImageFormatOption
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Asistente modular para captura de instantáneas (Screenshots) durante la grabación o uso de la app.
 * Soporta compresión configurable en PNG (sin pérdida), JPEG (calidad 10-100%) y WebP (alta eficiencia / sin pérdida).
 */
object ScreenshotHelper {
    private const val TAG = "ScreenshotHelper"

    /**
     * Captura una instantánea en tiempo real de alta fidelidad directamente desde [MediaProjection]
     * utilizando un [ImageReader] y una superficie [VirtualDisplay] temporal acelerada por hardware.
     */
    fun captureFromMediaProjection(
        context: Context,
        mediaProjection: MediaProjection,
        width: Int,
        height: Int,
        densityDpi: Int,
        format: ImageFormatOption? = null,
        quality: Int? = null,
        webpLossless: Boolean? = null,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            var virtualDisplay: VirtualDisplay? = null
            val isCaptured = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())

            // Timeout de seguridad de 3 segundos por si el sistema demora en renderizar el primer frame
            val timeoutRunnable = Runnable {
                if (isCaptured.compareAndSet(false, true)) {
                    try {
                        virtualDisplay?.release()
                        imageReader.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error liberando recursos tras timeout de screenshot: ${e.message}")
                    }
                    onError("Tiempo de espera agotado al capturar la pantalla")
                }
            }
            handler.postDelayed(timeoutRunnable, 3000)

            imageReader.setOnImageAvailableListener({ reader ->
                if (!isCaptured.compareAndSet(false, true)) return@setOnImageAvailableListener
                handler.removeCallbacks(timeoutRunnable)

                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val rawBitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        rawBitmap.copyPixelsFromBuffer(buffer)

                        val finalBitmap = if (rowPadding != 0) {
                            val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                            if (cropped != rawBitmap) rawBitmap.recycle()
                            cropped
                        } else {
                            rawBitmap
                        }

                        saveBitmapToGallery(
                            context = context,
                            bitmap = finalBitmap,
                            format = format,
                            quality = quality,
                            webpLossless = webpLossless,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    } else {
                        onError("No se recibió ningún frame del renderizador gráfico")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar buffer de ImageReader: ${e.message}", e)
                    onError(e.message ?: "Fallo al procesar imagen de captura")
                } finally {
                    try {
                        image?.close()
                        virtualDisplay?.release()
                        reader.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error al cerrar lector de imágenes: ${e.message}")
                    }
                }
            }, handler)

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "OBS_ScreenshotDisplay",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar captura con ImageReader: ${e.message}", e)
            onError(e.message ?: "Fallo al inicializar captura de pantalla")
        }
    }

    /**
     * Guarda un mapa de bits en el almacenamiento de imágenes (Pictures/Screenshots) con el formato y
     * compresión especificados, e indexa el archivo en MediaStore.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: ImageFormatOption? = null,
        quality: Int? = null,
        webpLossless: Boolean? = null,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val appConfig = SettingsRepository(context).getConfig()
            val effectiveFormat = format ?: appConfig.imageFormat
            val effectiveQuality = (quality ?: appConfig.imageQuality).coerceIn(10, 100)
            val effectiveLossless = webpLossless ?: appConfig.imageWebpLossless

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = effectiveFormat.extension
            val fileName = "OBS_SHOT_${timeStamp}.${extension}"

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
                when (effectiveFormat) {
                    ImageFormatOption.PNG -> {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    ImageFormatOption.JPEG -> {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, effectiveQuality, out)
                    }
                    ImageFormatOption.WEBP -> {
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (effectiveLossless) {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                            } else {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, effectiveQuality, out)
                            }
                        } else {
                            bitmap.compress(Bitmap.CompressFormat.WEBP, if (effectiveLossless) 100 else effectiveQuality, out)
                        }
                    }
                }
            }

            RecordStorageHelper.scanFileToMediaStore(context, imageFile)
            val formatLabel = when (effectiveFormat) {
                ImageFormatOption.PNG -> "PNG (100%)"
                ImageFormatOption.JPEG -> "JPG (${effectiveQuality}%)"
                ImageFormatOption.WEBP -> if (effectiveLossless) "WebP (Sin Pérdida)" else "WebP (${effectiveQuality}%)"
            }
            Log.i(TAG, "Captura guardada exitosamente ($formatLabel): ${imageFile.absolutePath} [${imageFile.length() / 1024} KB]")

            try {
                onSuccess(imageFile)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "📸 Captura guardada en Imágenes ($formatLabel)", Toast.LENGTH_SHORT).show()
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
        format: ImageFormatOption? = null,
        quality: Int? = null,
        webpLossless: Boolean? = null,
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
                saveBitmapToGallery(
                    context = context,
                    bitmap = frame,
                    format = format,
                    quality = quality,
                    webpLossless = webpLossless,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                onError("No se pudo obtener el frame del video")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo frame del video: ${e.message}", e)
            onError(e.message ?: "Fallo al procesar frame")
        }
    }
}
