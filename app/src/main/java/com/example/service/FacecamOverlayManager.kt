package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.service.facecam.FacecamCameraEngine
import com.example.service.facecam.FacecamLifecycleOwner
import com.example.service.facecam.FacecamViewHierarchy
import com.example.service.facecam.FacecamWindowHost

/**
 * Gestor modular de la ventana flotante de Facecam (Cámara superpuesta durante el gameplay).
 * Actúa como orquestador de alto nivel coordinando:
 * - [FacecamWindowHost]: Gestión de ventana en WindowManager y LayoutParams.
 * - [FacecamViewHierarchy]: Construcción de jerarquía de vistas, controles táctiles y decoraciones RGB.
 * - [FacecamCameraEngine]: Streaming de CameraX, selección de lente y target FPS.
 */
class FacecamOverlayManager(
    private val context: Context,
    shape: FacecamShape = FacecamShape.CIRCLE,
    size: FacecamSize = FacecamSize.MEDIUM,
    fps: FacecamFps = FacecamFps.FPS_30,
    isFrontCamera: Boolean = true,
    beautyFilterEnabled: Boolean = false,
    rgbBorderEnabled: Boolean = false,
    private val onCloseClicked: (() -> Unit)? = null,
    private val onShapeChanged: ((FacecamShape) -> Unit)? = null,
    private val onCameraFlipped: ((Boolean) -> Unit)? = null,
    private val onBeautyFilterToggled: ((Boolean) -> Unit)? = null,
    private val onRgbBorderToggled: ((Boolean) -> Unit)? = null
) {

    companion object {
        private const val TAG = "FacecamOverlayManager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowHost = FacecamWindowHost(context)
    private val customLifecycleOwner = FacecamLifecycleOwner()
    private val cameraEngine = FacecamCameraEngine(context, customLifecycleOwner)

    private var isShowingInternal = false
    private var currentShapeInternal: FacecamShape = shape
    private var currentSizeInternal: FacecamSize = size
    private var currentFpsInternal: FacecamFps = fps
    private var isFrontCameraInternal: Boolean = isFrontCamera
    private var beautyFilterEnabledInternal: Boolean = beautyFilterEnabled
    private var rgbBorderEnabledInternal: Boolean = rgbBorderEnabled

    private val viewHierarchy = FacecamViewHierarchy(
        context = context,
        shapeProvider = { currentShapeInternal },
        rgbEnabledProvider = { rgbBorderEnabledInternal },
        isShowingProvider = { isShowingInternal },
        onFlipClicked = { toggleFacecamCamera() },
        onShapeClicked = { cycleNextShape() },
        onBeautyClicked = { toggleBeautyFilter() },
        onRgbClicked = { toggleRgbBorder() },
        onCloseClicked = {
            dismiss()
            onCloseClicked?.invoke()
        }
    )

    val isShowing: Boolean get() = isShowingInternal
    val isFrontFacing: Boolean get() = isFrontCameraInternal
    val currentShape: FacecamShape get() = currentShapeInternal
    val currentSize: FacecamSize get() = currentSizeInternal
    val currentFps: FacecamFps get() = currentFpsInternal
    val isBeautyFilterEnabled: Boolean get() = beautyFilterEnabledInternal
    val isRgbBorderEnabled: Boolean get() = rgbBorderEnabledInternal

    fun isOverlayPermissionGranted(): Boolean = windowHost.isOverlayPermissionGranted()

    /**
     * Muestra la ventana flotante de Facecam en pantalla e inicia el streaming con CameraX.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val windowManager = windowHost.windowManager
        if (isShowingInternal || !isOverlayPermissionGranted() || windowManager == null) {
            return
        }

        try {
            val params = windowHost.createLayoutParams(currentShapeInternal, currentSizeInternal)
            val root = viewHierarchy.buildHierarchy(
                shape = currentShapeInternal,
                size = currentSizeInternal,
                beautyFilterEnabled = beautyFilterEnabledInternal,
                rgbBorderEnabled = rgbBorderEnabledInternal,
                windowParams = params,
                windowManager = windowManager
            )

            if (!windowHost.addView(root, params)) {
                return
            }

            isShowingInternal = true
            customLifecycleOwner.start()
            startCameraPreview()

            Log.i(TAG, "Facecam flotante mostrado con éxito (${currentShapeInternal.name}, ${currentSizeInternal.name})")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando Facecam flotante: ${e.message}", e)
            isShowingInternal = false
        }
    }

    private fun startCameraPreview() {
        val preview = viewHierarchy.previewView ?: return
        cameraEngine.startPreview(
            previewView = preview,
            isFrontCamera = isFrontCameraInternal,
            fps = currentFpsInternal
        )
    }

    fun setFacecamFps(fps: FacecamFps) {
        if (currentFpsInternal == fps) return
        currentFpsInternal = fps
        if (isShowingInternal) {
            mainHandler.post { startCameraPreview() }
        }
    }

    fun toggleFacecamCamera() {
        isFrontCameraInternal = !isFrontCameraInternal
        mainHandler.post {
            startCameraPreview()
            onCameraFlipped?.invoke(isFrontCameraInternal)
        }
    }

    fun setBeautyFilter(enabled: Boolean) {
        if (beautyFilterEnabledInternal == enabled) return
        beautyFilterEnabledInternal = enabled
        viewHierarchy.updateBeautyFilter(enabled)
        onBeautyFilterToggled?.invoke(beautyFilterEnabledInternal)
    }

    fun toggleBeautyFilter() {
        setBeautyFilter(!beautyFilterEnabledInternal)
    }

    fun setRgbBorder(enabled: Boolean) {
        if (rgbBorderEnabledInternal == enabled) return
        rgbBorderEnabledInternal = enabled
        viewHierarchy.updateRgbBorder(enabled)
        onRgbBorderToggled?.invoke(rgbBorderEnabledInternal)
    }

    fun toggleRgbBorder() {
        setRgbBorder(!rgbBorderEnabledInternal)
    }

    fun setShape(newShape: FacecamShape) {
        if (currentShapeInternal == newShape) return
        currentShapeInternal = newShape
        updateLayoutDimensions()
        onShapeChanged?.invoke(currentShapeInternal)
    }

    fun setSize(newSize: FacecamSize) {
        if (currentSizeInternal == newSize) return
        currentSizeInternal = newSize
        updateLayoutDimensions()
    }

    private fun cycleNextShape() {
        val values = FacecamShape.values()
        val nextIdx = (values.indexOf(currentShapeInternal) + 1) % values.size
        setShape(values[nextIdx])
    }

    private fun updateLayoutDimensions() {
        val root = viewHierarchy.rootContainer ?: return
        viewHierarchy.applyShape(currentShapeInternal)
        windowHost.updateDimensions(root, currentShapeInternal, currentSizeInternal)
    }

    fun dismiss() {
        if (!isShowingInternal) return

        try {
            customLifecycleOwner.stop()
            cameraEngine.release()
            windowHost.removeView(viewHierarchy.rootContainer)
            viewHierarchy.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error ocultando Facecam: ${e.message}")
        } finally {
            isShowingInternal = false
            Log.d(TAG, "Facecam flotante cerrado")
        }
    }
}
