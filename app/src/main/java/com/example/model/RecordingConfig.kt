package com.example.model

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    RES_1080P("1080p (Full HD)", 1920, 1080),
    RES_720P("720p (HD)", 1280, 720),
    RES_480P("480p (SD)", 854, 480);

    fun getDimensions(isPortrait: Boolean): Pair<Int, Int> {
        return if (isPortrait) {
            Pair(height, width)
        } else {
            Pair(width, height)
        }
    }
}

enum class VideoFps(val label: String, val fps: Int) {
    FPS_60("60 FPS (Fluido / Juegos)", 60),
    FPS_30("30 FPS (Estándar)", 30)
}

enum class VideoBitrate(val label: String, val bps: Int) {
    BITRATE_12M("12 Mbps (Alta Calidad)", 12_000_000),
    BITRATE_8M("8 Mbps (Recomendado)", 8_000_000),
    BITRATE_4M("4 Mbps (Ahorro de Espacio)", 4_000_000)
}

enum class AudioSourceType(val label: String) {
    INTERNAL_AND_MIC("Juego + Micrófono (Voz Dinámica)"),
    INTERNAL_GAME("Solo Audio del Juego (Interno)"),
    MIC("Solo Micrófono (Voz y Ambiente)"),
    NONE("Sin Audio (Mudo)")
}

data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val audioSource: AudioSourceType = AudioSourceType.INTERNAL_AND_MIC,
    val countdownSeconds: Int = 3,
    val isGameMode: Boolean = true,
    val showFloatingBubble: Boolean = true
)

enum class RecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    SAVING,
    ERROR
}
