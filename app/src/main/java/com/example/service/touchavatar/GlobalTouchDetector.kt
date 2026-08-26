package com.example.service.touchavatar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Receptor de eventos táctiles globales para el Avatar y Visualizador Táctil.
 * Utiliza una ventana flotante transparente con FLAG_NOT_TOUCH_MODAL y FLAG_WATCH_OUTSIDE_TOUCH
 * para detectar toques en pantalla sin bloquear los controles de los videojuegos.
 */
class GlobalTouchDetector(
    private val context: Context,
    private val onTouchEvent: (x: Float, y: Float) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var touchInterceptorView: View? = null
    private var isListeningInternal = false
    val isListening: Boolean get() = isListeningInternal

    @SuppressLint("ClickableViewAccessibility")
    fun start() {
        if (isListeningInternal || windowManager == null || !Settings.canDrawOverlays(context)) return

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Ventana de 1x1 píxel posicionada en la esquina para interceptar eventos outside sin bloquear interacción
            val params = WindowManager.LayoutParams(
                1,
                1,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            val view = View(context).apply {
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE ||
                        event.action == MotionEvent.ACTION_DOWN ||
                        event.action == MotionEvent.ACTION_MOVE
                    ) {
                        onTouchEvent(event.rawX, event.rawY)
                    }
                    false
                }
            }

            windowManager.addView(view, params)
            touchInterceptorView = view
            isListeningInternal = true
            Log.i(TAG, "GlobalTouchDetector iniciado con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando GlobalTouchDetector: ${e.message}")
        }
    }

    fun stop() {
        if (!isListeningInternal) return
        try {
            touchInterceptorView?.let { windowManager?.removeViewImmediate(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo GlobalTouchDetector: ${e.message}")
        } finally {
            touchInterceptorView = null
            isListeningInternal = false
        }
    }

    companion object {
        private const val TAG = "GlobalTouchDetector"
    }
}
