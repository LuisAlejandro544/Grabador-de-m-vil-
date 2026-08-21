package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig
import com.example.model.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "screen_record_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_RESUME = "com.example.service.RESUME"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RES_WIDTH = "extra_res_width"
        const val EXTRA_RES_HEIGHT = "extra_res_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_SHOW_FLOATING_BUBBLE = "extra_show_floating_bubble"

        private val _recordingState = MutableStateFlow(RecordingStatus.IDLE)
        val recordingState = _recordingState.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0)
        val elapsedSeconds = _elapsedSeconds.asStateFlow()

        private val _lastSavedFilePath = MutableStateFlow<String?>(null)
        val lastSavedFilePath = _lastSavedFilePath.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage = _errorMessage.asStateFlow()

        fun isRecording(): Boolean {
            val state = _recordingState.value
            return state == RecordingStatus.RECORDING || state == RecordingStatus.PAUSED
        }
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var floatingBubbleManager: FloatingBubbleManager? = null

    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                val width = intent.getIntExtra(EXTRA_RES_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_RES_HEIGHT, 1920)
                val fps = intent.getIntExtra(EXTRA_FPS, 60)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
                val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: AudioSourceType.MIC.name
                val showFloatingBubble = intent.getBooleanExtra(EXTRA_SHOW_FLOATING_BUBBLE, true)

                if (resultCode != 0 && resultData != null) {
                    startForegroundWithNotification("Iniciando grabación...")
                    startRecording(resultCode, resultData, width, height, fps, bitrate, audioSource, showFloatingBubble)
                } else {
                    Log.e(TAG, "Missing resultCode or resultData")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
            }
            ACTION_PAUSE -> {
                pauseRecording()
            }
            ACTION_RESUME -> {
                resumeRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(contentText: String) {
        val notification = buildNotification(contentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording(
        resultCode: Int,
        resultData: Intent,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        audioSource: String,
        showFloatingBubble: Boolean = true
    ) {
        try {
            _errorMessage.value = null
            _recordingState.value = RecordingStatus.RECORDING
            _elapsedSeconds.value = 0
            isPaused = false

            val metrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val screenDensity = metrics.densityDpi

            // Determine dimensions based on device orientation
            val isPortrait = metrics.heightPixels >= metrics.widthPixels
            val recWidth = if (isPortrait) minOf(width, height) else maxOf(width, height)
            val recHeight = if (isPortrait) maxOf(width, height) else minOf(width, height)

            // Setup output file
            val recordingsDir = getOutputDirectory()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentOutputFile = File(recordingsDir, "REC_$timeStamp.mp4")

            // Initialize MediaRecorder
            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            val isAudioEnabled = audioSource != AudioSourceType.NONE.name
            val isInternalOnly = audioSource == AudioSourceType.INTERNAL_GAME.name

            mediaRecorder?.apply {
                if (isAudioEnabled) {
                    try {
                        if (isInternalOnly) {
                            // Internal playback / game audio capture source
                            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                        } else {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set audio source, fallback to MIC: ${e.message}")
                        try {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                        } catch (e2: Exception) {
                            Log.w(TAG, "Audio source unavailable, proceeding video-only: ${e2.message}")
                        }
                    }
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(currentOutputFile?.absolutePath)
                setVideoSize(recWidth, recHeight)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (isAudioEnabled) {
                    try {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(192000)
                        setAudioSamplingRate(48000)
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio encoder error: ${e.message}")
                    }
                }
                setVideoEncodingBitRate(bitrate)
                setVideoFrameRate(fps)
                prepare()
            }

            // Obtain MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        Log.d(TAG, "MediaProjection stopped by system")
                        stopRecording()
                    }
                }, null)
            }

            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecordService",
                recWidth,
                recHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()

            // Setup Floating Bubble Overlay if requested
            if (showFloatingBubble) {
                floatingBubbleManager?.dismiss()
                floatingBubbleManager = FloatingBubbleManager(
                    context = this,
                    onPauseClicked = { pauseRecording() },
                    onResumeClicked = { resumeRecording() },
                    onStopClicked = { stopRecording() }
                )
                floatingBubbleManager?.show()
            }

            startTimer()
            updateNotification("Grabando pantalla (00:00)")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording: ${e.message}", e)
            _errorMessage.value = "Error al iniciar grabación: ${e.message}"
            _recordingState.value = RecordingStatus.ERROR
            cleanup()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                _recordingState.value = RecordingStatus.PAUSED
                floatingBubbleManager?.updateStatus(true)
                updateNotification("Grabación en pausa (${formatDuration(_elapsedSeconds.value)})")
            } catch (e: Exception) {
                Log.e(TAG, "Pause failed: ${e.message}")
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                _recordingState.value = RecordingStatus.RECORDING
                floatingBubbleManager?.updateStatus(false)
                updateNotification("Grabando pantalla (${formatDuration(_elapsedSeconds.value)})")
            } catch (e: Exception) {
                Log.e(TAG, "Resume failed: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        _recordingState.value = RecordingStatus.SAVING
        timerJob?.cancel()

        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Stop called in invalid state: ${e.message}")
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder: ${e.message}")
        }
        mediaRecorder = null

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VirtualDisplay: ${e.message}")
        }
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaProjection: ${e.message}")
        }
        mediaProjection = null

        currentOutputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                _lastSavedFilePath.value = file.absolutePath
                // Scan the file so it appears in system MediaStore
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(file.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            } else {
                file.delete()
                _errorMessage.value = "La grabación no pudo completarse correctamente."
            }
        }

        _recordingState.value = RecordingStatus.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!isPaused && _recordingState.value == RecordingStatus.RECORDING) {
                    _elapsedSeconds.value += 1
                    val formatted = formatDuration(_elapsedSeconds.value)
                    floatingBubbleManager?.updateTime(_elapsedSeconds.value)
                    updateNotification("Grabando pantalla ($formatted)")
                }
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    private fun cleanup() {
        timerJob?.cancel()
        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignored
        }
        mediaRecorder = null
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            // Ignored
        }
        virtualDisplay = null
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            // Ignored
        }
        mediaProjection = null
    }

    private fun getOutputDirectory(): File {
        val moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val recordingsDir = File(moviesDir, "Recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Servicio de Grabación de Pantalla"
            val description = "Muestra el estado de grabación de pantalla activa y controles de pausa/detener"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabador de Pantalla")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                val resumeIntent = Intent(this, ScreenRecordService::class.java).apply {
                    action = ACTION_RESUME
                }
                val resumePendingIntent = PendingIntent.getService(
                    this,
                    2,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_play, "Reanudar", resumePendingIntent)
            } else {
                val pauseIntent = Intent(this, ScreenRecordService::class.java).apply {
                    action = ACTION_PAUSE
                }
                val pausePendingIntent = PendingIntent.getService(
                    this,
                    3,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pausePendingIntent)
            }
        }

        return builder.build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        cleanup()
        _recordingState.value = RecordingStatus.IDLE
        super.onDestroy()
    }
}
