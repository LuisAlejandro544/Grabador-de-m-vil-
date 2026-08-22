package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.service.facecam.FacecamControlsBar
import com.example.service.facecam.FacecamLifecycleOwner
import com.example.service.facecam.FacecamRgbBorderView
import com.example.service.facecam.FacecamShapeHelper
import com.example.service.facecam.FacecamTouchDragHelper

/**
 * Gestor modular de la ventana flotante de Facecam (Cámara superpuesta durante el gameplay).
 * Soporta formas geométricas recortadas, borde animado RGB arcoíris, filtro de belleza facial,
 * cambio instantáneo entre cámara frontal y trasera, y arrastre táctil fluido.
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

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootContainer: FrameLayout? = null
    private var cardContainer: FrameLayout? = null
    private var previewView: PreviewView? = null
    private var beautyOverlay: View? = null
    private var rgbBorderView: FacecamRgbBorderView? = null
    private var controlsBar: FacecamControlsBar? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private val customLifecycleOwner = FacecamLifecycleOwner()
    private var cameraProvider: ProcessCameraProvider? = null

    private var isShowingInternal = false
    private var currentShapeInternal: FacecamShape = shape
    private var currentSizeInternal: FacecamSize = size
    private var currentFpsInternal: FacecamFps = fps
    private var isFrontCameraInternal: Boolean = isFrontCamera
    private var beautyFilterEnabledInternal: Boolean = beautyFilterEnabled
    private var rgbBorderEnabledInternal: Boolean = rgbBorderEnabled

    val isShowing: Boolean get() = isShowingInternal
    val isFrontFacing: Boolean get() = isFrontCameraInternal
    val currentShape: FacecamShape get() = currentShapeInternal
    val currentSize: FacecamSize get() = currentSizeInternal
    val currentFps: FacecamFps get() = currentFpsInternal
    val isBeautyFilterEnabled: Boolean get() = beautyFilterEnabledInternal
    val isRgbBorderEnabled: Boolean get() = rgbBorderEnabledInternal

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /**
     * Muestra la ventana flotante de Facecam en pantalla e inicia el streaming con CameraX.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowingInternal || !isOverlayPermissionGranted() || windowManager == null) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val (widthPx, heightPx) = FacecamShapeHelper.calculateDimensions(context, currentShapeInternal, currentSizeInternal)
            val paddingGlow = FacecamShapeHelper.dpToPx(context, 12f).toInt()
            val totalW = widthPx + (paddingGlow * 2)
            val totalH = heightPx + (paddingGlow * 2)

            val params = WindowManager.LayoutParams(
                totalW,
                totalH,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = FacecamShapeHelper.dpToPx(context, 16f).toInt()
                y = FacecamShapeHelper.dpToPx(context, 180f).toInt()
            }
            this.windowParams = params

            val root = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(totalW, totalH)
                setPadding(paddingGlow, paddingGlow, paddingGlow, paddingGlow)
            }
            this.rootContainer = root

            val card = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.BLACK)
            }
            this.cardContainer = card

            FacecamShapeHelper.applyShapeOutline(context, card, currentShapeInternal)

            val preview = PreviewView(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            this.previewView = preview
            card.addView(preview)

            val beauty = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(0x28FFE4E1.toInt())
                visibility = if (beautyFilterEnabledInternal) View.VISIBLE else View.GONE
            }
            this.beautyOverlay = beauty
            card.addView(beauty)

            val rgb = FacecamRgbBorderView(
                context = context,
                shapeProvider = { currentShapeInternal },
                rgbEnabledProvider = { rgbBorderEnabledInternal }
            ).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            this.rgbBorderView = rgb
            card.addView(rgb)

            val controls = FacecamControlsBar(
                context = context,
                onFlipClicked = { toggleFacecamCamera() },
                onShapeClicked = { cycleNextShape() },
                onBeautyClicked = { toggleBeautyFilter() },
                onRgbClicked = { toggleRgbBorder() },
                onCloseClicked = {
                    dismiss()
                    onCloseClicked?.invoke()
                }
            )
            this.controlsBar = controls
            controls.updateBeautyIcon(beautyFilterEnabledInternal)
            controls.updateRgbIcon(rgbBorderEnabledInternal)
            card.addView(controls.layout)

            root.addView(card)

            FacecamTouchDragHelper.attach(
                container = root,
                params = params,
                windowManager = windowManager,
                isShowingProvider = { isShowingInternal },
                onSingleTap = { controls.toggleVisibility() }
            )

            windowManager.addView(root, params)
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
        val preview = previewView ?: return

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    this.cameraProvider = provider

                    val cameraSelector = if (isFrontCameraInternal) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val previewBuilder = Preview.Builder()
                    try {
                        val targetFps = currentFpsInternal.fps
                        val fpsRange = Range(targetFps, targetFps)
                        val camera2Extender = Camera2Interop.Extender(previewBuilder)
                        camera2Extender.setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            fpsRange
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo configurar target FPS range en Facecam: ${e.message}")
                    }

                    val previewUseCase = previewBuilder.build()
                    previewUseCase.setSurfaceProvider(preview.surfaceProvider)

                    provider.unbindAll()
                    provider.bindToLifecycle(customLifecycleOwner, cameraSelector, previewUseCase)

                    Log.d(TAG, "CameraX vinculada correctamente al Surface de Facecam (Frontal: $isFrontCameraInternal, FPS: ${currentFpsInternal.fps})")
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al vincular cámara en Facecam: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error en ProcessCameraProvider: ${e.message}", e)
        }
    }

    fun setFacecamFps(fps: FacecamFps) {
        if (currentFpsInternal == fps) return
        currentFpsInternal = fps
        if (isShowingInternal) {
            mainHandler.post {
                startCameraPreview()
            }
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
        beautyOverlay?.visibility = if (beautyFilterEnabledInternal) View.VISIBLE else View.GONE
        controlsBar?.updateBeautyIcon(beautyFilterEnabledInternal)
        onBeautyFilterToggled?.invoke(beautyFilterEnabledInternal)
    }

    fun toggleBeautyFilter() {
        setBeautyFilter(!beautyFilterEnabledInternal)
    }

    fun setRgbBorder(enabled: Boolean) {
        if (rgbBorderEnabledInternal == enabled) return
        rgbBorderEnabledInternal = enabled
        rgbBorderView?.updateRgbMode(rgbBorderEnabledInternal)
        controlsBar?.updateRgbIcon(rgbBorderEnabledInternal)
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
        val root = rootContainer ?: return
        val card = cardContainer ?: return
        val params = windowParams ?: return

        FacecamShapeHelper.applyShapeOutline(context, card, currentShapeInternal)

        val (widthPx, heightPx) = FacecamShapeHelper.calculateDimensions(context, currentShapeInternal, currentSizeInternal)
        val paddingGlow = FacecamShapeHelper.dpToPx(context, 12f).toInt()
        val totalW = widthPx + (paddingGlow * 2)
        val totalH = heightPx + (paddingGlow * 2)

        params.width = totalW
        params.height = totalH

        mainHandler.post {
            try {
                if (isShowingInternal) {
                    windowManager?.updateViewLayout(root, params)
                    rgbBorderView?.invalidate()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando dimensiones de Facecam: ${e.message}")
            }
        }
    }

    fun dismiss() {
        if (!isShowingInternal) return

        try {
            customLifecycleOwner.stop()
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}

            rgbBorderView?.release()

            if (rootContainer != null) {
                try {
                    windowManager?.removeView(rootContainer)
                } catch (e: Exception) {
                    Log.w(TAG, "Error al remover vista Facecam de WindowManager: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ocultando Facecam: ${e.message}")
        } finally {
            rootContainer = null
            cardContainer = null
            previewView = null
            beautyOverlay = null
            rgbBorderView = null
            controlsBar = null
            windowParams = null
            isShowingInternal = false
            Log.d(TAG, "Facecam flotante cerrado")
        }
    }
}
