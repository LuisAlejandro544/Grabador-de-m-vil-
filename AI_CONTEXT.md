# 🧠 AI Context & Domain Knowledge (OBS Mobile)

Este documento provee el contexto de dominio y las restricciones técnicas fundamentales para cualquier asistente o modelo de IA que trabaje en esta base de código.

---

## 🎯 Propósito del Proyecto
Construir una suite de grabación de pantalla y streaming en vivo para Android equivalente a **OBS Studio**, optimizada específicamente para teléfonos móviles, sesiones de videojuegos a 60 FPS y consumo térmico eficiente.

---

## ⚠️ Reglas Críticas y Restricciones de Entorno

1. **Hardware Compartido (SoC) & Térmica:**
   - En Android, la CPU y la GPU comparten energía y disipación pasiva.
   - **PROHIBIDO:** Usar bucles intensivos de CPU para procesar píxeles en Kotlin o Python.
   - **OBLIGATORIO:** Usar `MediaRecorder` o `MediaCodec` con buffers de hardware (`Surface`) y shaders en C++/OpenGL ES.

2. **Prohibición de Propiedades Restringidas (`persist.sys.*`):**
   - Nunca utilizar comandos `setprop persist.sys.*` ni hacks de sistema no estándar. La aplicación debe operar mediante APIs públicas de Android estándar para compatibilidad en tiendas y tiendas de terceros como Uptodown.

3. **Arquitectura de Audio:**
   - **`AudioSourceType.INTERNAL_GAME`:** Captura exclusiva del sonido generado por las aplicaciones y juegos.
   - **`AudioSourceType.MIC`:** Captura mediante micrófono con soporte de audio estéreo y supresión de eco si está disponible.
   - **`AudioSourceType.NONE`:** Modo silencioso sin pista de audio.

4. **Integración Nativa Segura (C++ y Rust):**
   - Toda llamada a librerías nativas debe estar envuelta con protección contra `UnsatisfiedLinkError` en sus respectivos puentes (`NativeOBSBridge.kt`, `NativeRustNetwork.kt`).
   - Esto asegura que el frontend en Compose funcione fluidamente incluso en entornos donde las bibliotecas nativas se compilan por separado.

---

## 📊 Modelo de Datos Clave

```kotlin
data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val audioSource: AudioSourceType = AudioSourceType.INTERNAL_GAME,
    val countdownSeconds: Int = 3,
    val isGameMode: Boolean = true
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
