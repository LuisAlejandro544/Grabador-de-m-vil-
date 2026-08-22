# 🧠 AI Context & Domain Knowledge (Vortex Studio)

Este documento provee el contexto de dominio y las restricciones técnicas fundamentales para cualquier asistente o modelo de IA que trabaje en esta base de código.

---

## 🎯 Propósito del Proyecto
Construir una suite de grabación de pantalla y streaming en vivo para Android llamada **Vortex Studio** (estilo OBS Studio), optimizada específicamente para teléfonos móviles, sesiones de videojuegos a 60 FPS, herramientas de anotación en tiempo real, efectos visuales (Filtro de Belleza, Borde RGB, Toques Táctiles) y consumo térmico eficiente.

---

## ⚠️ Reglas Críticas y Restricciones de Entorno

1. **Hardware Compartido (SoC) & Térmica:**
   - En Android, la CPU y la GPU comparten energía y disipación pasiva.
   - **PROHIBIDO:** Usar bucles intensivos de CPU para procesar píxeles en Kotlin o Python.
   - **OBLIGATORIO:** Usar `MediaRecorder` o `MediaCodec` con buffers de hardware (`Surface`), overlays transparentes nativos en `WindowManager` y shaders en C++/OpenGL ES.

2. **Prohibición de Propiedades Restringidas (`persist.sys.*`):**
   - Nunca utilizar comandos `setprop persist.sys.*` ni hacks de sistema no estándar. La aplicación debe operar mediante APIs públicas de Android estándar para compatibilidad en tiendas y tiendas de terceros como Uptodown.

3. **Arquitectura de Audio y Mezcla Dinámica con DSP C++:**
   - **`AudioSourceType.INTERNAL_AND_MIC`:** Modo por defecto con mezcla dual PCM procesada mediante motor C++ DSP (`obs::dsp::AudioDspEngine`). Captura `AudioPlaybackCapture` (juego) y `AudioRecord` (micrófono), aplicando **Noise Gate** para silenciar ruidos de ambiente, **Audio Ducking** (-9 dB en el juego cuando hablas) y **Soft Limiter** sin distorsión digital, permitiendo conmutar la voz en vivo (`Voz ON` / `Solo Juego`) sin reiniciar codificadores.
   - **`AudioSourceType.INTERNAL_GAME`:** Captura exclusiva del sonido generado por las aplicaciones y juegos.
   - **`AudioSourceType.MIC`:** Captura mediante micrófono con filtrado de ruido en C++ DSP.
   - **`AudioSourceType.NONE`:** Modo silencioso sin pista de audio.

4. **Herramientas en Vivo (Overlay Draw & Screenshot):**
   - **`ScreenDrawingOverlay`:** Dibuja directamente sobre una ventana transparente acelerada por GPU (`Canvas`/`Path`), siendo capturada de inmediato por el stream de `MediaProjection` sin requerir recodificación en C++.
   - **`ScreenshotHelper`:** Extracción de fotogramas e instantáneas guardadas en `Pictures/Screenshots` con indexación en `MediaStore`.

5. **Facecam Flotante con Filtro de Belleza y Borde RGB:**
   - **`FacecamOverlayManager`:** Despliega una vista de cámara flotante arrastrable con soporte de formas geométricas por hardware (`ViewOutlineProvider` y `GradientDrawable`): Circular 1:1, Cuadrado Redondeado, Cuadrado 1:1 y Rectangular 16:9.
   - **Filtro de Belleza:** Capa cromática y difuminado suave para un acabado facial limpio sin sobrecargar el procesador.
   - **Borde RGB:** Marco animado con gradiente circular `SweepGradient` continuo.
   - **Ciclo de vida desacoplado:** Implementa un `LifecycleOwner` personalizado para CameraX (`FacecamLifecycleOwner`) dentro del contexto de `ScreenRecordService`.

6. **Indicador de Toques Táctiles Animado (Touch Visualizer):**
   - **`TouchVisualizerOverlay`:** Dibuja ondas táctiles fluidas en tiempo real sobre toda la pantalla sin requerir permisos de desarrollador ni depuración USB.
   - **Colores Personalizables:** Azul Neón, Verde Gamer, Púrpura Neón, Rojo Fuego, Amarillo Eléctrico y Blanco Puro.

7. **Marca de Agua / Logo Personalizado Superpuesto:**
   - **`WatermarkOverlayManager`:** Superpone un logo propio o texto personal arrastrable (`WatermarkTouchHelper`) por el usuario sobre la pantalla con `FLAG_NOT_FOCUSABLE`.
   - Permite control total de transparencia, color y tamaño sin interferir en los botones del juego.

8. **Overlays de Escena Personalizados:**
   - **`SceneOverlayManager`:** Proyecta marcos gamer cyberpunk, banners inferiores de streamer, badges "🔴 LIVE" o carteles de pausa con `FLAG_NOT_TOUCHABLE`, capturados íntegramente por `MediaProjection`.

9. **Integración Nativa Segura (C++, DSP y Rust):**
   - Toda llamada a librerías nativas debe estar envuelta con protección contra `UnsatisfiedLinkError` en sus respectivos puentes (`NativeOBSBridge.kt`, `NativeAudioDSPBridge.kt`, `NativeFFmpegBridge.kt`, `NativeRustNetwork.kt`).

---

## 📊 Modelo de Datos Clave

```kotlin
data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val bitrateMbps: Int = 8,
    val audioSource: AudioSourceType = AudioSourceType.INTERNAL_AND_MIC,
    val audioSampleRate: AudioSampleRate = AudioSampleRate.RATE_48000,
    val countdownSeconds: Int = 3,
    val isGameMode: Boolean = true,
    val showFloatingBubble: Boolean = true,
    val showFacecam: Boolean = false,
    val facecamShape: FacecamShape = FacecamShape.CIRCLE,
    val facecamSize: FacecamSize = FacecamSize.MEDIUM,
    val facecamFps: FacecamFps = FacecamFps.FPS_30,
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
    val sceneOverlayImageUri: String? = null
)

data class RecordedVideo(
    val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModified: Long
)
```

---

## 🎨 Pipeline Gráfico C++ (OpenGL ES 3.0 & EGL)

- **Shaders GLSL:** Renderizado acelerado por hardware de capas ordenadas por `zOrder`.
- **Facecam Circular:** Máscara de fragmento con `smoothstep` para bordes antialiasing suaves.
- **Chroma Key GPU:** Supresión de color verde con parámetros dinámicos de similitud y suavizado sin impacto en la CPU.
- **EGL Offscreen Surface:** Permite renderizar y componer frames a 60 FPS directamente hacia los buffers de video.
