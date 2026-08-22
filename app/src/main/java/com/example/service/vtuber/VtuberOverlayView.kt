package com.example.service.vtuber

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import com.example.model.RecordingConfig
import com.example.model.VtuberPreset
import kotlin.math.abs

/**
 * Vista de ventana flotante para el Avatar 2D Reactivo / PNGtuber.
 * Renderiza el preset vectorial o las imágenes PNG personalizadas,
 * soporta rebote elástico al hablar y responde a gestos táctiles de arrastre.
 */
@SuppressLint("ViewConstructor")
class VtuberOverlayView(
    context: Context,
    private var config: RecordingConfig,
    private val onCloseClicked: () -> Unit,
    private val onDragDelta: (dx: Float, dy: Float) -> Unit
) : FrameLayout(context) {

    private var currentState: VtuberState = VtuberState.IDLE
    private var currentBounceOffset = 0f
    private var bounceAnimator: ValueAnimator? = null

    // Bitmaps para modo personalizado
    private var customIdleBitmap: Bitmap? = null
    private var customTalkBitmap: Bitmap? = null
    private var customBlinkBitmap: Bitmap? = null
    private var customBlinkTalkBitmap: Bitmap? = null

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var showControls = false

    private val closeButton: ImageButton
    private val avatarDrawingView: View

    init {
        setWillNotDraw(false)
        loadCustomBitmaps()

        avatarDrawingView = object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                drawAvatar(canvas)
            }
        }
        addView(avatarDrawingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Botón de cerrar discreto en esquina superior derecha
        closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.parseColor("#99000000"))
            setColorFilter(Color.WHITE)
            setPadding(8, 8, 8, 8)
            visibility = GONE
            setOnClickListener { onCloseClicked() }
        }
        val closeParams = LayoutParams(72, 72).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            setMargins(0, 0, 8, 0)
        }
        addView(closeButton, closeParams)

        setupTouchHandling()
    }

    private fun loadCustomBitmaps() {
        if (config.vtuberPreset == VtuberPreset.CUSTOM) {
            customIdleBitmap = VtuberPresetDrawables.loadBitmapFromUri(context, config.vtuberIdleImageUri)
            customTalkBitmap = VtuberPresetDrawables.loadBitmapFromUri(context, config.vtuberTalkImageUri)
            customBlinkBitmap = VtuberPresetDrawables.loadBitmapFromUri(context, config.vtuberBlinkImageUri)
            customBlinkTalkBitmap = VtuberPresetDrawables.loadBitmapFromUri(context, config.vtuberBlinkTalkImageUri)
        } else {
            customIdleBitmap = null
            customTalkBitmap = null
            customBlinkBitmap = null
            customBlinkTalkBitmap = null
        }
    }

    fun updateConfig(newConfig: RecordingConfig) {
        this.config = newConfig
        loadCustomBitmaps()
        avatarDrawingView.invalidate()
    }

    fun updateState(state: VtuberState, amplitude: Float) {
        val oldState = currentState
        currentState = state

        // Animación de rebote (Squash & Stretch / Bounce) al empezar a hablar
        if (config.vtuberBounceEnabled && (state == VtuberState.TALKING || state == VtuberState.BLINKING_TALKING)) {
            if (oldState != VtuberState.TALKING && oldState != VtuberState.BLINKING_TALKING) {
                triggerBounce()
            }
        }

        avatarDrawingView.invalidate()
    }

    private fun triggerBounce() {
        bounceAnimator?.cancel()
        bounceAnimator = ValueAnimator.ofFloat(0f, -18f, 0f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener { anim ->
                currentBounceOffset = anim.animatedValue as Float
                avatarDrawingView.invalidate()
            }
            start()
        }
    }

    private fun drawAvatar(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (config.vtuberPreset == VtuberPreset.CUSTOM) {
            val activeBitmap = getCustomBitmapForState(currentState)
            if (activeBitmap != null && !activeBitmap.isRecycled) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val src = Rect(0, 0, activeBitmap.width, activeBitmap.height)
                val cy = (h / 2f) + currentBounceOffset
                val size = minOf(w, h) * 0.92f
                val dst = RectF(
                    (w - size) / 2f,
                    cy - (size / 2f),
                    (w + size) / 2f,
                    cy + (size / 2f)
                )
                canvas.drawBitmap(activeBitmap, src, dst, paint)
                return
            }
        }

        // Renderizado del preset vectorial (Cyber Cat, Anime Aoi, Pixel Slime)
        VtuberPresetDrawables.drawPreset(
            canvas = canvas,
            width = w,
            height = h,
            preset = config.vtuberPreset,
            state = currentState,
            bounceOffset = currentBounceOffset
        )
    }

    private fun getCustomBitmapForState(state: VtuberState): Bitmap? {
        return when (state) {
            VtuberState.TALKING -> customTalkBitmap ?: customIdleBitmap
            VtuberState.BLINKING -> customBlinkBitmap ?: customIdleBitmap
            VtuberState.BLINKING_TALKING -> customBlinkTalkBitmap ?: customTalkBitmap ?: customIdleBitmap
            VtuberState.IDLE -> customIdleBitmap
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        onDragDelta(dx, dy)
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Toggle visibilidad del botón de cerrar
                        showControls = !showControls
                        closeButton.visibility = if (showControls) VISIBLE else GONE
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bounceAnimator?.cancel()
    }
}
