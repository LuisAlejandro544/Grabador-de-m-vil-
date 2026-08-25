package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Gestor modular de notificaciones para el servicio de grabación en primer plano.
 */
class RecordNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "screen_record_channel"
        const val CHANNEL_NAME = "Vortex Studio - Grabación de Pantalla"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Registra el canal de notificación con importancia baja para evitar ruidos molestos durante la grabación.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente de control de grabación y streaming"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Construye la notificación persistente del Foreground Service con controles de Pausa/Reanudar y Detener.
     */
    fun buildForegroundNotification(
        durationSeconds: Long,
        isPaused: Boolean,
        isMicrophoneEnabled: Boolean
    ): Notification {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción Detener
        val stopIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción Pausar / Reanudar (Android 24+)
        val pauseResumeAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                val resumeIntent = Intent(context, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_RESUME
                }
                val resumePendingIntent = PendingIntent.getService(
                    context,
                    2,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_play,
                    "Reanudar",
                    resumePendingIntent
                ).build()
            } else {
                val pauseIntent = Intent(context, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_PAUSE
                }
                val pausePendingIntent = PendingIntent.getService(
                    context,
                    3,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Pausar",
                    pausePendingIntent
                ).build()
            }
        } else {
            null
        }

        // Acción Dinámica Silenciar / Activar Voz (Micrófono)
        val toggleMicIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_TOGGLE_MIC
        }
        val toggleMicPendingIntent = PendingIntent.getService(
            context,
            4,
            toggleMicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val micActionTitle = if (isMicrophoneEnabled) "Silenciar Voz" else "Activar Voz"
        val micActionIcon = if (isMicrophoneEnabled) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_lock_silent_mode
        val toggleMicAction = NotificationCompat.Action.Builder(
            micActionIcon,
            micActionTitle,
            toggleMicPendingIntent
        ).build()

        val title = if (isPaused) "🔴 Grabación en Pausa ($timeFormatted)" else "🔴 Grabando Pantalla ($timeFormatted)"
        val contentText = if (isMicrophoneEnabled) {
            "Audio: Juego + Voz (Mic Activo) | Toca para abrir"
        } else {
            "Audio: Solo Audio del Juego (Voz Silenciada) | Toca para abrir"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)

        if (pauseResumeAction != null) {
            builder.addAction(pauseResumeAction)
        }
        builder.addAction(toggleMicAction)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    /**
     * Construye la notificación mostrada durante la cuenta regresiva antes de grabar.
     */
    fun buildCountdownNotification(secondsRemaining: Int): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("⏱️ Iniciando grabación en ${secondsRemaining}s")
            .setContentText("Preparando captura en segundo plano... Toca para abrir")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", stopPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    /**
     * Actualiza la notificación con el conteo regresivo activo.
     */
    fun updateCountdownNotification(secondsRemaining: Int) {
        val notification = buildCountdownNotification(secondsRemaining)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Actualiza la notificación activa con el tiempo transcurrido o estado de pausa.
     */
    fun updateNotification(
        durationSeconds: Long,
        isPaused: Boolean,
        isMicrophoneEnabled: Boolean
    ) {
        val notification = buildForegroundNotification(durationSeconds, isPaused, isMicrophoneEnabled)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancela la notificación cuando el servicio se detiene.
     */
    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
