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
