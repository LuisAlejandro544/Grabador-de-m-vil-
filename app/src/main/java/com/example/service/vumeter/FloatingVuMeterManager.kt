package com.example.service.vumeter

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import com.example.model.RecordingConfig
import com.example.service.watermark.WatermarkTouchHelper

/**
 * Gestor modular del Vúmetro Flotante y Mezclador de Audio de Vortex Studio.
 * Desacopla la ventana flotante en WindowManager, la animación de barras de niveles a 30 FPS,
 * y los controles de ganancia y filtros en tiempo real.
 */
class FloatingVuMeterManager(
    private val context: Context,
    private var config: RecordingConfig,
    private val isMicMutedProvider: () -> Boolean,
    private val onAudioGainsChanged: (gameGain: Float, micGain: Float) -> Unit,
    private val onFiltersChanged: (noiseGate: Boolean, ducking: Boolean) -> Unit,
    private val onMicMuteToggled: (Boolean) -> Unit,
    private val audioLevelsProvider: () -> FloatArray,
    private val onCloseClicked: () -> Unit
) {
    companion object {
        private const val TAG = "FloatingVuMeterManager"
        private const val METER_REFRESH_INTERVAL_MS = 33L // ~30 FPS
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: FloatingVuMeterOverlayView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isShowingInternal = false
    val isShowing: Boolean get() = isShowingInternal

    private var currentGameGain: Float = config.gameAudioGain
    private var currentMicGain: Float = config.micAudioGain
    private var isGameMuted: Boolean = false
    private var isDuckingActive: Boolean = config.audioDuckingEnabled
    private var isNoiseGateActive: Boolean = config.noiseGateEnabled

    private val meterUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isShowingInternal || overlayView == null) return
            try {
                val levels = audioLevelsProvider()
                val gameLvl = if (levels.isNotEmpty()) levels[0] else 0f
                val micLvl = if (levels.size > 1) levels[1] else 0f

                overlayView?.updateLevels(gameLvl, micLvl)
            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando vúmetro: ${e.message}")
            }
            mainHandler.postDelayed(this, METER_REFRESH_INTERVAL_MS)
        }
    }

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

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

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(16)
                y = dpToPx(120)
            }
            this.windowParams = params

            val view = FloatingVuMeterOverlayView(
                context = context,
                initialGameGain = currentGameGain,
                initialMicGain = currentMicGain,
                isGameMutedInitial = isGameMuted,
                isMicMutedInitial = isMicMutedProvider(),
                isDuckingActiveInitial = isDuckingActive,
                isNoiseGateActiveInitial = isNoiseGateActive,
                onGameGainChanged = { gain ->
                    currentGameGain = gain
                    applyGainChanges()
                },
                onMicGainChanged = { gain ->
                    currentMicGain = gain
                    applyGainChanges()
                },
                onGameMuteToggled = { muted ->
                    isGameMuted = muted
                    applyGainChanges()
                },
                onMicMuteToggled = { muted ->
                    onMicMuteToggled(muted)
                },
                onDuckingToggled = { ducking ->
                    isDuckingActive = ducking
                    onFiltersChanged(isNoiseGateActive, isDuckingActive)
                },
                onNoiseGateToggled = { noiseGate ->
                    isNoiseGateActive = noiseGate
                    onFiltersChanged(isNoiseGateActive, isDuckingActive)
                },
                onCloseClicked = {
                    onCloseClicked()
                }
            )
            this.overlayView = view

            WatermarkTouchHelper.attach(
                targetView = view,
                params = params,
                windowManager = windowManager,
                isShowingProvider = { isShowingInternal },
                onSingleTap = {}
            )

            windowManager.addView(view, params)
            isShowingInternal = true

            // Iniciar bucle de refresco del vúmetro
            mainHandler.post(meterUpdateRunnable)

            Log.i(TAG, "Vúmetro Flotante de Audio mostrado con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando Vúmetro Flotante: ${e.message}", e)
            isShowingInternal = false
        }
    }

    private fun applyGainChanges() {
        val effectiveGameGain = if (isGameMuted) 0.0f else currentGameGain
        onAudioGainsChanged(effectiveGameGain, currentMicGain)
    }

    fun updateMicMuteStatus(muted: Boolean) {
        overlayView?.updateMicMuteStatus(muted)
    }

    fun updateConfig(newConfig: RecordingConfig) {
        this.config = newConfig
        this.currentGameGain = newConfig.gameAudioGain
        this.currentMicGain = newConfig.micAudioGain
        this.isDuckingActive = newConfig.audioDuckingEnabled
        this.isNoiseGateActive = newConfig.noiseGateEnabled
        applyGainChanges()
        onFiltersChanged(isNoiseGateActive, isDuckingActive)
    }

    fun dismiss() {
        if (!isShowingInternal) return
        mainHandler.removeCallbacks(meterUpdateRunnable)
        try {
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removiendo vista del Vúmetro Flotante: ${e.message}")
        } finally {
            overlayView = null
            windowParams = null
            isShowingInternal = false
            Log.d(TAG, "Vúmetro Flotante de Audio cerrado")
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
