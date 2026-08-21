package com.example.service

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Gestor táctil modular para el arrastre y reposicionamiento del widget flotante en pantalla.
 * Separa el cálculo de desplazamiento del ciclo de vida de la vista.
 */
class BubbleTouchHandler(
    private val windowManager: WindowManager?,
    private val params: WindowManager.LayoutParams,
    private val onBubbleClick: () -> Unit
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        private const val DRAG_THRESHOLD_PX = 10
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (Math.abs(dx) > DRAG_THRESHOLD_PX || Math.abs(dy) > DRAG_THRESHOLD_PX) {
                    isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        // Ignored if view is detached during animation
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    onBubbleClick()
                }
                return true
            }

            else -> return false
        }
    }
}
