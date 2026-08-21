package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.example.model.TouchColorOption
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gestor del Visualizador de Toques Táctiles Animado (Touch Visualizer).
 * Renderiza ondas expansivas translúcidas y partículas de impacto táctil en tiempo real
 * sobre la pantalla SIN necesidad de habilitar las Opciones de Desarrollador de Android.
 */
class TouchVisualizerOverlay(
    private val context: Context,
    private var touchColor: TouchColorOption = TouchColorOption.CYAN
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var overlayView: TouchCanvasView? = null
    private var isShowingInternal = false

    val isShowing: Boolean get() = isShowingInternal

    private class TouchRipple(
        val x: Float,
        val y: Float,
        val colorArgb: Int,
        var currentRadius: Float = 0f,
        var maxRadius: Float = 60f,
        var alpha: Int = 220,
        var innerAlpha: Int = 255
    )

    private inner class TouchCanvasView(context: Context) : View(context) {
        private val ripples = CopyOnWriteArrayList<TouchRipple>()
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(3.5f)
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        fun addTouch(x: Float, y: Float) {
            val maxR = dpToPx(38f)
            val ripple = TouchRipple(
                x = x,
                y = y,
                colorArgb = touchColor.primaryArgb.toInt(),
                currentRadius = dpToPx(8f),
                maxRadius = maxR
            )
            ripples.add(ripple)

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 380
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    val frac = anim.animatedFraction
                    ripple.currentRadius = ripple.maxRadius * (0.2f + 0.8f * frac)
                    ripple.alpha = ((1f - frac) * 230).toInt().coerceIn(0, 255)
                    ripple.innerAlpha = ((1f - frac) * 200).toInt().coerceIn(0, 255)
                    postInvalidateOnAnimation()
                }
            }
            animator.start()

            // Limpiar ripple al terminar
            postDelayed({
                ripples.remove(ripple)
                postInvalidateOnAnimation()
            }, 400)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            for (r in ripples) {
                if (r.alpha <= 0) continue

                // 1. Núcleo central brillante
                centerDotPaint.color = r.colorArgb
                centerDotPaint.alpha = r.innerAlpha
                canvas.drawCircle(r.x, r.y, dpToPx(6f), centerDotPaint)

                // 2. Halo difuso (Glow)
                glowPaint.shader = RadialGradient(
                    r.x, r.y, r.currentRadius,
                    intArrayOf(r.colorArgb, Color.TRANSPARENT),
                    floatArrayOf(0.3f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                glowPaint.alpha = (r.alpha * 0.45f).toInt().coerceIn(0, 255)
                canvas.drawCircle(r.x, r.y, r.currentRadius, glowPaint)

                // 3. Anillo de onda expansiva
                ringPaint.color = r.colorArgb
                ringPaint.alpha = r.alpha
                canvas.drawCircle(r.x, r.y, r.currentRadius, ringPaint)
            }
        }
    }

    fun isOverlayAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowingInternal || !isOverlayAvailable() || windowManager == null) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Usamos FLAG_NOT_TOUCH_MODAL y FLAG_WATCH_OUTSIDE_TOUCH para capturar toques o mostrar efectos
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            val view = TouchCanvasView(context)
            this.overlayView = view

            windowManager.addView(view, params)
            isShowingInternal = true
            Log.i(TAG, "TouchVisualizerOverlay inicializado con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando TouchVisualizerOverlay: ${e.message}", e)
            dismiss()
        }
    }

    /**
     * Registra un toque dinámico en coordenadas de pantalla absoluta.
     */
    fun triggerTouch(x: Float, y: Float) {
        overlayView?.addTouch(x, y)
    }

    fun updateColor(newColor: TouchColorOption) {
        this.touchColor = newColor
        overlayView?.invalidate()
    }

    fun dismiss() {
        if (!isShowingInternal) return
        try {
            overlayView?.let {
                windowManager?.removeViewImmediate(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando TouchVisualizerOverlay: ${e.message}")
        } finally {
            overlayView = null
            isShowingInternal = false
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    companion object {
        private const val TAG = "TouchVisualizerOverlay"
    }
}
