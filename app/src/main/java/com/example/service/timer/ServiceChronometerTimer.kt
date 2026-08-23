package com.example.service.timer

import android.content.Context
import android.util.Log
import com.example.data.SettingsRepository
import com.example.data.StorageMonitorHelper
import com.example.service.ScreenCaptureEngine
import com.example.service.RecordNotificationHelper
import com.example.service.overlay.ServiceOverlayCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gestor del cronómetro de grabación en tiempo real.
 * Realiza el conteo de segundos, actualiza la notificación y la burbuja flotante,
 * y ejecuta la comprobación periódica de espacio en disco en segundo plano.
 */
class ServiceChronometerTimer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val captureEngine: ScreenCaptureEngine,
    private val overlayCoordinator: ServiceOverlayCoordinator,
    private val notificationHelper: RecordNotificationHelper,
    private val settingsRepository: SettingsRepository,
    private val elapsedSecondsFlow: MutableStateFlow<Int>,
    private val onEmergencyStorageStop: (String) -> Unit
) {
    companion object {
        private const val TAG = "ServiceChronometerTimer"
    }

    private var timerJob: Job? = null

    fun start() {
        stop()
        timerJob = scope.launch {
            var checkStorageCounter = 0
            while (isActive) {
                delay(1000)
                if (captureEngine.isRecording && !captureEngine.isPaused) {
                    elapsedSecondsFlow.value += 1
                    val currentSec = elapsedSecondsFlow.value
                    overlayCoordinator.updateBubbleTime(currentSec)
                    notificationHelper.updateNotification(
                        currentSec.toLong(),
                        isPaused = false,
                        isMicrophoneEnabled = !captureEngine.isMicrophoneMuted
                    )

                    // Verificación activa periódica de espacio en disco (cada 4s)
                    checkStorageCounter++
                    if (checkStorageCounter >= 4) {
                        checkStorageCounter = 0
                        checkDiskSpaceSafeguard()
                    }
                }
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun checkDiskSpaceSafeguard() {
        try {
            val storageInfo = StorageMonitorHelper.queryStorageInfo(
                context = context,
                bitrateBps = settingsRepository.getConfig().bitrate.bps
            )
            if (storageInfo.availableBytes <= StorageMonitorHelper.EMERGENCY_STOP_THRESHOLD_BYTES) {
                Log.w(TAG, "¡EMERGENCIA! Espacio crítico alcanzado (${storageInfo.formattedAvailable}). Salvaguardando MP4...")
                val message = "Grabación salvada: Espacio en disco casi lleno (${storageInfo.formattedAvailable} restantes)."
                onEmergencyStorageStop(message)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error verificando espacio en disco: ${e.message}")
        }
    }
}
