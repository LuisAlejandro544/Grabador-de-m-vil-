package com.example.service.facecam

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * Gestor táctil de arrastre magnético y click en la ventana de Facecam.
 */
object FacecamTouchDragHelper {

    @SuppressLint("ClickableViewAccessibility")
    fun attach(
        container: View,
        params: WindowManager.LayoutParams,
        windowManager: WindowManager?,
        isShowingProvider: () -> Boolean,
        onSingleTap: () -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (initialTouchX - event.rawX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isClick = false
                    }

                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    if (isShowingProvider()) {
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        onSingleTap()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
