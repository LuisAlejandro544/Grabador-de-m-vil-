package com.example.service.facecam

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.FacecamFps

/**
 * Motor modular para la gestión del ciclo de vida y streaming de CameraX en la Facecam.
 * Desacopla la selección de lente (frontal/trasera), rango de FPS y vinculación con PreviewView.
 */
class FacecamCameraEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "FacecamCameraEngine"
    }

    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Inicia o reinicia la vista previa de la cámara asociándola a la PreviewView y LifecycleOwner.
     */
    fun startPreview(
        previewView: PreviewView,
        isFrontCamera: Boolean,
        fps: FacecamFps,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        try {
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

                    val previewBuilder = Preview.Builder()
                    try {
                        val targetFps = fps.fps
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
                    previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase)

                    Log.d(TAG, "CameraX vinculada correctamente (Frontal: $isFrontCamera, FPS: ${fps.fps})")
                    onSuccess?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al vincular cámara en Facecam: ${e.message}", e)
                    onError?.invoke(e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error en ProcessCameraProvider: ${e.message}", e)
            onError?.invoke(e)
        }
    }

    /**
     * Desvincula y libera los casos de uso de CameraX.
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error desvinculando CameraX: ${e.message}")
        } finally {
            cameraProvider = null
        }
    }
}
