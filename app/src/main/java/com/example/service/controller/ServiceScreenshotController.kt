package com.example.service.controller

import android.content.Context
import android.util.Log
import com.example.service.ScreenCaptureEngine
import com.example.service.ScreenshotHelper
import java.io.File

/**
 * Controlador modular para la captura de instantáneas HD en tiempo real durante la grabación.
 * Maneja la estrategia dual:
 * 1. Captura directa desde ImageReader / MediaProjection en tiempo de ejecución.
 * 2. Fallback resiliente extrayendo el fotograma actual desde el archivo MP4 activo.
 */
class ServiceScreenshotController(
    private val context: Context,
    private val captureEngine: ScreenCaptureEngine
) {
    companion object {
        private const val TAG = "ServiceScreenshotCtrl"
    }

    fun captureScreenshot(
        width: Int,
        height: Int,
        densityDpi: Int,
        activeFile: File?,
        onSuccess: (File) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (captureEngine.activeProjection != null) {
            captureEngine.takeScreenshot(
                context = context,
                width = width,
                height = height,
                densityDpi = densityDpi,
                onSuccess = { shotFile ->
                    Log.i(TAG, "Screenshot tomado exitosamente: ${shotFile.absolutePath}")
                    onSuccess(shotFile)
                },
                onError = { err ->
                    Log.w(TAG, "Fallo ImageReader, intentando fallback de video: $err")
                    fallbackScreenshot(activeFile, onSuccess, onError)
                }
            )
        } else {
            fallbackScreenshot(activeFile, onSuccess, onError)
        }
    }

    private fun fallbackScreenshot(
        activeFile: File?,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        if (activeFile != null) {
            ScreenshotHelper.captureFrameFromVideo(
                context = context,
                videoFile = activeFile,
                onSuccess = { shotFile ->
                    Log.i(TAG, "Screenshot fallback OK: ${shotFile.absolutePath}")
                    onSuccess(shotFile)
                },
                onError = { e ->
                    Log.e(TAG, "Screenshot fallback falló: $e")
                    onError(e)
                }
            )
        } else {
            Log.w(TAG, "Screenshot no capturado: no hay archivo activo ni proyección")
            onError("No hay archivo activo ni proyección")
        }
    }
}
