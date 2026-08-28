package com.example.service.capture

/**
 * Contenedor reutilizable de búfer de audio PCM de alta eficiencia (Zero-Allocation).
 * Permite reciclar matrices de bytes en pools en memoria fija sin instanciar objetos
 * en el Garbage Collector durante la grabación en tiempo real.
 */
class AudioFrameBuffer(
    val data: ByteArray = ByteArray(DEFAULT_CAPACITY),
    var size: Int = 0
) {
    companion object {
        const val DEFAULT_CAPACITY = 4096
    }
}
