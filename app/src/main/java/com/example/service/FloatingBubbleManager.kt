package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager

/**
 * Gestor modular de la burbuja flotante / widget superpuesto durante la grabación.
 * Coordina el ciclo de vida en WindowManager, la vista visual [BubbleOverlayView],
 * la detección de gestos de arrastre [BubbleTouchHandler], la captura rápida [ScreenshotHelper]
 * y el lienzo de dibujo en pantalla [ScreenDrawingOverlay].
 */
class FloatingBubbleManager(
    private val context: Context,
    private val onPauseClicked: () -> Unit,
    private val onResumeClicked: () -> Unit,
    private val onStopClicked: () -> Unit,
    private val onMicToggleClicked: (() -> Unit)? = null,
    private val onScreenshotRequested: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "FloatingBubbleManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var bubbleOverlayView: BubbleOverlayView? = null
    private var screenDrawingOverlay: ScreenDrawingOverlay? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShowingInternal = false

    val isShowing: Boolean get() = isShowingInternal

    /**
     * Comprueba si la aplicación tiene permiso para dibujar sobre otras aplicaciones.
     */
    fun isOverlayAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Muestra la burbuja flotante en pantalla y configura el arrastre táctil y herramientas.
     */
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

            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(16)
                y = dpToPx(120)
            }
            this.params = p

            val overlay = BubbleOverlayView(
                context = context,
                onPauseClicked = onPauseClicked,
                onResumeClicked = onResumeClicked,
                onStopClicked = onStopClicked,
                onMicToggleClicked = {
                    onMicToggleClicked?.invoke()
                },
                onScreenshotClicked = {
                    onScreenshotRequested?.invoke()
                },
                onDrawToolClicked = {
                    toggleDrawingOverlay()
                }
            )
            this.bubbleOverlayView = overlay

            val touchHandler = BubbleTouchHandler(
                windowManager = windowManager,
                params = p,
                onBubbleClick = { overlay.toggleExpand() }
            )
            overlay.rootView.setOnTouchListener(touchHandler)

            windowManager.addView(overlay.rootView, p)
            isShowingInternal = true
            overlay.startPulse()

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando la burbuja flotante en WindowManager: ${e.message}", e)
            isShowingInternal = false
            bubbleOverlayView = null
        }
    }

    /**
     * Abre o cierra el lienzo de dibujo sobre la pantalla.
     */
    private fun toggleDrawingOverlay() {
        if (screenDrawingOverlay?.isShowing == true) {
            screenDrawingOverlay?.dismiss()
            screenDrawingOverlay = null
        } else {
            screenDrawingOverlay = ScreenDrawingOverlay(context) {
                screenDrawingOverlay = null
            }.apply { show() }
        }
    }

    /**
     * Actualiza el contador de tiempo de la burbuja.
     */
    fun updateTime(elapsedSeconds: Int) {
        if (!isShowingInternal) return
        bubbleOverlayView?.updateTimer(elapsedSeconds)
    }

    /**
     * Actualiza el estado visual (grabando vs pausado).
     */
    fun updateStatus(paused: Boolean) {
        if (!isShowingInternal) return
        bubbleOverlayView?.updateStatus(paused)
    }

    /**
     * Actualiza el estado visual del micrófono (Voz ON vs Solo Juego).
     */
    fun updateMicStatus(muted: Boolean) {
        if (!isShowingInternal) return
        bubbleOverlayView?.updateMicStatus(muted)
    }

    /**
     * Oculta y remueve la vista del WindowManager y el lienzo de dibujo.
     */
    fun dismiss() {
        if (!isShowingInternal) return
        try {
            screenDrawingOverlay?.dismiss()
            screenDrawingOverlay = null

            bubbleOverlayView?.stopPulse()
            bubbleOverlayView?.let { overlay ->
                windowManager?.removeView(overlay.rootView)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al remover vista flotante: ${e.message}")
        } finally {
            bubbleOverlayView = null
            isShowingInternal = false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
