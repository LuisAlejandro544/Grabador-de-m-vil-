package com.example.service.vtuber

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.nativecore.NativeVTuberFaceBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Gestor de seguimiento facial local para el Avatar VTuber basado en CameraX ImageAnalysis + C++ Nativo.
 * Ejecuta análisis visual 100% en el dispositivo sin necesidad de Surface ni vista previa en pantalla,
 * procesando YUV_420_888 en un hilo de fondo dedicado con latencia sub-5ms.
 */
class VtuberCameraTracker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFacePoseUpdated: (VtuberFacePose) -> Unit
) : LifecycleOwner {

    companion object {
        private const val TAG = "VtuberCameraTracker"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isTracking = false

    private var lastProcessTimestamp = 0L
    private val minFrameIntervalMs = 25L // ~40 FPS target for optimal battery & thermal headroom

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    fun startTracking(isFrontCamera: Boolean = true) {
        if (isTracking) return

        try {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            cameraExecutor = Executors.newSingleThreadExecutor()
            NativeVTuberFaceBridge.initTracker(320, 240)
            NativeVTuberFaceBridge.resetTemporalFilter()

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    this.cameraProvider = provider

                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(320, 240))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        processImageProxy(imageProxy, isFrontCamera)
                    }

                    provider.unbindAll()
                    provider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                    isTracking = true
                    Log.i(TAG, "Seguimiento facial local VTuber iniciado exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al vincular CameraX para VTuber Face Tracking: ${e.message}", e)
                    isTracking = false
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando VtuberCameraTracker: ${t.message}", t)
            isTracking = false
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTimestamp < minFrameIntervalMs) {
            imageProxy.close()
            return
        }
        lastProcessTimestamp = currentTime

        try {
            val planes = imageProxy.planes
            if (planes.isNotEmpty()) {
                val yPlane = planes[0]
                val yBuffer = yPlane.buffer
                val rowStride = yPlane.rowStride
                val width = imageProxy.width
                val height = imageProxy.height
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val remaining = yBuffer.remaining()
                val yBytes = ByteArray(remaining)
                yBuffer.get(yBytes)

                val result = NativeVTuberFaceBridge.processFrameYUV(
                    yPlaneData = yBytes,
                    rowStride = rowStride,
                    width = width,
                    height = height,
                    rotationDegrees = rotationDegrees,
                    isFrontCamera = isFrontCamera
                )

                if (result.faceDetected) {
                    val pose = VtuberFacePose(
                        headRoll = result.headRoll,
                        headPitch = result.headPitch,
                        headYaw = result.headYaw,
                        leftEyeOpenness = result.leftEyeOpenness,
                        rightEyeOpenness = result.rightEyeOpenness,
                        mouthOpenness = result.mouthOpenness,
                        smileRatio = result.smileRatio,
                        isFaceDetected = true
                    )
                    val canonical = pose.toCanonicalState()
                    val finalPose = pose.copy(state = canonical)

                    scope.launch(Dispatchers.Main) {
                        onFacePoseUpdated(finalPose)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error procesando fotograma de seguimiento facial: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        try {
            cameraProvider?.unbindAll()
            cameraProvider = null

            cameraExecutor?.shutdown()
            cameraExecutor = null

            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            NativeVTuberFaceBridge.resetTemporalFilter()
            Log.i(TAG, "Seguimiento facial local VTuber detenido y recursos liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener VtuberCameraTracker: ${e.message}", e)
        }
    }
}
