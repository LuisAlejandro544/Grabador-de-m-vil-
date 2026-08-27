package com.example.service.vtuber

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.example.model.VtuberPreset
import java.io.InputStream

/**
 * Cargador de Bitmaps optimizado para avatares PNG personalizados (4 estados)
 * y renderizador de fallback ligero para el modo PNGtuber.
 */
object VtuberPresetDrawables {
    private const val TAG = "VtuberPresetDrawables"

    /**
     * Dibuja un marco / indicador elegante cuando el usuario activa el modo PNGtuber
     * pero aún no ha subido imágenes personalizadas.
     */
    fun drawPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        preset: VtuberPreset = VtuberPreset.CUSTOM,
        state: VtuberState = VtuberState.IDLE,
        bounceOffset: Float = 0f
    ) {
        val cx = width / 2f
        val cy = (height / 2f) + bounceOffset
        val size = minOf(width, height) * 0.88f
        val radius = size * 0.44f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fondo circular translúcido moderno
        paint.color = Color.parseColor("#991E1B4B")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)

        // Borde púrpura neón
        paint.color = Color.parseColor("#C084FC")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.035f
        canvas.drawCircle(cx, cy, radius, paint)

        // Silueta / Icono de avatar minimalista
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E9D5FF")

        // Cabeza
        canvas.drawCircle(cx, cy - radius * 0.22f, radius * 0.32f, paint)

        // Hombros / Base
        val shoulderRect = RectF(cx - radius * 0.65f, cy + radius * 0.15f, cx + radius * 0.65f, cy + radius * 0.85f)
        canvas.drawRoundRect(shoulderRect, 28f, 28f, paint)

        // Indicador de voz / boca sutil si está hablando
        if (state == VtuberState.TALKING || state == VtuberState.BLINKING_TALKING) {
            paint.color = Color.parseColor("#A855F7")
            canvas.drawCircle(cx, cy - radius * 0.12f, radius * 0.10f, paint)
        }
    }

    /**
     * Guarda una imagen seleccionada por el usuario en el almacenamiento interno privado
     * de la aplicación para garantizar acceso permanente y persistencia sin pérdidas de permisos.
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri, slotName: String): String? {
        return try {
            val vtuberDir = java.io.File(context.filesDir, "vtuber").apply { if (!exists()) mkdirs() }
            val destFile = java.io.File(vtuberDir, "vtuber_$slotName.png")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Error guardando imagen VTuber interna para slot $slotName: ${t.message}", t)
            null
        }
    }

    /**
     * Carga un [Bitmap] de forma segura desde una URI o ruta local (SAF / MediaStore / File / Internal Storage).
     * Incluye submuestreo inteligente (max 512x512) para evitar [OutOfMemoryError] en móviles.
     */
    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val file = java.io.File(uriString)
            if (file.exists() && file.isFile) {
                decodeSampledBitmapFromFile(file.absolutePath, 512, 512)
            } else {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    decodeSampledBitmapFromFile(uri.path ?: uriString, 512, 512)
                } else {
                    decodeSampledBitmapFromUri(context, uri, 512, 512)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error cargando imagen VTuber desde $uriString: ${t.message}")
            null
        }
    }

    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeFile(filePath, options)
        } catch (t: Throwable) {
            Log.e(TAG, "Error decodificando bitmap desde archivo $filePath: ${t.message}")
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error decodificando bitmap desde URI $uri: ${t.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return maxOf(1, inSampleSize)
    }
}
