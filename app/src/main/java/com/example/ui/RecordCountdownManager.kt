package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor modular de la cuenta atrás y respuesta háptica antes de iniciar la grabación.
 */
class RecordCountdownManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _countdownNumber = MutableStateFlow(0)
    val countdownNumber = _countdownNumber.asStateFlow()

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown = _isCountingDown.asStateFlow()

    private var countdownJob: Job? = null

    fun startCountdown(seconds: Int, onFinish: () -> Unit) {
        if (seconds <= 0) {
            _isCountingDown.value = false
            _countdownNumber.value = 0
            onFinish()
            return
        }

        countdownJob?.cancel()
        _isCountingDown.value = true
        countdownJob = scope.launch {
            for (i in seconds downTo 1) {
                _countdownNumber.value = i
                vibrateQuick()
                delay(1000)
            }
            _isCountingDown.value = false
            _countdownNumber.value = 0
            onFinish()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _isCountingDown.value = false
        _countdownNumber.value = 0
    }

    private fun vibrateQuick() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }
}
