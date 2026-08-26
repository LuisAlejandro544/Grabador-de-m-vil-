package com.example.model

enum class VideoResolution(val label: String, val baseWidth: Int, val baseHeight: Int) {
    RES_NATIVE("Nativa (Pantalla Completa)", 0, 0),
    RES_1080P("1080p (Full HD Adaptativo)", 1080, 1920),
    RES_720P("720p (HD Adaptativo)", 720, 1280),
    RES_480P("480p (SD Adaptativo)", 480, 854);

    val width: Int get() = if (baseWidth > 0) baseWidth else 1080
    val height: Int get() = if (baseHeight > 0) baseHeight else 1920

    fun getDimensions(isPortrait: Boolean): Pair<Int, Int> {
        return if (isPortrait) {
            Pair(width, height)
        } else {
            Pair(height, width)
        }
    }

    /**
     * Calcula dimensiones proporcionales exactas a la pantalla física del dispositivo
     * eliminando bordes negros (pillarbox / letterbox) y alineando a múltiplos de 16 para MediaCodec.
     */
    fun getAdaptiveDimensions(screenWidth: Int, screenHeight: Int, isPortrait: Boolean): Pair<Int, Int> {
        val realShort = minOf(screenWidth, screenHeight).coerceAtLeast(480)
        val realLong = maxOf(screenWidth, screenHeight).coerceAtLeast(854)
        val aspectRatio = realLong.toDouble() / realShort.toDouble()

        val targetShort = when (this) {
            RES_NATIVE -> realShort
            RES_1080P -> minOf(1080, realShort)
            RES_720P -> minOf(720, realShort)
            RES_480P -> minOf(480, realShort)
        }

        val targetLong = if (this == RES_NATIVE) {
            realLong
        } else {
            kotlin.math.round(targetShort * aspectRatio).toInt()
        }

        // Alinear a múltiplos de 16 para compatibilidad universal con hardware AVC/H.264
        val finalShort = ((targetShort + 15) / 16) * 16
        val finalLong = ((targetLong + 15) / 16) * 16

        return if (isPortrait) {
            Pair(finalShort, finalLong)
        } else {
            Pair(finalLong, finalShort)
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

enum class FacecamFps(val label: String, val fps: Int) {
    FPS_30("30 FPS (Estándar / Fluido)", 30),
    FPS_45("45 FPS (Dinámico)", 45),
    FPS_50("50 FPS (PAL / Alta Tasa)", 50),
    FPS_60("60 FPS (Ultra Pro)", 60)
}

enum class AudioSampleRate(val label: String, val sampleRate: Int) {
    RATE_32000("32.0 kHz (Baja Latencia / Ahorro)", 32000),
    RATE_44100("44.1 kHz (CD Audio / Música)", 44100),
    RATE_48000("48.0 kHz (Estudio Pro / Broadcast)", 48000),
    RATE_96000("96.0 kHz (Hi-Res Master)", 96000)
}

enum class AudioSourceType(val label: String) {
    INTERNAL_AND_MIC("Juego + Micrófono (Voz Dinámica)"),
    INTERNAL_GAME("Solo Audio del Juego (Interno)"),
    MIC("Solo Micrófono (Voz y Ambiente)"),
    NONE("Sin Audio (Mudo)")
}

enum class FacecamShape(val label: String) {
    CIRCLE("Circular (1:1)"),
    ROUNDED_SQUARE("Cuadrado Redondeado"),
    SQUARE("Cuadrado"),
    RECTANGLE("Rectangular (16:9)")
}

enum class FacecamSize(val label: String, val dpSize: Int) {
    SMALL("Pequeño (100 dp)", 100),
    MEDIUM("Mediano (140 dp)", 140),
    LARGE("Grande (180 dp)", 180)
}

enum class TouchColorOption(val label: String, val hexColor: String, val primaryArgb: Long) {
    CYAN("Azul Neón", "#0284C7", 0xFF0284C7),
    GREEN("Verde Gamer", "#10B981", 0xFF10B981),
    PURPLE("Púrpura Neón", "#8B5CF6", 0xFF8B5CF6),
    RED("Rojo Fuego", "#EF4444", 0xFFEF4444),
    AMBER("Amarillo Eléctrico", "#F59E0B", 0xFFF59E0B),
    WHITE("Blanco Puro", "#FFFFFF", 0xFFFFFFFF);

    val colorInt: Int get() = primaryArgb.toInt()
}

enum class WatermarkType(val label: String) {
    TEXT("Texto Personalizado"),
    IMAGE("Logo / Imagen PNG")
}

enum class WatermarkSize(val label: String, val textSizeSp: Float, val iconSizeDp: Int) {
    SMALL("Pequeño", 13f, 42),
    MEDIUM("Mediano", 17f, 60),
    LARGE("Grande", 22f, 84)
}

enum class SceneOverlayType(val label: String, val description: String) {
    NONE("Ninguno", "Sin marco superpuesto"),
    GAMER_NEON_FRAME("Marco Neón Gamer", "Biseles ciberpunk en bordes de pantalla"),
    STREAMER_BANNER("Banner de Redes Sociales", "Barra inferior con nombre y redes"),
    LIVE_BADGE("Badge '🔴 EN VIVO'", "Insignia de streaming en esquina superior"),
    STANDBY_PAUSE("Cartel 'Volvemos en Breve'", "Alerta translúcida para pausas"),
    CUSTOM_IMAGE("Marco PNG Personalizado", "Superposición de imagen propia con transparencia")
}

enum class VtuberPreset(val label: String, val description: String) {
    CYBER_CAT("Gato Ciberpunk", "Gatito mecha con audífonos gamer neón"),
    ANIME_AOI("Aoi Chibi", "Personaje anime con coletas y lazo"),
    PIXEL_SLIME("Slime Gamer", "Slime verde retro con corona dorada"),
    CUSTOM("Avatar Personalizado", "Tus propios archivos PNG transparentes")
}

enum class VtuberTrackingMode(val label: String, val description: String) {
    VOICE_ONLY("Solo Voz (Micrófono)", "El avatar reacciona únicamente al volumen del micrófono"),
    FACE_MESH_LOCAL("IA Local: Seguimiento Facial (Cámara)", "Detecta ojos, parpadeo, boca y rotación de cabeza con IA local 100% offline"),
    HYBRID("Modo Híbrido (Cámara + Micrófono)", "Combina expresiones faciales visuales con amplificación de voz")
}

enum class VtuberSize(val label: String, val dpSize: Int) {
    SMALL("Pequeño (110 dp)", 110),
    MEDIUM("Mediano (150 dp)", 150),
    LARGE("Grande (200 dp)", 200)
}

enum class ImageFormatOption(val label: String, val extension: String, val description: String) {
    PNG("PNG", "png", "Sin pérdida de calidad (Máxima nitidez, mayor tamaño)"),
    JPEG("JPG / JPEG", "jpg", "Compresión estándar (Ahorro de espacio configurable)"),
    WEBP("WebP", "webp", "Formato moderno y ligero (Ultra eficiente en tamaño)")
}

enum class TouchAvatarGenre(val label: String, val shortDesc: String, val iconEmoji: String) {
    RHYTHM_4K("Juegos de Ritmo / 4 Teclas", "Teclado reactivo con 4 flechas de colores (← ↓ ↑ →)", "🎵"),
    SHOOTER_FPS("Shooter / Battle Royale", "Joystick táctil izquierdo + Gatillo y apuntado derecho", "🎯"),
    FIGHTING_ACTION("Lucha / Arcade / Acción", "D-Pad direccional + 4 Botones de combo (A/B/X/Y)", "🥊"),
    CASUAL_TAP("Casual / Táctil Libre", "Manos reactivas con pulsaciones y gestos libres", "⚡")
}

enum class TouchAvatarSize(val label: String, val dpWidth: Int, val dpHeight: Int) {
    COMPACT("Compacto", 140, 100),
    MEDIUM("Estándar", 180, 130),
    LARGE("Amplio", 230, 165)
}

data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val bitrateMbps: Int = 8,
    val facecamFps: FacecamFps = FacecamFps.FPS_30,
    val audioSampleRate: AudioSampleRate = AudioSampleRate.RATE_48000,
    val audioSource: AudioSourceType = AudioSourceType.INTERNAL_AND_MIC,
    val countdownSeconds: Int = 3,
    val isGameMode: Boolean = true,
    val showFloatingBubble: Boolean = true,
    val showFacecam: Boolean = false,
    val facecamShape: FacecamShape = FacecamShape.CIRCLE,
    val facecamSize: FacecamSize = FacecamSize.MEDIUM,
    val isFrontCamera: Boolean = true,
    val beautyFilterEnabled: Boolean = false,
    val facecamRgbBorder: Boolean = false,
    val showTouchVisualizer: Boolean = false,
    val touchVisualizerColor: TouchColorOption = TouchColorOption.CYAN,
    val showWatermark: Boolean = false,
    val watermarkType: WatermarkType = WatermarkType.TEXT,
    val watermarkText: String = "🌪️ Vortex Studio",
    val watermarkOpacity: Float = 0.85f,
    val watermarkSize: WatermarkSize = WatermarkSize.MEDIUM,
    val watermarkColor: TouchColorOption = TouchColorOption.CYAN,
    val watermarkCustomImageUri: String? = null,
    val showSceneOverlay: Boolean = false,
    val sceneOverlayType: SceneOverlayType = SceneOverlayType.GAMER_NEON_FRAME,
    val sceneOverlayText: String = "🔴 EN VIVO | @TuCanal",
    val sceneOverlayOpacity: Float = 0.90f,
    val sceneOverlayImageUri: String? = null,
    val showVtuber: Boolean = false,
    val vtuberPreset: VtuberPreset = VtuberPreset.CYBER_CAT,
    val vtuberTrackingMode: VtuberTrackingMode = VtuberTrackingMode.VOICE_ONLY,
    val vtuberSize: VtuberSize = VtuberSize.MEDIUM,
    val vtuberSensitivity: Float = 0.18f,
    val vtuberBounceEnabled: Boolean = true,
    val vtuberHeadTiltEnabled: Boolean = true,
    val vtuberEyeBlinkSensitivity: Float = 0.35f,
    val vtuberMouthSensitivity: Float = 0.40f,
    val vtuberIdleImageUri: String? = null,
    val vtuberTalkImageUri: String? = null,
    val vtuberBlinkImageUri: String? = null,
    val vtuberBlinkTalkImageUri: String? = null,
    val showTouchAvatar: Boolean = false,
    val touchAvatarGenre: TouchAvatarGenre = TouchAvatarGenre.RHYTHM_4K,
    val touchAvatarSize: TouchAvatarSize = TouchAvatarSize.MEDIUM,
    val touchAvatarOpacity: Float = 0.95f,
    val touchAvatarVoiceSync: Boolean = true,
    val touchAvatarCustomImageUri: String? = null,
    val gameAudioGain: Float = 1.0f,
    val micAudioGain: Float = 1.25f,
    val audioDuckingEnabled: Boolean = true,
    val noiseGateEnabled: Boolean = true,
    val showFloatingVuMeter: Boolean = false,
    val avSyncOffsetMs: Int = 0,
    val imageFormat: ImageFormatOption = ImageFormatOption.PNG,
    val imageQuality: Int = 80,
    val imageWebpLossless: Boolean = false
) {
    fun getEffectiveBitrateBps(): Int = bitrateMbps * 1_000_000
}

enum class RecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    SAVING,
    ERROR
}

