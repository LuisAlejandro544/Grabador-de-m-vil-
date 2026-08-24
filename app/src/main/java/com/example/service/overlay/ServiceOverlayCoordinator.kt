package com.example.service.overlay

import android.content.Context
import android.util.Log
import com.example.data.SettingsRepository
import com.example.model.RecordingConfig
import com.example.service.FacecamOverlayManager
import com.example.service.FloatingBubbleManager
import com.example.service.SceneOverlayManager
import com.example.service.TouchVisualizerOverlay
import com.example.service.WatermarkOverlayManager
import com.example.service.vtuber.VtuberOverlayManager
import com.example.service.vumeter.FloatingVuMeterManager

/**
 * Coordinador modular de todas las capas visuales y widgets flotantes del servicio.
 * Desacopla la gestión de Facecam, PNGtuber / Avatar 2D, Vúmetro Flotante, Burbuja Flotante,
 * Toques Táctiles, Marca de Agua y Overlays de Escena fuera del ciclo de vida de [ScreenRecordService].
 */
class ServiceOverlayCoordinator(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onFacecamStateChanged: (Boolean) -> Unit,
    private val onBeautyStateChanged: (Boolean) -> Unit,
    private val onRgbStateChanged: (Boolean) -> Unit,
    private val onTouchStateChanged: (Boolean) -> Unit,
    private val onWatermarkStateChanged: (Boolean) -> Unit,
    private val onSceneOverlayStateChanged: (Boolean) -> Unit,
    private val onVtuberStateChanged: (Boolean) -> Unit = {},
    private val onVuMeterStateChanged: (Boolean) -> Unit = {},
    private val onAudioGainsChanged: (gameGain: Float, micGain: Float) -> Unit = { _, _ -> },
    private val onAudioFiltersChanged: (noiseGate: Boolean, ducking: Boolean) -> Unit = { _, _ -> },
    private val onMicMuteToggled: (Boolean) -> Unit = {},
    private val isMicMutedProvider: () -> Boolean = { false },
    private val audioLevelsProvider: () -> FloatArray = { floatArrayOf(0f, 0f, 0f, 1f) }
) {
    companion object {
        private const val TAG = "ServiceOverlayCoord"
    }

    private var floatingBubbleManager: FloatingBubbleManager? = null
    private var facecamOverlayManager: FacecamOverlayManager? = null
    private var touchVisualizerOverlay: TouchVisualizerOverlay? = null
    private var watermarkOverlayManager: WatermarkOverlayManager? = null
    private var sceneOverlayManager: SceneOverlayManager? = null
    private var vtuberOverlayManager: VtuberOverlayManager? = null
    private var floatingVuMeterManager: FloatingVuMeterManager? = null

    val isFacecamActive: Boolean get() = facecamOverlayManager?.isShowing == true
    val isTouchActive: Boolean get() = touchVisualizerOverlay?.isShowing == true
    val isWatermarkActive: Boolean get() = watermarkOverlayManager?.isShowing == true
    val isSceneOverlayActive: Boolean get() = sceneOverlayManager?.isShowing == true
    val isVtuberActive: Boolean get() = vtuberOverlayManager?.isShowing == true
    val isVuMeterActive: Boolean get() = floatingVuMeterManager?.isShowing == true

    fun onConfigUpdated(config: RecordingConfig) {
        if (facecamOverlayManager?.isShowing == true) {
            facecamOverlayManager?.setShape(config.facecamShape)
            facecamOverlayManager?.setSize(config.facecamSize)
            facecamOverlayManager?.setFacecamFps(config.facecamFps)
            facecamOverlayManager?.setBeautyFilter(config.beautyFilterEnabled)
            facecamOverlayManager?.setRgbBorder(config.facecamRgbBorder)
        }
        touchVisualizerOverlay?.updateColor(config.touchVisualizerColor)
        watermarkOverlayManager?.updateConfig(config)
        sceneOverlayManager?.updateConfig(config)
        vtuberOverlayManager?.updateConfig(config)
        floatingVuMeterManager?.updateConfig(config)
    }

    fun setupFloatingBubble(
        isMicMuted: Boolean,
        isBeautyActive: Boolean,
        isRgbActive: Boolean,
        hideInFinalVideo: Boolean = false,
        onPauseClicked: () -> Unit,
        onResumeClicked: () -> Unit,
        onStopClicked: () -> Unit,
        onMicToggleClicked: () -> Unit,
        onScreenshotRequested: () -> Unit
    ) {
        floatingBubbleManager?.dismiss()
        floatingBubbleManager = FloatingBubbleManager(
            context = context,
            hideInFinalVideo = hideInFinalVideo,
            onPauseClicked = onPauseClicked,
            onResumeClicked = onResumeClicked,
            onStopClicked = onStopClicked,
            onMicToggleClicked = onMicToggleClicked,
            onScreenshotRequested = onScreenshotRequested,
            onFacecamToggleClicked = { toggleFacecam() },
            onBeautyToggleClicked = { toggleBeauty() },
            onRgbBorderToggleClicked = { toggleRgbBorder() },
            onTouchToggleClicked = { toggleTouchVisualizer() },
            onWatermarkToggleClicked = { toggleWatermark() },
            onSceneOverlayToggleClicked = { toggleSceneOverlay() },
            onVtuberToggleClicked = { toggleVtuber() },
            onVuMeterToggleClicked = { toggleVuMeter() }
        ).apply {
            show()
            updateMicStatus(isMicMuted)
            updateFacecamStatus(isFacecamActive)
            updateBeautyStatus(isBeautyActive)
            updateRgbStatus(isRgbActive)
            updateTouchStatus(isTouchActive)
            updateWatermarkStatus(isWatermarkActive)
            updateSceneOverlayStatus(isSceneOverlayActive)
            updateVtuberStatus(isVtuberActive)
            updateVuMeterStatus(isVuMeterActive)
        }
    }

    fun updateBubbleTime(seconds: Int) {
        floatingBubbleManager?.updateTime(seconds)
    }

    fun updateBubbleStatus(isPaused: Boolean) {
        floatingBubbleManager?.updateStatus(isPaused)
    }

    fun updateBubbleMicStatus(isMuted: Boolean) {
        floatingBubbleManager?.updateMicStatus(isMuted)
        floatingVuMeterManager?.updateMicMuteStatus(isMuted)
    }

    fun launchFacecam(config: RecordingConfig) {
        facecamOverlayManager?.dismiss()
        facecamOverlayManager = FacecamOverlayManager(
            context = context,
            shape = config.facecamShape,
            size = config.facecamSize,
            fps = config.facecamFps,
            isFrontCamera = config.isFrontCamera,
            beautyFilterEnabled = config.beautyFilterEnabled,
            rgbBorderEnabled = config.facecamRgbBorder,
            onCloseClicked = {
                onFacecamStateChanged(false)
                floatingBubbleManager?.updateFacecamStatus(false)
                settingsRepository.toggleFacecam(false)
            },
            onShapeChanged = { newShape ->
                settingsRepository.updateFacecamShape(newShape)
            },
            onCameraFlipped = { isFront ->
                settingsRepository.setFacecamCamera(isFront)
            },
            onBeautyFilterToggled = { enabled ->
                onBeautyStateChanged(enabled)
                floatingBubbleManager?.updateBeautyStatus(enabled)
                settingsRepository.toggleBeautyFilter(enabled)
            },
            onRgbBorderToggled = { enabled ->
                onRgbStateChanged(enabled)
                floatingBubbleManager?.updateRgbStatus(enabled)
                settingsRepository.toggleFacecamRgbBorder(enabled)
            }
        ).apply {
            show()
        }
        val isShowing = facecamOverlayManager?.isShowing == true
        onFacecamStateChanged(isShowing)
        floatingBubbleManager?.updateFacecamStatus(isShowing)
    }

    fun toggleFacecam() {
        if (facecamOverlayManager?.isShowing == true) {
            facecamOverlayManager?.dismiss()
            facecamOverlayManager = null
            onFacecamStateChanged(false)
            floatingBubbleManager?.updateFacecamStatus(false)
            settingsRepository.toggleFacecam(false)
            Log.i(TAG, "Facecam desactivada")
        } else {
            val config = settingsRepository.getConfig()
            launchFacecam(config)
            settingsRepository.toggleFacecam(true)
            Log.i(TAG, "Facecam activada")
        }
    }

    fun toggleBeauty() {
        val current = settingsRepository.getConfig().beautyFilterEnabled
        val newState = !current
        settingsRepository.toggleBeautyFilter(newState)
        onBeautyStateChanged(newState)
        facecamOverlayManager?.setBeautyFilter(newState)
        floatingBubbleManager?.updateBeautyStatus(newState)
        Log.i(TAG, "Filtro de Belleza: $newState")
    }

    fun toggleRgbBorder() {
        val current = settingsRepository.getConfig().facecamRgbBorder
        val newState = !current
        settingsRepository.toggleFacecamRgbBorder(newState)
        onRgbStateChanged(newState)
        facecamOverlayManager?.setRgbBorder(newState)
        floatingBubbleManager?.updateRgbStatus(newState)
        Log.i(TAG, "Borde RGB Arcoíris: $newState")
    }

    fun launchTouchVisualizer(config: RecordingConfig) {
        touchVisualizerOverlay?.dismiss()
        touchVisualizerOverlay = TouchVisualizerOverlay(
            context = context,
            touchColor = config.touchVisualizerColor
        ).apply {
            show()
        }
        val isShowing = touchVisualizerOverlay?.isShowing == true
        onTouchStateChanged(isShowing)
        floatingBubbleManager?.updateTouchStatus(isShowing)
    }

    fun toggleTouchVisualizer() {
        if (touchVisualizerOverlay?.isShowing == true) {
            touchVisualizerOverlay?.dismiss()
            touchVisualizerOverlay = null
            onTouchStateChanged(false)
            floatingBubbleManager?.updateTouchStatus(false)
            settingsRepository.toggleTouchVisualizer(false)
            Log.i(TAG, "Touch Visualizer desactivado")
        } else {
            val config = settingsRepository.getConfig()
            settingsRepository.toggleTouchVisualizer(true)
            launchTouchVisualizer(config)
            Log.i(TAG, "Touch Visualizer activado")
        }
    }

    fun launchWatermark(config: RecordingConfig) {
        watermarkOverlayManager?.dismiss()
        watermarkOverlayManager = WatermarkOverlayManager(
            context = context,
            config = config,
            onCloseClicked = {
                onWatermarkStateChanged(false)
                floatingBubbleManager?.updateWatermarkStatus(false)
                settingsRepository.toggleWatermark(false)
            }
        ).apply {
            show()
        }
        val isShowing = watermarkOverlayManager?.isShowing == true
        onWatermarkStateChanged(isShowing)
        floatingBubbleManager?.updateWatermarkStatus(isShowing)
    }

    fun toggleWatermark() {
        if (watermarkOverlayManager?.isShowing == true) {
            watermarkOverlayManager?.dismiss()
            watermarkOverlayManager = null
            onWatermarkStateChanged(false)
            floatingBubbleManager?.updateWatermarkStatus(false)
            settingsRepository.toggleWatermark(false)
            Log.i(TAG, "Marca de Agua desactivada")
        } else {
            val config = settingsRepository.getConfig()
            settingsRepository.toggleWatermark(true)
            launchWatermark(config)
            Log.i(TAG, "Marca de Agua activada")
        }
    }

    fun launchSceneOverlay(config: RecordingConfig) {
        sceneOverlayManager?.dismiss()
        sceneOverlayManager = SceneOverlayManager(
            context = context,
            config = config
        ).apply {
            show()
        }
        val isShowing = sceneOverlayManager?.isShowing == true
        onSceneOverlayStateChanged(isShowing)
        floatingBubbleManager?.updateSceneOverlayStatus(isShowing)
    }

    fun toggleSceneOverlay() {
        if (sceneOverlayManager?.isShowing == true) {
            sceneOverlayManager?.dismiss()
            sceneOverlayManager = null
            onSceneOverlayStateChanged(false)
            floatingBubbleManager?.updateSceneOverlayStatus(false)
            settingsRepository.toggleSceneOverlay(false)
            Log.i(TAG, "Scene Overlay desactivado")
        } else {
            val config = settingsRepository.getConfig()
            settingsRepository.toggleSceneOverlay(true)
            launchSceneOverlay(config)
            Log.i(TAG, "Scene Overlay activado")
        }
    }

    fun launchVtuber(config: RecordingConfig) {
        try {
            vtuberOverlayManager?.dismiss()
            vtuberOverlayManager = VtuberOverlayManager(
                context = context,
                config = config,
                onCloseClicked = {
                    onVtuberStateChanged(false)
                    floatingBubbleManager?.updateVtuberStatus(false)
                    settingsRepository.toggleVtuber(false)
                }
            ).apply {
                show()
            }
            val isShowing = vtuberOverlayManager?.isShowing == true
            onVtuberStateChanged(isShowing)
            floatingBubbleManager?.updateVtuberStatus(isShowing)
        } catch (t: Throwable) {
            Log.e(TAG, "Error lanzando PNGtuber: ${t.message}", t)
            onVtuberStateChanged(false)
            floatingBubbleManager?.updateVtuberStatus(false)
        }
    }

    fun toggleVtuber() {
        if (vtuberOverlayManager?.isShowing == true) {
            vtuberOverlayManager?.dismiss()
            vtuberOverlayManager = null
            onVtuberStateChanged(false)
            floatingBubbleManager?.updateVtuberStatus(false)
            settingsRepository.toggleVtuber(false)
            Log.i(TAG, "PNGtuber Overlay desactivado")
        } else {
            val config = settingsRepository.getConfig()
            settingsRepository.toggleVtuber(true)
            launchVtuber(config)
            Log.i(TAG, "PNGtuber Overlay activado")
        }
    }

    fun launchVuMeter(config: RecordingConfig) {
        floatingVuMeterManager?.dismiss()
        floatingVuMeterManager = FloatingVuMeterManager(
            context = context,
            config = config,
            isMicMutedProvider = isMicMutedProvider,
            onAudioGainsChanged = { gameGain, micGain ->
                onAudioGainsChanged(gameGain, micGain)
                settingsRepository.updateGameAudioGain(gameGain)
                settingsRepository.updateMicAudioGain(micGain)
            },
            onFiltersChanged = { noiseGate, ducking ->
                onAudioFiltersChanged(noiseGate, ducking)
                settingsRepository.toggleNoiseGate(noiseGate)
                settingsRepository.toggleAudioDucking(ducking)
            },
            onMicMuteToggled = { muted ->
                onMicMuteToggled(muted)
            },
            audioLevelsProvider = audioLevelsProvider,
            onCloseClicked = {
                onVuMeterStateChanged(false)
                floatingBubbleManager?.updateVuMeterStatus(false)
                settingsRepository.toggleFloatingVuMeter(false)
            }
        ).apply {
            show()
        }
        val isShowing = floatingVuMeterManager?.isShowing == true
        onVuMeterStateChanged(isShowing)
        floatingBubbleManager?.updateVuMeterStatus(isShowing)
    }

    fun toggleVuMeter() {
        if (floatingVuMeterManager?.isShowing == true) {
            floatingVuMeterManager?.dismiss()
            floatingVuMeterManager = null
            onVuMeterStateChanged(false)
            floatingBubbleManager?.updateVuMeterStatus(false)
            settingsRepository.toggleFloatingVuMeter(false)
            Log.i(TAG, "Vúmetro Flotante desactivado")
        } else {
            val config = settingsRepository.getConfig()
            settingsRepository.toggleFloatingVuMeter(true)
            launchVuMeter(config)
            Log.i(TAG, "Vúmetro Flotante activado")
        }
    }

    fun onAudioAmplitude(amp: Float) {
        try {
            vtuberOverlayManager?.onAudioVolume(amp)
        } catch (t: Throwable) {
            // Prevenir excepciones en pipeline de audio
        }
    }

    fun dismissAll() {
        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null
        facecamOverlayManager?.dismiss()
        facecamOverlayManager = null
        touchVisualizerOverlay?.dismiss()
        touchVisualizerOverlay = null
        watermarkOverlayManager?.dismiss()
        watermarkOverlayManager = null
        sceneOverlayManager?.dismiss()
        sceneOverlayManager = null
        vtuberOverlayManager?.dismiss()
        vtuberOverlayManager = null
        floatingVuMeterManager?.dismiss()
        floatingVuMeterManager = null

        onFacecamStateChanged(false)
        onBeautyStateChanged(false)
        onRgbStateChanged(false)
        onTouchStateChanged(false)
        onWatermarkStateChanged(false)
        onSceneOverlayStateChanged(false)
        onVtuberStateChanged(false)
        onVuMeterStateChanged(false)
    }
}

