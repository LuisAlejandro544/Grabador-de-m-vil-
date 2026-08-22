package com.example.service.watermark

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Gestor táctil de arrastre (drag & drop) para el Logo / Marca de Agua Flotante.
 * Permite mover el logo a cualquier posición de la pantalla con fluidez.
 */
object WatermarkTouchHelper {

    fun attach(
        targetView: View,
        params: WindowManager.LayoutParams,
        windowManager: WindowManager,
        isShowingProvider: () -> Boolean,
        onSingleTap: (() -> Unit)? = null
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        targetView.setOnTouchListener { _, event ->
            if (!isShowingProvider()) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (Math.hypot(dx.toDouble(), dy.toDouble()) > 12) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(targetView, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onSingleTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
