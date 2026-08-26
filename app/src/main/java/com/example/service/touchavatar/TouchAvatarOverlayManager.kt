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
import com.example.model.RecordingConfig
import com.example.model.TouchAvatarGenre
import com.example.model.TouchAvatarSize

/**
 * Gestor de la ventana flotante interactiva y arrastrable del Avatar VTuber Reactivo a Toques ("Bongo Cat" Handcam).
 */
class TouchAvatarOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
) {
    private var isShowingInternal = false
    val isShowing: Boolean get() = isShowingInternal

    private var avatarView: TouchAvatarView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    // Dimensiones en píxeles de pantalla
    private var screenWidth = 1080f
    private var screenHeight = 1920f

    fun isOverlayAvailable(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(config: RecordingConfig) {
        if (isShowingInternal || !isOverlayAvailable() || windowManager == null) {
            return
        }

        try {
            val metrics = context.resources.displayMetrics
            screenWidth = metrics.widthPixels.toFloat()
            screenHeight = metrics.heightPixels.toFloat()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val size = config.touchAvatarSize
            val widthPx = dpToPx(size.dpWidth.toFloat()).toInt()
            val heightPx = dpToPx(size.dpHeight.toFloat()).toInt()

            val params = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (screenWidth - widthPx - dpToPx(16f)).toInt().coerceAtLeast(0)
                y = (screenHeight * 0.55f).toInt()
            }
            this.windowParams = params

            val view = TouchAvatarView(context).apply {
                genre = config.touchAvatarGenre
                overlayAlpha = config.touchAvatarOpacity
                voiceSyncEnabled = config.touchAvatarVoiceSync
                setCustomImageUri(config.touchAvatarCustomImageUri)
            }

            // Hacer el avatar libremente arrastrable por la pantalla
            setupDragListener(view, params)

            this.avatarView = view
            windowManager.addView(view, params)
            isShowingInternal = true
            Log.i(TAG, "TouchAvatarOverlayManager inicializado con éxito (Género: ${config.touchAvatarGenre.name})")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando TouchAvatarOverlay: ${e.message}", e)
            dismiss()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    fun onScreenTouch(x: Float, y: Float) {
        avatarView?.onScreenTouch(x, y, screenWidth, screenHeight)
    }

    fun onAudioAmplitude(amplitude: Float) {
        avatarView?.onAudioAmplitude(amplitude)
    }

    fun updateConfig(config: RecordingConfig) {
        avatarView?.apply {
            genre = config.touchAvatarGenre
            overlayAlpha = config.touchAvatarOpacity
            voiceSyncEnabled = config.touchAvatarVoiceSync
            setCustomImageUri(config.touchAvatarCustomImageUri)
        }

        // Actualizar tamaño de ventana si cambió
        val params = windowParams
        val view = avatarView
        if (params != null && view != null && windowManager != null) {
            val size = config.touchAvatarSize
            val newWidthPx = dpToPx(size.dpWidth.toFloat()).toInt()
            val newHeightPx = dpToPx(size.dpHeight.toFloat()).toInt()
            if (params.width != newWidthPx || params.height != newHeightPx) {
                params.width = newWidthPx
                params.height = newHeightPx
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
    }

    fun dismiss() {
        if (!isShowingInternal) return
        try {
            avatarView?.let {
                windowManager?.removeViewImmediate(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando TouchAvatarOverlay: ${e.message}")
        } finally {
            avatarView = null
            windowParams = null
            isShowingInternal = false
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    companion object {
        private const val TAG = "TouchAvatarOverlayMgr"
    }
}
