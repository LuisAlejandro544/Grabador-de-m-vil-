package com.example.service.vtuber

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import com.example.model.RecordingConfig
import com.example.model.VtuberTrackingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Gestor de la ventana flotante del Avatar 2D Reactivo / PNGtuber.
 * Administra el ciclo de vida de la ventana en [WindowManager], el reactor de audio y el seguimiento facial local por IA.
 */
class VtuberOverlayManager(
    private val context: Context,
    private var config: RecordingConfig,
    private val onCloseClicked: () -> Unit
) {
    companion object {
        private const val TAG = "VtuberOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var overlayView: VtuberOverlayView? = null
    private var audioReactor: VtuberAudioReactor? = null
    private var cameraTracker: VtuberCameraTracker? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    var isShowing: Boolean = false
        private set

    fun show() {
        if (isShowing) return
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No hay permiso de superposición para mostrar el PNGtuber")
            return
        }

        try {
            val dpSize = config.vtuberSize.dpSize
            val pxSize = (dpSize * context.resources.displayMetrics.density).toInt()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                pxSize,
                pxSize,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                x = (20 * context.resources.displayMetrics.density).toInt()
                y = (120 * context.resources.displayMetrics.density).toInt()
            }

            val view = VtuberOverlayView(
                context = context,
                config = config,
                onCloseClicked = {
                    dismiss()
                    onCloseClicked()
                },
                onDragDelta = { dx, dy ->
                    layoutParams?.let { params ->
                        params.x += dx.toInt()
                        params.y -= dy.toInt() // En BOTTOM gravity, dy hacia arriba aumenta Y
                        try {
                            windowManager.updateViewLayout(overlayView, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error actualizando posición de PNGtuber: ${e.message}")
                        }
                    }
                }
            )

            windowManager.addView(view, layoutParams)
            overlayView = view
            isShowing = true

            setupTrackingPipelines()

            Log.i(TAG, "PNGtuber Overlay mostrado exitosamente con preset: ${config.vtuberPreset.label} y modo: ${config.vtuberTrackingMode.label}")
        } catch (t: Throwable) {
            Log.e(TAG, "Error al mostrar PNGtuber overlay: ${t.message}", t)
            isShowing = false
            overlayView = null
        }
    }

    private fun setupTrackingPipelines() {
        // Detener pipelines previos
        audioReactor?.stop()
        audioReactor = null
        cameraTracker?.stopTracking()
        cameraTracker = null

        when (config.vtuberTrackingMode) {
            VtuberTrackingMode.VOICE_ONLY -> {
                audioReactor = VtuberAudioReactor(managerScope) { state, amp ->
                    overlayView?.updateState(state, amp)
                }.apply {
                    sensitivity = config.vtuberSensitivity
                    start()
                }
            }
            VtuberTrackingMode.FACE_MESH_LOCAL -> {
                cameraTracker = VtuberCameraTracker(context, managerScope) { pose ->
                    overlayView?.updateFacePose(pose)
                }.apply {
                    startTracking(isFrontCamera = true)
                }
            }
            VtuberTrackingMode.HYBRID -> {
                audioReactor = VtuberAudioReactor(managerScope) { state, amp ->
                    if (amp > config.vtuberSensitivity) {
                        overlayView?.updateState(state, amp)
                    }
                }.apply {
                    sensitivity = config.vtuberSensitivity
                    start()
                }

                cameraTracker = VtuberCameraTracker(context, managerScope) { pose ->
                    overlayView?.updateFacePose(pose)
                }.apply {
                    startTracking(isFrontCamera = true)
                }
            }
        }
    }

    fun updateConfig(newConfig: RecordingConfig) {
        val modeChanged = this.config.vtuberTrackingMode != newConfig.vtuberTrackingMode
        this.config = newConfig
        overlayView?.updateConfig(newConfig)
        audioReactor?.sensitivity = newConfig.vtuberSensitivity

        if (modeChanged && isShowing) {
            setupTrackingPipelines()
        }

        // Redimensionar si cambió el tamaño
        layoutParams?.let { params ->
            val dpSize = newConfig.vtuberSize.dpSize
            val pxSize = (dpSize * context.resources.displayMetrics.density).toInt()
            if (params.width != pxSize || params.height != pxSize) {
                params.width = pxSize
                params.height = pxSize
                try {
                    windowManager.updateViewLayout(overlayView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error redimensionando PNGtuber: ${e.message}")
                }
            }
        }
    }

    fun onAudioVolume(amplitude: Float) {
        audioReactor?.onAudioAmplitude(amplitude)
    }

    fun dismiss() {
        if (!isShowing) return
        try {
            audioReactor?.stop()
            audioReactor = null
            cameraTracker?.stopTracking()
            cameraTracker = null
            managerScope.cancel()

            overlayView?.let {
                windowManager.removeView(it)
            }
            overlayView = null
            isShowing = false
            Log.i(TAG, "PNGtuber Overlay cerrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar PNGtuber overlay: ${e.message}", e)
        }
    }
}
