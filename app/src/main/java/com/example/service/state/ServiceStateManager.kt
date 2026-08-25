package com.example.service.state

import com.example.model.RecordingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado y desacoplado del estado del servicio de grabación [ScreenRecordService].
 * Mantiene la reactividad del ciclo de vida, cronómetro, micrófonos y capas de overlay.
 */
object ServiceStateManager {

    private val _recordingState = MutableStateFlow(RecordingStatus.IDLE)
    val recordingState: StateFlow<RecordingStatus> = _recordingState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _countdownNumber = MutableStateFlow(0)
    val countdownNumber: StateFlow<Int> = _countdownNumber.asStateFlow()

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown: StateFlow<Boolean> = _isCountingDown.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isFacecamActive = MutableStateFlow(false)
    val isFacecamActive: StateFlow<Boolean> = _isFacecamActive.asStateFlow()

    private val _isVtuberActive = MutableStateFlow(false)
    val isVtuberActive: StateFlow<Boolean> = _isVtuberActive.asStateFlow()

    private val _isVuMeterActive = MutableStateFlow(false)
    val isVuMeterActive: StateFlow<Boolean> = _isVuMeterActive.asStateFlow()

    private val _isBeautyActive = MutableStateFlow(false)
    val isBeautyActive: StateFlow<Boolean> = _isBeautyActive.asStateFlow()

    private val _isRgbActive = MutableStateFlow(false)
    val isRgbActive: StateFlow<Boolean> = _isRgbActive.asStateFlow()

    private val _isTouchActive = MutableStateFlow(false)
    val isTouchActive: StateFlow<Boolean> = _isTouchActive.asStateFlow()

    private val _isWatermarkActive = MutableStateFlow(false)
    val isWatermarkActive: StateFlow<Boolean> = _isWatermarkActive.asStateFlow()

    private val _isSceneOverlayActive = MutableStateFlow(false)
    val isSceneOverlayActive: StateFlow<Boolean> = _isSceneOverlayActive.asStateFlow()

    private val _lastSavedFilePath = MutableStateFlow<String?>(null)
    val lastSavedFilePath: StateFlow<String?> = _lastSavedFilePath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Acceso mutable para el cronómetro y componentes internos
    val elapsedSecondsMutableFlow: MutableStateFlow<Int>
        get() = _elapsedSeconds

    fun isRecording(): Boolean {
        val state = _recordingState.value
        return state == RecordingStatus.RECORDING || state == RecordingStatus.PAUSED
    }

    fun setRecordingState(status: RecordingStatus) {
        _recordingState.value = status
    }

    fun setElapsedSeconds(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    fun setCountdown(number: Int, isCounting: Boolean) {
        _countdownNumber.value = number
        _isCountingDown.value = isCounting
    }

    fun setMicMuted(muted: Boolean) {
        _isMicMuted.value = muted
    }

    fun setFacecamActive(active: Boolean) {
        _isFacecamActive.value = active
    }

    fun setVtuberActive(active: Boolean) {
        _isVtuberActive.value = active
    }

    fun setVuMeterActive(active: Boolean) {
        _isVuMeterActive.value = active
    }

    fun setBeautyActive(active: Boolean) {
        _isBeautyActive.value = active
    }

    fun setRgbActive(active: Boolean) {
        _isRgbActive.value = active
    }

    fun setTouchActive(active: Boolean) {
        _isTouchActive.value = active
    }

    fun setWatermarkActive(active: Boolean) {
        _isWatermarkActive.value = active
    }

    fun setSceneOverlayActive(active: Boolean) {
        _isSceneOverlayActive.value = active
    }

    fun setLastSavedFilePath(path: String?) {
        _lastSavedFilePath.value = path
    }

    fun setErrorMessage(msg: String?) {
        _errorMessage.value = msg
    }

    fun reset() {
        _recordingState.value = RecordingStatus.IDLE
        _elapsedSeconds.value = 0
        _countdownNumber.value = 0
        _isCountingDown.value = false
        _isMicMuted.value = false
        _isFacecamActive.value = false
        _isVtuberActive.value = false
        _isVuMeterActive.value = false
        _isBeautyActive.value = false
        _isRgbActive.value = false
        _isTouchActive.value = false
        _isWatermarkActive.value = false
        _isSceneOverlayActive.value = false
    }
}
