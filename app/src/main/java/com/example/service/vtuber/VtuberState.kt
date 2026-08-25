package com.example.service.vtuber

/**
 * Estados canónicos de animación de un avatar 2D Reactivo / PNGtuber.
 * Representa la matriz clásica 2x2 (Ojos: Abiertos/Cerrados x Boca: Cerrada/Abierta).
 */
enum class VtuberState(val label: String) {
    IDLE("En reposo (Ojos abiertos, boca cerrada)"),
    TALKING("Hablando (Ojos abiertos, boca abierta)"),
    BLINKING("Parpadeando (Ojos cerrados, boca cerrada)"),
    BLINKING_TALKING("Hablando y Parpadeando (Ojos cerrados, boca abierta)")
}

/**
 * Expresión facial y postura 3D calculada por el motor de IA local / Seguimiento Facial para VTubers.
 */
data class VtuberFacePose(
    val state: VtuberState = VtuberState.IDLE,
    val headRoll: Float = 0f,       // Inclinación lateral (-30 a +30 deg)
    val headPitch: Float = 0f,      // Inclinación arriba/abajo (-25 a +25 deg)
    val headYaw: Float = 0f,        // Giro horizontal (-35 a +35 deg)
    val leftEyeOpenness: Float = 1.0f,
    val rightEyeOpenness: Float = 1.0f,
    val mouthOpenness: Float = 0.0f,
    val smileRatio: Float = 0.0f,
    val isFaceDetected: Boolean = false
) {
    val isBlinking: Boolean get() = leftEyeOpenness < 0.35f && rightEyeOpenness < 0.35f
    val isTalking: Boolean get() = mouthOpenness > 0.35f
    val isWinkingLeft: Boolean get() = leftEyeOpenness < 0.35f && rightEyeOpenness >= 0.35f
    val isWinkingRight: Boolean get() = rightEyeOpenness < 0.35f && leftEyeOpenness >= 0.35f

    fun toCanonicalState(): VtuberState {
        return when {
            isBlinking && isTalking -> VtuberState.BLINKING_TALKING
            isBlinking -> VtuberState.BLINKING
            isTalking -> VtuberState.TALKING
            else -> VtuberState.IDLE
        }
    }
}

