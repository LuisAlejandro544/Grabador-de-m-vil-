package com.example.service.controller

import com.example.service.RecordNotificationHelper
import com.example.service.state.ServiceStateManager

/**
 * Controlador modular para la gestión y actualización reactiva de las notificaciones
 * en primer plano (Foreground Service) de Vortex Studio.
 */
class ServiceNotificationController(
    private val notificationHelper: RecordNotificationHelper
) {
    /**
     * Actualiza la notificación de grabación con el tiempo transcurrido, estado de pausa y micrófono.
     */
    fun updateNotification(isPaused: Boolean, isMicrophoneEnabled: Boolean) {
        val elapsed = ServiceStateManager.elapsedSeconds.value.toLong()
        notificationHelper.updateNotification(
            durationSeconds = elapsed,
            isPaused = isPaused,
            isMicrophoneEnabled = isMicrophoneEnabled
        )
    }

    /**
     * Construye la notificación inicial para Foreground Service.
     */
    fun buildInitialNotification(isAudioEnabled: Boolean) =
        notificationHelper.buildForegroundNotification(
            durationSeconds = 0L,
            isPaused = false,
            isMicrophoneEnabled = isAudioEnabled
        )
}
