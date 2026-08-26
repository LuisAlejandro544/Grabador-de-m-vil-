package com.example.service.touchavatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.example.model.TouchAvatarGenre
import kotlin.math.sin

/**
 * Vista de alta eficiencia acelerada por hardware para renderizar el Avatar VTuber Reactivo a Toques Táctiles
 * ("Bongo Cat" / Handcam Táctil) sin requerir assets externos pesados.
 *
 * Soporta:
 * - 4 Géneros de juego: Ritmo (4 Teclas ← ↓ ↑ →), Shooter/FPS (Joystick + Gatillo), Arcade/Lucha (D-Pad + Botones ABXY) y Casual/Táctil.
 * - Animación física procedural de patas/manos reactivas al toque en pantalla.
 * - Sincronización bucal reactiva a amplitud de audio (micrófono/voz).
 * - Carga opcional de avatar de usuario personalizado o dibujo procedural nativo vectorizado.
 */
class TouchAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var genre: TouchAvatarGenre = TouchAvatarGenre.RHYTHM_4K
        set(value) {
            field = value
            invalidate()
        }

    var voiceSyncEnabled: Boolean = true
    var overlayAlpha: Float = 0.95f
        set(value) {
            field = value
            alpha = value
        }

    // Estados de animación táctil
    private var leftHandPressed = false
    private var rightHandPressed = false
    private var activeKeyIndex = -1 // 0: Left, 1: Down, 2: Up, 3: Right
    private var leftHandOffset = 0f
    private var rightHandOffset = 0f
    private var bodyBounceOffset = 0f
    private var mouthOpenRatio = 0f
    private var eyeBlinkRatio = 0f

    // Imagen personalizada del usuario (si la configura)
    private var customAvatarBitmap: Bitmap? = null
    private var customAvatarUri: String? = null

    // Herramientas de pintura reutilizables (Cero allocations en onDraw)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFDF5") // Blanco cálido crema
        style = Paint.Style.FILL
    }

    private val bodyOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1E24") // Delineado oscuro gamer
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val earInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8A9E") // Rosa pastel
        style = Paint.Style.FILL
    }

    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44FF8A9E") // Sonrojo translúcido
        style = Paint.Style.FILL
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1E24")
        style = Paint.Style.FILL
    }

    private val deskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#23272E") // Mesa / Pad gamer oscuro
        style = Paint.Style.FILL
    }

    private val deskOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B4252")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val keyDefaultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E3440")
        style = Paint.Style.FILL
    }

    private val keyOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C566A")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Colores dinámicos neón para teclas de ritmo / botones shooter
    private val keyColors = arrayOf(
        Color.parseColor("#C2410C"), // Violeta Neón (←)
        Color.parseColor("#00E5FF"), // Cian Neón (↓)
        Color.parseColor("#00E676"), // Verde Neón (↑)
        Color.parseColor("#FF1744")  // Rojo Neón (→)
    )

    private val path = Path()
    private val rectF = RectF()

    init {
        // Habilitar aceleración por hardware
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setCustomImageUri(uriString: String?) {
        if (uriString == customAvatarUri) return
        customAvatarUri = uriString
        if (uriString.isNullOrBlank()) {
            customAvatarBitmap?.recycle()
            customAvatarBitmap = null
            invalidate()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream)
                if (original != null) {
                    customAvatarBitmap = original
                }
            }
        } catch (_: Exception) {
            customAvatarBitmap = null
        }
        invalidate()
    }

    fun onScreenTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        val relX = (x / screenWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val relY = (y / screenHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)

        when (genre) {
            TouchAvatarGenre.RHYTHM_4K -> {
                // 4 carriles horizontales de ritmo
                val lane = (relX * 4f).toInt().coerceIn(0, 3)
                activeKeyIndex = lane
                leftHandPressed = (lane < 2)
                rightHandPressed = (lane >= 2)
            }
            TouchAvatarGenre.SHOOTER_FPS -> {
                // Izquierda: movimiento joystick; Derecha: apuntado y disparo
                leftHandPressed = (relX < 0.5f)
                rightHandPressed = (relX >= 0.5f)
                activeKeyIndex = if (relX < 0.5f) 0 else 1
            }
            TouchAvatarGenre.FIGHTING_ACTION -> {
                // Izquierda: D-Pad; Derecha: Botones A/B/X/Y
                leftHandPressed = (relX < 0.45f)
                rightHandPressed = (relX >= 0.45f)
                activeKeyIndex = if (relX < 0.45f) 0 else 1
            }
            TouchAvatarGenre.CASUAL_TAP -> {
                leftHandPressed = (relX < 0.5f)
                rightHandPressed = (relX >= 0.5f)
                activeKeyIndex = if (relX < 0.5f) 0 else 1
            }
        }

        // Rebote físico dinámico
        leftHandOffset = if (leftHandPressed) 12f else 0f
        rightHandOffset = if (rightHandPressed) 12f else 0f
        bodyBounceOffset = if (leftHandPressed || rightHandPressed) 4f else 0f

        postInvalidateOnAnimation()

        // Temporizador de liberación automática de teclas tras 120ms para pulsaciones rápidas
        removeCallbacks(releaseRunnable)
        postDelayed(releaseRunnable, 140)
    }

    private val releaseRunnable = Runnable {
        leftHandPressed = false
        rightHandPressed = false
        activeKeyIndex = -1
        leftHandOffset = 0f
        rightHandOffset = 0f
        bodyBounceOffset = 0f
        postInvalidateOnAnimation()
    }

    fun onAudioAmplitude(amplitude: Float) {
        if (!voiceSyncEnabled) return
        val targetMouth = (amplitude * 2.5f).coerceIn(0f, 1f)
        if (targetMouth != mouthOpenRatio) {
            mouthOpenRatio = targetMouth
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        bodyOutlinePaint.strokeWidth = 3f * density
        deskOutlinePaint.strokeWidth = 2f * density

        val customBmp = customAvatarBitmap
        if (customBmp != null && !customBmp.isRecycled) {
            // Dibujar avatar personalizado de usuario con rebote y manos overlay
            drawCustomAvatar(canvas, customBmp, w, h)
        } else {
            // Renderizado procedural vectorial nativo estilo Bongo Cat Gamer
            drawProceduralBongoCat(canvas, w, h, density)
        }

        // Dibujar el controlador o teclado gamer según el género
        drawGameController(canvas, w, h, density)

        // Dibujar las patas/manos superpuestas pulsando el teclado
        drawHands(canvas, w, h, density)
    }

    private fun drawCustomAvatar(canvas: Canvas, bmp: Bitmap, w: Float, h: Float) {
        val deskHeight = h * 0.35f
        val avatarHeight = h - deskHeight
        val bounceY = bodyBounceOffset

        rectF.set(w * 0.1f, bounceY, w * 0.9f, avatarHeight + bounceY)
        canvas.drawBitmap(bmp, null, rectF, null)
    }

    private fun drawProceduralBongoCat(canvas: Canvas, w: Float, h: Float, density: Float) {
        val bounceY = bodyBounceOffset
        val bodyCenterX = w * 0.5f
        val bodyCenterY = h * 0.42f + bounceY
        val bodyRadiusX = w * 0.36f
        val bodyRadiusY = h * 0.36f

        // 1. Orejas
        // Oreja Izquierda
        path.reset()
        path.moveTo(bodyCenterX - bodyRadiusX * 0.75f, bodyCenterY - bodyRadiusY * 0.5f)
        path.lineTo(bodyCenterX - bodyRadiusX * 0.95f, bodyCenterY - bodyRadiusY * 1.15f)
        path.lineTo(bodyCenterX - bodyRadiusX * 0.35f, bodyCenterY - bodyRadiusY * 0.85f)
        path.close()
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, bodyOutlinePaint)

        // Interior oreja izquierda
        path.reset()
        path.moveTo(bodyCenterX - bodyRadiusX * 0.72f, bodyCenterY - bodyRadiusY * 0.55f)
        path.lineTo(bodyCenterX - bodyRadiusX * 0.88f, bodyCenterY - bodyRadiusY * 1.05f)
        path.lineTo(bodyCenterX - bodyRadiusX * 0.42f, bodyCenterY - bodyRadiusY * 0.80f)
        path.close()
        canvas.drawPath(path, earInnerPaint)

        // Oreja Derecha
        path.reset()
        path.moveTo(bodyCenterX + bodyRadiusX * 0.75f, bodyCenterY - bodyRadiusY * 0.5f)
        path.lineTo(bodyCenterX + bodyRadiusX * 0.95f, bodyCenterY - bodyRadiusY * 1.15f)
        path.lineTo(bodyCenterX + bodyRadiusX * 0.35f, bodyCenterY - bodyRadiusY * 0.85f)
        path.close()
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, bodyOutlinePaint)

        // Interior oreja derecha
        path.reset()
        path.moveTo(bodyCenterX + bodyRadiusX * 0.72f, bodyCenterY - bodyRadiusY * 0.55f)
        path.lineTo(bodyCenterX + bodyRadiusX * 0.88f, bodyCenterY - bodyRadiusY * 1.05f)
        path.lineTo(bodyCenterX + bodyRadiusX * 0.42f, bodyCenterY - bodyRadiusY * 0.80f)
        path.close()
        canvas.drawPath(path, earInnerPaint)

        // 2. Cabeza y Cuerpo (Óvalo suave)
        rectF.set(
            bodyCenterX - bodyRadiusX,
            bodyCenterY - bodyRadiusY * 0.9f,
            bodyCenterX + bodyRadiusX,
            bodyCenterY + bodyRadiusY * 0.85f
        )
        canvas.drawOval(rectF, bodyPaint)
        canvas.drawOval(rectF, bodyOutlinePaint)

        // Sonrojo en mejillas
        canvas.drawCircle(bodyCenterX - bodyRadiusX * 0.55f, bodyCenterY + bodyRadiusY * 0.1f, 10f * density, blushPaint)
        canvas.drawCircle(bodyCenterX + bodyRadiusX * 0.55f, bodyCenterY + bodyRadiusY * 0.1f, 10f * density, blushPaint)

        // 3. Ojos (Líneas curvas o puntos)
        val eyeY = bodyCenterY - bodyRadiusY * 0.1f
        val leftEyeX = bodyCenterX - bodyRadiusX * 0.32f
        val rightEyeX = bodyCenterX + bodyRadiusX * 0.32f

        if (leftHandPressed || rightHandPressed) {
            // Ojos felices cerrados en arco (^ ^)
            path.reset()
            path.moveTo(leftEyeX - 8f * density, eyeY)
            path.quadTo(leftEyeX, eyeY - 6f * density, leftEyeX + 8f * density, eyeY)
            canvas.drawPath(path, bodyOutlinePaint)

            path.reset()
            path.moveTo(rightEyeX - 8f * density, eyeY)
            path.quadTo(rightEyeX, eyeY - 6f * density, rightEyeX + 8f * density, eyeY)
            canvas.drawPath(path, bodyOutlinePaint)
        } else {
            // Ojos redondos atentos (• •)
            canvas.drawCircle(leftEyeX, eyeY, 4.5f * density, eyePaint)
            canvas.drawCircle(rightEyeX, eyeY, 4.5f * density, eyePaint)
        }

        // 4. Boca (Reacciona a la voz o sonrisa en 'w')
        val mouthY = bodyCenterY + bodyRadiusY * 0.15f
        if (mouthOpenRatio > 0.25f) {
            // Boca abierta reactiva a la voz
            rectF.set(
                bodyCenterX - 8f * density,
                mouthY - 2f * density,
                bodyCenterX + 8f * density,
                mouthY + (12f * density * mouthOpenRatio)
            )
            canvas.drawOval(rectF, earInnerPaint)
            canvas.drawOval(rectF, bodyOutlinePaint)
        } else {
            // Sonrisa de gato :3
            path.reset()
            path.moveTo(bodyCenterX - 9f * density, mouthY)
            path.quadTo(bodyCenterX - 4.5f * density, mouthY + 5f * density, bodyCenterX, mouthY)
            path.quadTo(bodyCenterX + 4.5f * density, mouthY + 5f * density, bodyCenterX + 9f * density, mouthY)
            canvas.drawPath(path, bodyOutlinePaint)
        }
    }

    private fun drawGameController(canvas: Canvas, w: Float, h: Float, density: Float) {
        val deskTop = h * 0.62f
        val deskBottom = h - 2f

        // Pad / Base gamer
        rectF.set(w * 0.05f, deskTop, w * 0.95f, deskBottom)
        canvas.drawRoundRect(rectF, 12f * density, 12f * density, deskPaint)
        canvas.drawRoundRect(rectF, 12f * density, 12f * density, deskOutlinePaint)

        when (genre) {
            TouchAvatarGenre.RHYTHM_4K -> drawRhythmKeys(canvas, w, deskTop, deskBottom, density)
            TouchAvatarGenre.SHOOTER_FPS -> drawShooterControls(canvas, w, deskTop, deskBottom, density)
            TouchAvatarGenre.FIGHTING_ACTION -> drawArcadeControls(canvas, w, deskTop, deskBottom, density)
            TouchAvatarGenre.CASUAL_TAP -> drawCasualPad(canvas, w, deskTop, deskBottom, density)
        }
    }

    private fun drawRhythmKeys(canvas: Canvas, w: Float, top: Float, bottom: Float, density: Float) {
        val totalWidth = w * 0.82f
        val startX = w * 0.09f
        val keyWidth = totalWidth / 4f
        val keyHeight = (bottom - top) * 0.72f
        val keyTop = top + (bottom - top - keyHeight) * 0.5f

        val labels = arrayOf("←", "↓", "↑", "→")
        textPaint.textSize = 13f * density

        for (i in 0 until 4) {
            val kx = startX + i * keyWidth + 2f * density
            val kw = keyWidth - 4f * density
            rectF.set(kx, keyTop, kx + kw, keyTop + keyHeight)

            val isPressed = (activeKeyIndex == i)
            if (isPressed) {
                bodyPaint.color = keyColors[i]
                canvas.drawRoundRect(rectF, 6f * density, 6f * density, bodyPaint)
            } else {
                canvas.drawRoundRect(rectF, 6f * density, 6f * density, keyDefaultPaint)
            }
            canvas.drawRoundRect(rectF, 6f * density, 6f * density, keyOutlinePaint)

            // Flecha / Texto
            textPaint.color = if (isPressed) Color.BLACK else keyColors[i]
            canvas.drawText(labels[i], rectF.centerX(), rectF.centerY() + 4.5f * density, textPaint)
        }
    }

    private fun drawShooterControls(canvas: Canvas, w: Float, top: Float, bottom: Float, density: Float) {
        val centerY = (top + bottom) * 0.5f
        val leftCenterX = w * 0.28f
        val rightCenterX = w * 0.72f
        val radius = (bottom - top) * 0.32f

        // Joystick Izquierdo (Movimiento)
        val joyActive = leftHandPressed
        bodyPaint.color = if (joyActive) Color.parseColor("#00E5FF") else Color.parseColor("#2E3440")
        canvas.drawCircle(leftCenterX, centerY, radius, bodyPaint)
        canvas.drawCircle(leftCenterX, centerY, radius, keyOutlinePaint)
        textPaint.textSize = 11f * density
        textPaint.color = if (joyActive) Color.BLACK else Color.WHITE
        canvas.drawText("🕹️", leftCenterX, centerY + 4f * density, textPaint)

        // Botón Derecho (Disparo / Apuntado)
        val shootActive = rightHandPressed
        bodyPaint.color = if (shootActive) Color.parseColor("#FF1744") else Color.parseColor("#2E3440")
        canvas.drawCircle(rightCenterX, centerY, radius, bodyPaint)
        canvas.drawCircle(rightCenterX, centerY, radius, keyOutlinePaint)
        textPaint.textSize = 11f * density
        textPaint.color = if (shootActive) Color.BLACK else Color.WHITE
        canvas.drawText("🎯", rightCenterX, centerY + 4f * density, textPaint)
    }

    private fun drawArcadeControls(canvas: Canvas, w: Float, top: Float, bottom: Float, density: Float) {
        val centerY = (top + bottom) * 0.5f
        val leftCenterX = w * 0.28f
        val rightCenterX = w * 0.72f
        val btnRadius = (bottom - top) * 0.16f

        // D-Pad Izquierdo
        val dpadActive = leftHandPressed
        bodyPaint.color = if (dpadActive) Color.parseColor("#00E676") else Color.parseColor("#2E3440")
        rectF.set(leftCenterX - btnRadius * 1.8f, centerY - btnRadius * 0.8f, leftCenterX + btnRadius * 1.8f, centerY + btnRadius * 0.8f)
        canvas.drawRoundRect(rectF, 4f * density, 4f * density, bodyPaint)
        canvas.drawRoundRect(rectF, 4f * density, 4f * density, keyOutlinePaint)

        // 4 Botones Arcade Derechos (A / B)
        val btnActive = rightHandPressed
        bodyPaint.color = if (btnActive) Color.parseColor("#FFD600") else Color.parseColor("#2E3440")
        canvas.drawCircle(rightCenterX - btnRadius * 1.1f, centerY, btnRadius, bodyPaint)
        canvas.drawCircle(rightCenterX - btnRadius * 1.1f, centerY, btnRadius, keyOutlinePaint)

        bodyPaint.color = if (btnActive) Color.parseColor("#FF1744") else Color.parseColor("#2E3440")
        canvas.drawCircle(rightCenterX + btnRadius * 1.1f, centerY, btnRadius, bodyPaint)
        canvas.drawCircle(rightCenterX + btnRadius * 1.1f, centerY, btnRadius, keyOutlinePaint)
    }

    private fun drawCasualPad(canvas: Canvas, w: Float, top: Float, bottom: Float, density: Float) {
        val centerY = (top + bottom) * 0.5f
        val leftCenterX = w * 0.28f
        val rightCenterX = w * 0.72f

        textPaint.textSize = 12f * density
        textPaint.color = if (leftHandPressed) Color.parseColor("#00E5FF") else Color.parseColor("#78909C")
        canvas.drawText(if (leftHandPressed) "⚡ TAP!" else "👋 Tap L", leftCenterX, centerY + 4f * density, textPaint)

        textPaint.color = if (rightHandPressed) Color.parseColor("#FF4081") else Color.parseColor("#78909C")
        canvas.drawText(if (rightHandPressed) "⚡ TAP!" else "Tap R 👋", rightCenterX, centerY + 4f * density, textPaint)
    }

    private fun drawHands(canvas: Canvas, w: Float, h: Float, density: Float) {
        bodyPaint.color = Color.parseColor("#FFFDF5") // Blanco patita

        val leftArmStartX = w * 0.22f
        val leftArmStartY = h * 0.48f + bodyBounceOffset
        val leftTargetX = w * 0.30f
        val leftTargetY = h * 0.68f + leftHandOffset

        // Pata Izquierda
        path.reset()
        path.moveTo(leftArmStartX, leftArmStartY)
        path.quadTo(w * 0.16f, h * 0.60f, leftTargetX, leftTargetY)
        path.lineTo(leftTargetX + 12f * density, leftTargetY - 4f * density)
        path.quadTo(w * 0.32f, leftArmStartY + 8f * density, leftArmStartX + 14f * density, leftArmStartY)
        path.close()
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, bodyOutlinePaint)

        // Huellita Pata Izquierda
        canvas.drawCircle(leftTargetX + 4f * density, leftTargetY - 2f * density, 5f * density, bodyPaint)
        canvas.drawCircle(leftTargetX + 4f * density, leftTargetY - 2f * density, 5f * density, bodyOutlinePaint)

        val rightArmStartX = w * 0.78f
        val rightArmStartY = h * 0.48f + bodyBounceOffset
        val rightTargetX = w * 0.70f
        val rightTargetY = h * 0.68f + rightHandOffset

        // Pata Derecha
        path.reset()
        path.moveTo(rightArmStartX, rightArmStartY)
        path.quadTo(w * 0.84f, h * 0.60f, rightTargetX, rightTargetY)
        path.lineTo(rightTargetX - 12f * density, rightTargetY - 4f * density)
        path.quadTo(w * 0.68f, rightArmStartY + 8f * density, rightArmStartX - 14f * density, rightArmStartY)
        path.close()
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, bodyOutlinePaint)

        // Huellita Pata Derecha
        canvas.drawCircle(rightTargetX - 4f * density, rightTargetY - 2f * density, 5f * density, bodyPaint)
        canvas.drawCircle(rightTargetX - 4f * density, rightTargetY - 2f * density, 5f * density, bodyOutlinePaint)
    }
}
