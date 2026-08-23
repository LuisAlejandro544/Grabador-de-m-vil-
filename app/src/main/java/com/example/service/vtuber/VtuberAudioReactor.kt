package com.example.service.vtuber

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Reactor de audio y temporizador biológico de parpadeo para PNGtuber / Avatares 2D.
 * Determina el estado actual ([VtuberState]) evaluando el volumen de entrada
 * y simulando parpadeos oculares naturales no periódicos.
 */
class VtuberAudioReactor(
    private val scope: CoroutineScope,
    private val onStateChanged: (VtuberState, Float) -> Unit
) {
    private var isTalking = false
    private var isBlinking = false
    private var currentAmplitude = 0f
    private var talkHoldCounter = 0
    private var blinkJob: Job? = null
    private var audioSimJob: Job? = null

    var sensitivity: Float = 0.18f

    fun start() {
        startBlinkLoop()
    }

    fun stop() {
        blinkJob?.cancel()
        audioSimJob?.cancel()
        blinkJob = null
        audioSimJob = null
    }

    /**
     * Alimenta el nivel de amplitud de audio normalizado (0.0f a 1.0f).
     * Aplicamos un filtro de "hold" (histéresis de 3 a 5 ciclos) para evitar
     * que la boca vibre violentamente en los micro-silencios entre sílabas.
     */
    fun onAudioAmplitude(amplitude: Float) {
        currentAmplitude = amplitude.coerceIn(0f, 1f)
        val threshold = (1.0f - sensitivity.coerceIn(0.01f, 0.99f)) * 0.45f

        if (currentAmplitude >= threshold) {
            isTalking = true
            talkHoldCounter = 4 // Mantener abierta durante ~200ms
        } else {
            if (talkHoldCounter > 0) {
                talkHoldCounter--
                isTalking = true
            } else {
                isTalking = false
            }
        }
        notifyState()
    }

    /**
     * Si no hay entrada de audio externa directa activa, permite simular
     * voz para pruebas y visualización previa dentro de la app.
     */
    fun startPreviewAudioPulse() {
        audioSimJob?.cancel()
        audioSimJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                // Simulación de frases naturales de 1.5s hablando y 1.5s en silencio
                val phrases = Random.nextInt(3, 8)
                for (i in 0 until phrases) {
                    val amp = Random.nextFloat() * 0.7f + 0.3f
                    onAudioAmplitude(amp)
                    delay(120)
                }
                onAudioAmplitude(0f)
                delay(Random.nextLong(1200, 3000))
            }
        }
    }

    fun stopPreviewAudioPulse() {
        audioSimJob?.cancel()
        audioSimJob = null
        onAudioAmplitude(0f)
    }

    private fun startBlinkLoop() {
        blinkJob?.cancel()
        blinkJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                // Intervalo natural entre parpadeos (2.5 a 5 segundos)
                val waitTimeMs = Random.nextLong(2500, 5000)
                delay(waitTimeMs)

                // Ojos cerrados durante ~140ms
                isBlinking = true
                notifyState()
                delay(140)

                // Posibilidad de doble parpadeo rápido (30% de probabilidad)
                isBlinking = false
                notifyState()
                if (Random.nextFloat() < 0.30f) {
                    delay(90)
                    isBlinking = true
                    notifyState()
                    delay(120)
                    isBlinking = false
                    notifyState()
                }
            }
        }
    }

    private fun notifyState() {
        val state = when {
            isTalking && isBlinking -> VtuberState.BLINKING_TALKING
            isTalking && !isBlinking -> VtuberState.TALKING
            !isTalking && isBlinking -> VtuberState.BLINKING
            else -> VtuberState.IDLE
        }
        val amp = currentAmplitude
        scope.launch(Dispatchers.Main) {
            try {
                onStateChanged(state, amp)
            } catch (t: Throwable) {
                // Prevenir caídas no deseadas en callbacks
            }
        }
    }
}
