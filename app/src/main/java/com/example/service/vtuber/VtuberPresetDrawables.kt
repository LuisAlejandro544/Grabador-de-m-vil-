package com.example.service.vtuber

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.example.model.VtuberPreset
import java.io.InputStream

/**
 * Motor de renderizado vectorial de avatares predeterminados (Cyber Cat, Anime Aoi, Pixel Slime)
 * y cargador seguro de Bitmaps para avatares PNG personalizados.
 */
object VtuberPresetDrawables {
    private const val TAG = "VtuberPresetDrawables"

    /**
     * Dibuja el avatar correspondiente sobre el [Canvas] en función del estado reactivo y preset.
     */
    fun drawPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        preset: VtuberPreset,
        state: VtuberState,
        bounceOffset: Float = 0f
    ) {
        val cx = width / 2f
        val cy = (height / 2f) + bounceOffset
        val size = minOf(width, height) * 0.90f

        when (preset) {
            VtuberPreset.CYBER_CAT -> drawCyberCat(canvas, cx, cy, size, state)
            VtuberPreset.ANIME_AOI -> drawAnimeAoi(canvas, cx, cy, size, state)
            VtuberPreset.PIXEL_SLIME -> drawPixelSlime(canvas, cx, cy, size, state)
            VtuberPreset.CUSTOM -> {
                // Si no hay bitmap cargado para custom, fallback a Cyber Cat
                drawCyberCat(canvas, cx, cy, size, state)
            }
        }
    }

    /**
     * Carga un [Bitmap] de forma segura desde una URI local (SAF / MediaStore / File).
     */
    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando imagen VTuber desde $uriString: ${e.message}")
            null
        }
    }

    // ==========================================
    // 1. AVATAR: CYBER CAT (Gato Ciberpunk Neón)
    // ==========================================
    private fun drawCyberCat(canvas: Canvas, cx: Float, cy: Float, size: Float, state: VtuberState) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size * 0.38f

        val isEyesClosed = state == VtuberState.BLINKING || state == VtuberState.BLINKING_TALKING
        val isMouthOpen = state == VtuberState.TALKING || state == VtuberState.BLINKING_TALKING

        // Aura / Resplandor Neón
        paint.color = Color.parseColor("#3300E5FF")
        canvas.drawCircle(cx, cy, radius * 1.15f, paint)

        // Orejas Robóticas
        val earPath = Path()
        // Oreja izquierda
        earPath.moveTo(cx - radius * 0.7f, cy - radius * 0.3f)
        earPath.lineTo(cx - radius * 0.95f, cy - radius * 1.1f)
        earPath.lineTo(cx - radius * 0.2f, cy - radius * 0.8f)
        earPath.close()

        // Oreja derecha
        earPath.moveTo(cx + radius * 0.7f, cy - radius * 0.3f)
        earPath.lineTo(cx + radius * 0.95f, cy - radius * 1.1f)
        earPath.lineTo(cx + radius * 0.2f, cy - radius * 0.8f)
        earPath.close()

        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        canvas.drawPath(earPath, paint)

        // Interior neón de las orejas
        val innerEar = Path()
        innerEar.moveTo(cx - radius * 0.65f, cy - radius * 0.35f)
        innerEar.lineTo(cx - radius * 0.85f, cy - radius * 0.95f)
        innerEar.lineTo(cx - radius * 0.3f, cy - radius * 0.75f)
        innerEar.close()

        innerEar.moveTo(cx + radius * 0.65f, cy - radius * 0.35f)
        innerEar.lineTo(cx + radius * 0.85f, cy - radius * 0.95f)
        innerEar.lineTo(cx + radius * 0.3f, cy - radius * 0.75f)
        innerEar.close()

        paint.color = Color.parseColor("#FF0055")
        canvas.drawPath(innerEar, paint)

        // Cabeza / Casco
        paint.color = Color.parseColor("#0F172A")
        canvas.drawCircle(cx, cy, radius, paint)

        // Borde Neón Cian del Casco
        paint.color = Color.parseColor("#00E5FF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.03f
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL

        // Audífonos Gamer laterales
        paint.color = Color.parseColor("#FF007F")
        canvas.drawRoundRect(
            RectF(cx - radius * 1.12f, cy - radius * 0.3f, cx - radius * 0.92f, cy + radius * 0.3f),
            12f, 12f, paint
        )
        canvas.drawRoundRect(
            RectF(cx + radius * 0.92f, cy - radius * 0.3f, cx + radius * 1.12f, cy + radius * 0.3f),
            12f, 12f, paint
        )

        // Diadema de los audífonos
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.04f
        paint.color = Color.parseColor("#334155")
        val headBandRect = RectF(cx - radius * 1.05f, cy - radius * 1.1f, cx + radius * 1.05f, cy + radius * 0.5f)
        canvas.drawArc(headBandRect, 195f, 150f, false, paint)
        paint.style = Paint.Style.FILL

        // OJOS
        val eyeOffsetY = cy - radius * 0.1f
        val eyeSpacing = radius * 0.45f
        val eyeSize = radius * 0.28f

        if (isEyesClosed) {
            // Ojos cerrados / Parpadeo (Líneas de arco gamer)
            paint.color = Color.parseColor("#00E5FF")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.04f
            paint.strokeCap = Paint.Cap.ROUND

            val leftArc = RectF(cx - eyeSpacing - eyeSize, eyeOffsetY - eyeSize * 0.5f, cx - eyeSpacing + eyeSize, eyeOffsetY + eyeSize * 0.5f)
            val rightArc = RectF(cx + eyeSpacing - eyeSize, eyeOffsetY - eyeSize * 0.5f, cx + eyeSpacing + eyeSize, eyeOffsetY + eyeSize * 0.5f)
            canvas.drawArc(leftArc, 200f, 140f, false, paint)
            canvas.drawArc(rightArc, 200f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        } else {
            // Ojos abiertos (Visor digital resplandeciente)
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawCircle(cx - eyeSpacing, eyeOffsetY, eyeSize, paint)
            canvas.drawCircle(cx + eyeSpacing, eyeOffsetY, eyeSize, paint)

            // Pupilas tecnológicas
            paint.color = Color.parseColor("#FFFFFF")
            canvas.drawCircle(cx - eyeSpacing - eyeSize * 0.25f, eyeOffsetY - eyeSize * 0.25f, eyeSize * 0.35f, paint)
            canvas.drawCircle(cx + eyeSpacing - eyeSize * 0.25f, eyeOffsetY - eyeSize * 0.25f, eyeSize * 0.35f, paint)
        }

        // Nariz robótica sutil
        paint.color = Color.parseColor("#FF007F")
        canvas.drawCircle(cx, cy + radius * 0.15f, size * 0.02f, paint)

        // BOCA REACTIVA
        val mouthY = cy + radius * 0.38f
        if (isMouthOpen) {
            // Boca abierta (Hablando - Sonrisa abierta con colmillitos)
            paint.color = Color.parseColor("#1E1B4B")
            val mouthRect = RectF(cx - radius * 0.32f, mouthY - radius * 0.15f, cx + radius * 0.32f, mouthY + radius * 0.28f)
            canvas.drawRoundRect(mouthRect, 20f, 20f, paint)

            // Lengüita / Fondo de boca Neón
            paint.color = Color.parseColor("#FF0055")
            val tongueRect = RectF(cx - radius * 0.20f, mouthY + radius * 0.05f, cx + radius * 0.20f, mouthY + radius * 0.25f)
            canvas.drawRoundRect(tongueRect, 16f, 16f, paint)

            // Colmillitos gatunos
            paint.color = Color.WHITE
            val fangPath = Path()
            fangPath.moveTo(cx - radius * 0.18f, mouthY - radius * 0.15f)
            fangPath.lineTo(cx - radius * 0.12f, mouthY - radius * 0.02f)
            fangPath.lineTo(cx - radius * 0.06f, mouthY - radius * 0.15f)
            fangPath.close()

            fangPath.moveTo(cx + radius * 0.06f, mouthY - radius * 0.15f)
            fangPath.lineTo(cx + radius * 0.12f, mouthY - radius * 0.02f)
            fangPath.lineTo(cx + radius * 0.18f, mouthY - radius * 0.15f)
            fangPath.close()
            canvas.drawPath(fangPath, paint)
        } else {
            // Boca cerrada (Boceto 'w' clásico gatuno)
            paint.color = Color.parseColor("#00E5FF")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.035f
            paint.strokeCap = Paint.Cap.ROUND

            val wLeft = RectF(cx - radius * 0.25f, mouthY - radius * 0.1f, cx, mouthY + radius * 0.08f)
            val wRight = RectF(cx, mouthY - radius * 0.1f, cx + radius * 0.25f, mouthY + radius * 0.08f)
            canvas.drawArc(wLeft, 20f, 140f, false, paint)
            canvas.drawArc(wRight, 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // ==========================================
    // 2. AVATAR: ANIME AOI (Chibi Anime Chibi)
    // ==========================================
    private fun drawAnimeAoi(canvas: Canvas, cx: Float, cy: Float, size: Float, state: VtuberState) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size * 0.36f

        val isEyesClosed = state == VtuberState.BLINKING || state == VtuberState.BLINKING_TALKING
        val isMouthOpen = state == VtuberState.TALKING || state == VtuberState.BLINKING_TALKING

        // Coletas laterales de pelo azul
        paint.color = Color.parseColor("#2563EB")
        canvas.drawCircle(cx - radius * 0.95f, cy + radius * 0.1f, radius * 0.45f, paint)
        canvas.drawCircle(cx + radius * 0.95f, cy + radius * 0.1f, radius * 0.45f, paint)

        // Lazos rojos en las coletas
        paint.color = Color.parseColor("#EF4444")
        canvas.drawCircle(cx - radius * 0.75f, cy - radius * 0.2f, radius * 0.18f, paint)
        canvas.drawCircle(cx + radius * 0.75f, cy - radius * 0.2f, radius * 0.18f, paint)

        // Rostro / Piel suave
        paint.color = Color.parseColor("#FFE4D6")
        canvas.drawCircle(cx, cy, radius, paint)

        // Pelo trasero y flequillo superior
        val hairPath = Path()
        hairPath.moveTo(cx - radius * 1.05f, cy - radius * 0.1f)
        hairPath.quadTo(cx, cy - radius * 1.25f, cx + radius * 1.05f, cy - radius * 0.1f)
        hairPath.lineTo(cx + radius * 0.9f, cy - radius * 0.5f)
        hairPath.lineTo(cx + radius * 0.4f, cy - radius * 0.1f)
        hairPath.lineTo(cx, cy - radius * 0.45f)
        hairPath.lineTo(cx - radius * 0.4f, cy - radius * 0.1f)
        hairPath.lineTo(cx - radius * 0.9f, cy - radius * 0.5f)
        hairPath.close()

        paint.color = Color.parseColor("#3B82F6")
        canvas.drawPath(hairPath, paint)

        // Rubor en mejillas
        paint.color = Color.parseColor("#66FF6B81")
        canvas.drawCircle(cx - radius * 0.55f, cy + radius * 0.18f, radius * 0.18f, paint)
        canvas.drawCircle(cx + radius * 0.55f, cy + radius * 0.18f, radius * 0.18f, paint)

        // OJOS ANIME
        val eyeOffsetY = cy - radius * 0.05f
        val eyeSpacing = radius * 0.42f
        val eyeWidth = radius * 0.28f
        val eyeHeight = radius * 0.36f

        if (isEyesClosed) {
            // Ojos felices cerrados (> <)
            paint.color = Color.parseColor("#1E293B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.04f
            paint.strokeCap = Paint.Cap.ROUND

            val leftHappy = Path()
            leftHappy.moveTo(cx - eyeSpacing - eyeWidth * 0.8f, eyeOffsetY + eyeHeight * 0.2f)
            leftHappy.lineTo(cx - eyeSpacing, eyeOffsetY - eyeHeight * 0.2f)
            leftHappy.lineTo(cx - eyeSpacing + eyeWidth * 0.8f, eyeOffsetY + eyeHeight * 0.2f)
            canvas.drawPath(leftHappy, paint)

            val rightHappy = Path()
            rightHappy.moveTo(cx + eyeSpacing - eyeWidth * 0.8f, eyeOffsetY + eyeHeight * 0.2f)
            rightHappy.lineTo(cx + eyeSpacing, eyeOffsetY - eyeHeight * 0.2f)
            rightHappy.lineTo(cx + eyeSpacing + eyeWidth * 0.8f, eyeOffsetY + eyeHeight * 0.2f)
            canvas.drawPath(rightHappy, paint)
            paint.style = Paint.Style.FILL
        } else {
            // Ojos anime abiertos con degradado y brillos
            paint.color = Color.parseColor("#1D4ED8")
            val leftEyeRect = RectF(cx - eyeSpacing - eyeWidth * 0.7f, eyeOffsetY - eyeHeight * 0.6f, cx - eyeSpacing + eyeWidth * 0.7f, eyeOffsetY + eyeHeight * 0.6f)
            val rightEyeRect = RectF(cx + eyeSpacing - eyeWidth * 0.7f, eyeOffsetY - eyeHeight * 0.6f, cx + eyeSpacing + eyeWidth * 0.7f, eyeOffsetY + eyeHeight * 0.6f)
            canvas.drawRoundRect(leftEyeRect, 30f, 30f, paint)
            canvas.drawRoundRect(rightEyeRect, 30f, 30f, paint)

            // Reflejos de luz grandes (Kawai spark)
            paint.color = Color.WHITE
            canvas.drawCircle(cx - eyeSpacing - eyeWidth * 0.25f, eyeOffsetY - eyeHeight * 0.22f, eyeWidth * 0.35f, paint)
            canvas.drawCircle(cx + eyeSpacing - eyeWidth * 0.25f, eyeOffsetY - eyeHeight * 0.22f, eyeWidth * 0.35f, paint)
            canvas.drawCircle(cx - eyeSpacing + eyeWidth * 0.25f, eyeOffsetY + eyeHeight * 0.25f, eyeWidth * 0.18f, paint)
            canvas.drawCircle(cx + eyeSpacing + eyeWidth * 0.25f, eyeOffsetY + eyeHeight * 0.25f, eyeWidth * 0.18f, paint)
        }

        // BOCA
        val mouthY = cy + radius * 0.42f
        if (isMouthOpen) {
            // Boca abierta feliz
            paint.color = Color.parseColor("#E11D48")
            val openMouth = RectF(cx - radius * 0.25f, mouthY - radius * 0.1f, cx + radius * 0.25f, mouthY + radius * 0.32f)
            canvas.drawRoundRect(openMouth, 24f, 24f, paint)

            paint.color = Color.parseColor("#FDA4AF")
            canvas.drawCircle(cx, mouthY + radius * 0.20f, radius * 0.14f, paint)
        } else {
            // Sonrisa pequeña
            paint.color = Color.parseColor("#991B1B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.035f
            paint.strokeCap = Paint.Cap.ROUND
            val smileRect = RectF(cx - radius * 0.18f, mouthY - radius * 0.08f, cx + radius * 0.18f, mouthY + radius * 0.08f)
            canvas.drawArc(smileRect, 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // ==========================================
    // 3. AVATAR: PIXEL SLIME (Slime Gamer Retro)
    // ==========================================
    private fun drawPixelSlime(canvas: Canvas, cx: Float, cy: Float, size: Float, state: VtuberState) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size * 0.38f

        val isEyesClosed = state == VtuberState.BLINKING || state == VtuberState.BLINKING_TALKING
        val isMouthOpen = state == VtuberState.TALKING || state == VtuberState.BLINKING_TALKING

        // Corona Dorada Gamer
        val crownPath = Path()
        val crownY = cy - radius * 0.75f
        crownPath.moveTo(cx - radius * 0.45f, crownY)
        crownPath.lineTo(cx - radius * 0.55f, crownY - radius * 0.45f)
        crownPath.lineTo(cx - radius * 0.2f, crownY - radius * 0.2f)
        crownPath.lineTo(cx, crownY - radius * 0.55f)
        crownPath.lineTo(cx + radius * 0.2f, crownY - radius * 0.2f)
        crownPath.lineTo(cx + radius * 0.55f, crownY - radius * 0.45f)
        crownPath.lineTo(cx + radius * 0.45f, crownY)
        crownPath.close()

        paint.color = Color.parseColor("#FACC15")
        canvas.drawPath(crownPath, paint)

        // Rubí en la corona
        paint.color = Color.parseColor("#EF4444")
        canvas.drawCircle(cx, crownY - radius * 0.22f, radius * 0.09f, paint)

        // Cuerpo de Slime (Gota gelatinosa)
        val slimePath = Path()
        slimePath.moveTo(cx, cy - radius * 0.9f)
        slimePath.cubicTo(cx + radius * 1.15f, cy - radius * 0.7f, cx + radius * 1.25f, cy + radius * 0.95f, cx, cy + radius * 0.95f)
        slimePath.cubicTo(cx - radius * 1.25f, cy + radius * 0.95f, cx - radius * 1.15f, cy - radius * 0.7f, cx, cy - radius * 0.9f)
        slimePath.close()

        // Relleno Slime Verde Neón
        paint.color = Color.parseColor("#10B981")
        canvas.drawPath(slimePath, paint)

        // Brillo gelatinoso superior
        paint.color = Color.parseColor("#6EE7B7")
        canvas.drawCircle(cx - radius * 0.45f, cy - radius * 0.35f, radius * 0.22f, paint)

        // OJOS
        val eyeOffsetY = cy + radius * 0.05f
        val eyeSpacing = radius * 0.38f
        val eyeSize = radius * 0.18f

        if (isEyesClosed) {
            paint.color = Color.parseColor("#064E3B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.04f
            paint.strokeCap = Paint.Cap.ROUND

            val leftLine = RectF(cx - eyeSpacing - eyeSize, eyeOffsetY - eyeSize * 0.4f, cx - eyeSpacing + eyeSize, eyeOffsetY + eyeSize * 0.4f)
            val rightLine = RectF(cx + eyeSpacing - eyeSize, eyeOffsetY - eyeSize * 0.4f, cx + eyeSpacing + eyeSize, eyeOffsetY + eyeSize * 0.4f)
            canvas.drawArc(leftLine, 200f, 140f, false, paint)
            canvas.drawArc(rightLine, 200f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        } else {
            paint.color = Color.parseColor("#064E3B")
            canvas.drawCircle(cx - eyeSpacing, eyeOffsetY, eyeSize, paint)
            canvas.drawCircle(cx + eyeSpacing, eyeOffsetY, eyeSize, paint)

            // Brillo blanco
            paint.color = Color.WHITE
            canvas.drawCircle(cx - eyeSpacing - eyeSize * 0.3f, eyeOffsetY - eyeSize * 0.3f, eyeSize * 0.4f, paint)
            canvas.drawCircle(cx + eyeSpacing - eyeSize * 0.3f, eyeOffsetY - eyeSize * 0.3f, eyeSize * 0.4f, paint)
        }

        // BOCA
        val mouthY = cy + radius * 0.42f
        if (isMouthOpen) {
            paint.color = Color.parseColor("#047857")
            val openRect = RectF(cx - radius * 0.22f, mouthY - radius * 0.1f, cx + radius * 0.22f, mouthY + radius * 0.28f)
            canvas.drawRoundRect(openRect, 20f, 20f, paint)
        } else {
            paint.color = Color.parseColor("#064E3B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.035f
            paint.strokeCap = Paint.Cap.ROUND
            val smileRect = RectF(cx - radius * 0.15f, mouthY - radius * 0.06f, cx + radius * 0.15f, mouthY + radius * 0.06f)
            canvas.drawArc(smileRect, 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
