# 🧠 AI Context & Domain Knowledge (OBS Mobile)

Este documento provee el contexto de dominio y las restricciones técnicas fundamentales para cualquier asistente o modelo de IA que trabaje en esta base de código.

---

## 🎯 Propósito del Proyecto
Construir una suite de grabación de pantalla y streaming en vivo para Android equivalente a **OBS Studio**, optimizada específicamente para teléfonos móviles, sesiones de videojuegos a 60 FPS, herramientas de anotación en tiempo real y consumo térmico eficiente.

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

5. **Integración Nativa Segura (C++, DSP y Rust):**
   - Toda llamada a librerías nativas debe estar envuelta con protección contra `UnsatisfiedLinkError` en sus respectivos puentes (`NativeOBSBridge.kt`, `NativeAudioDSPBridge.kt`, `NativeFFmpegBridge.kt`, `NativeRustNetwork.kt`).
   - Esto asegura que el frontend en Compose funcione fluidamente incluso en entornos donde las bibliotecas nativas se compilan por separado.

---

## 📊 Modelo de Datos Clave

```kotlin
data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val audioSource: AudioSourceType = AudioSourceType.INTERNAL_AND_MIC,
    val countdownSeconds: Int = 3,
    val isGameMode: Boolean = true,
    val showFloatingBubble: Boolean = true
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

---

## 🎬 Motor de Edición FFmpeg Puro Nativo (C/C++ libav*)

- **Sin Wrappers Descontinuados:** Implementación directa sobre librerías C nativas de FFmpeg (`libavcodec`, `libavformat`, `libavfilter`, `libswscale`).
- **Recorte Stream Copy:** Permite recortar videos de forma instantánea al no recodificar los fotogramas (`fast copy`).
- **Seguridad en NDK:** Encapsulado en `ffmpeg_engine.hpp` / `ffmpeg_engine.cpp` y exportado vía JNI en `NativeFFmpegBridge.kt`.
