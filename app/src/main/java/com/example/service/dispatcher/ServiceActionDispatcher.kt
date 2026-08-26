package com.example.service.dispatcher

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.RecordingConfig
import com.example.service.RecordNotificationHelper
import com.example.service.ScreenCaptureEngine
import com.example.service.ServiceRecordingParams
import com.example.service.overlay.ServiceOverlayCoordinator

/**
 * Despachador modular de acciones y lanzador de interfaces flotantes para ScreenRecordService.
 */
class ServiceActionDispatcher(
    private val service: Service,
    private val captureEngine: ScreenCaptureEngine,
    private val overlayCoordinator: ServiceOverlayCoordinator,
    private val notificationHelper: RecordNotificationHelper
) {
    companion object {
        private const val TAG = "ServiceActionDispatcher"
    }

    fun startForegroundWithType(notification: Notification, isAudioEnabled: Boolean, showFacecam: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            
            val hasMicPermission = ContextCompat.checkSelfPermission(service, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (isAudioEnabled || hasMicPermission) {
                if (hasMicPermission) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
            }

            val hasCameraPermission = ContextCompat.checkSelfPermission(service, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (showFacecam || hasCameraPermission) {
                if (hasCameraPermission) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
            }

            try {
                service.startForeground(RecordNotificationHelper.NOTIFICATION_ID, notification, serviceType)
            } catch (e: SecurityException) {
                Log.w(TAG, "Fallback startForeground sin permisos opcionales: ${e.message}")
                service.startForeground(RecordNotificationHelper.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(RecordNotificationHelper.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            service.startForeground(RecordNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    fun launchActiveOverlays(params: ServiceRecordingParams, onActionCallbacks: OverlayActionCallbacks) {
        val config = params.savedConfig

        // 1. Facecam
        try {
            if (params.showFacecam) overlayCoordinator.launchFacecam(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Facecam: ${t.message}")
        }

        // 2. PNGtuber / VTuber
        try {
            if (config.showVtuber) overlayCoordinator.launchVtuber(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando PNGtuber: ${t.message}")
        }

        // 2.1 Avatar Reactivo a Toques ("Bongo Cat" Handcam)
        try {
            if (config.showTouchAvatar) overlayCoordinator.launchTouchAvatar(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Avatar Reactivo a Toques: ${t.message}")
        }

        // 3. Vúmetro
        try {
            if (config.showFloatingVuMeter) overlayCoordinator.launchVuMeter(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Vúmetro Flotante: ${t.message}")
        }

        // 4. Toques
        try {
            if (config.showTouchVisualizer) overlayCoordinator.launchTouchVisualizer(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Toques: ${t.message}")
        }

        // 5. Marca de Agua
        try {
            if (config.showWatermark) overlayCoordinator.launchWatermark(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Marca de Agua: ${t.message}")
        }

        // 6. Overlay de Escena
        try {
            if (config.showSceneOverlay) overlayCoordinator.launchSceneOverlay(config)
        } catch (t: Throwable) {
            Log.e(TAG, "Error iniciando Overlay de Escena: ${t.message}")
        }

        // 7. Burbuja Flotante
        if (params.showFloatingBubble) {
            try {
                overlayCoordinator.setupFloatingBubble(
                    isMicMuted = captureEngine.isMicrophoneMuted,
                    isBeautyActive = config.beautyFilterEnabled,
                    isRgbActive = config.facecamRgbBorder,
                    onPauseClicked = onActionCallbacks.onPause,
                    onResumeClicked = onActionCallbacks.onResume,
                    onStopClicked = onActionCallbacks.onStop,
                    onMicToggleClicked = onActionCallbacks.onToggleMic,
                    onScreenshotRequested = onActionCallbacks.onScreenshot
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error iniciando Burbuja Flotante: ${t.message}")
            }
        }
    }
}

data class OverlayActionCallbacks(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onStop: () -> Unit,
    val onToggleMic: () -> Unit,
    val onScreenshot: () -> Unit
)
