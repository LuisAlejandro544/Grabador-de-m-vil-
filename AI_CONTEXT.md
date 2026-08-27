# 🧠 Vortex Studio — Contexto de Inteligencia Artificial y Memoria del Proyecto

Este documento mantiene el estado de desarrollo, decisiones de arquitectura y mapa de componentes del proyecto **Vortex Studio**.

---

## 📌 Resumen del Proyecto

**Vortex Studio** es una aplicación Android de grabación de pantalla de alto rendimiento, suite de edición rápida y transmisión en vivo para videojuegos, orientada a creadores de contenido móvil, streamers y gamers.

---

## 🧱 Arquitectura y Módulos Clave

### 1. Módulo de Edición de Video (`com.example.editor` & `com.example.ui.editor`)
- **`VideoEditorManager.kt`**:
  * **Recorte Rápido (Lossless Stream-Copy):** Manipulación de paquetes a nivel de contenedor MP4 con `MediaExtractor` / `MediaMuxer` y fallback a NDK C++ FFmpeg.
  * **Conversor de Aspect Ratio 1-Toque:** Convierte videos a 9:16 (TikTok/Shorts), 16:9 (YouTube), 1:1, 4:5 y 4:3 con modos de fondo desenfocado (Blur), relleno (Crop) y bandas negras (Letterbox).
  * **División de Video (Split Tool):** Corta el video en 2 partes en el playhead (`Parte 1` y `Parte 2`) de forma instantánea.
  * **Extractor de Miniaturas HD:** Captura fotogramas exactos en resolución nativa mediante `MediaMetadataRetriever`.
  * **Generador de Filmstrip:** Genera miniaturas en segundo plano para la línea de tiempo.
- **`VideoEditorDialog.kt`**: Visor interactivo con Blur reactivo, selector de aspecto, botón Split y Scrubber dual.

### 2. Motor Nativo C++ (`cpp/`) & JNI
- **`vtuber_face_mesh.hpp` / `vtuber_face_mesh.cpp`**: Motor de IA local y visión artificial en C++ para tracking facial (Face Mesh 60 FPS), cálculo de apertura ocular (parpadeo independiente), apertura bucal, estimación de inclinación de cabeza (*Head Tilt / Roll*) y filtrado temporal anti-vibración.
- **`ffmpeg_engine.hpp` / `ffmpeg_engine.cpp`**: Métodos nativos para `trimVideo`, `splitVideo`, `convertAspectRatio`, `extractAudio`, `compressVideo` y `applyWatermark`.
- **`obs_compositor.hpp` / `obs_compositor.cpp`**: Pipeline de renderizado con OpenGL ES 3.0 para composición de capas y aceleración por hardware.
- **`audio_dsp_engine.hpp` / `audio_dsp_engine.cpp`**: Puerta de ruido (Noise Gate), ducking automático (-9 dB) y limitador suave (Soft Limiter).

### 3. Motor Nativo Rust (`rust/`)
- **`lib.rs` / `NativeRustNetwork.kt`**:
  * Gestión de sockets para streaming seguro en memoria RTMP/SRT.
  * Función `rustCalculateTargetDimensions` para cálculo de resoluciones y aspect ratio de video.

### 4. Overlays Flotantes, Burbuja de Control y Sincronización A/V
- `ScreenRecordService.kt`: Servicio principal en primer plano para captura mediante `MediaProjection`. Posee salvaguardas reactivas contra batería baja (`ACTION_BATTERY_LOW`), almacenamiento lleno (`ACTION_DEVICE_STORAGE_LOW`), `onTaskRemoved` y `onTrimMemory`.
- `FloatingBubbleManager.kt` & `ServiceOverlayCoordinator.kt`:
  * **Burbuja de Control Flotante:** Menú superpuesto interactivo con cronómetro en vivo, pausa, reanudar, silenciador de micrófono, disparador de Facecam y captura de pantalla.
  * **Aclaración y Modo Limpio:** Documentación en UI de ajustes recomendando desactivar la burbuja y utilizar los controles de notificación para capturar partidas completamente limpias sin elementos en pantalla.
- `ScreenCaptureEngine.kt` & `MuxerManager.kt`: Coordinación de codificadores con **Protección contra Corrupción de Archivo (Graceful Finalize)**, asegurando la escritura del átomo `moov` y cierre de pistas en contenedores MP4 con JVM Shutdown Hook.
- `Zero-Latency AV Sync Engine` (`MuxerManager.kt`, `VideoEncoderModule.kt`, `AudioEncoderWorker.kt`): Eliminación de desincronizaciones entre video y audio mediante anclaje de reloj al primer fotograma, inyección de fotogramas repetidos `KEY_REPEAT_PREVIOUS_FRAME_AFTER`, cálculo de PTS lineal continuo por muestras PCM y compensador de delay manual calibrable (`avSyncOffsetMs` de -200ms a +200ms).
- `StorageMonitorHelper.kt` & `DiskStorageMonitorCard.kt`: Monitorización de espacio en disco en tiempo real con estimación de tiempo de grabación restante según la tasa de bits.
- `ScreenshotHelper.kt` & `ImageFormatSettingsCard.kt`: Selector y motor de compresión multiformato para capturas de pantalla (PNG, JPEG 10-100% y WebP lossless).
- `FacecamOverlayManager.kt`: Cámara frontal/trasera con CameraX, FPS configurable (30-60 FPS), marco RGB animado y filtro de belleza.
- `VtuberOverlayManager.kt` & `VtuberCameraTracker.kt`: Avatar 2D / PNGtuber con IA Local en C++ (`Face Mesh`), reactividad a volumen de voz o modo híbrido, soporte de rotación de cabeza (*Head Tilt*), rebote dinámico y carga de 4 sprites PNG transparentes personalizados por el usuario.
- `FloatingVuMeterManager.kt`: Vúmetro LED en vivo y mezclador de volumen flotante con control de ganancia.
- `TouchVisualizerSettingsCard.kt`: Asistente interactivo y acceso directo a las Opciones de Desarrollador del sistema Android para activar la visualización nativa de toques y estelas de arrastre (*Show taps* a 60 FPS), garantizando compatibilidad global y captura fiel en el video final sin sobrecosto de CPU ni bloqueos en juegos.
- `WatermarkOverlayManager.kt` y `SceneOverlayManager.kt`: Marca de agua arrastrable y overlays de escena.

### 5. Flujo de Bienvenida (Onboarding) y Centro de Permisos (`com.example.ui.onboarding`)
- **`OnboardingScreen.kt`**: Orquestador visual con transiciones horizontales animadas entre pasos informativos y el centro de permisos.
- **`PermissionsSetupPage.kt`**: Centro interactivo con detección en tiempo real de permisos de Superposición, Micrófono, Cámara, Notificaciones y Almacenamiento.

### 6. Matriz de Canales de Lanzamiento Multi-Instalación y Logo Adaptativo
- **Identidad Visual Adaptativa (`VortexAppLogo.kt` & `ic_launcher`):** Sistema de logo en vórtice dinámico con tres aspas neón en espiral convergentes al centro de captura *REC*.
- **Canales Multi-Instalación:** DEV (`com.vortexstudio.recorder.dev`), CANARY (`com.vortexstudio.recorder.canary`), BETA (`com.vortexstudio.recorder.beta`), STABLE (`com.vortexstudio.recorder`).

### 7. Integración Continua y Despliegue Automatizado (CI/CD)
- `.github/workflows/build-apk.yml`: Compilación de APKs Debug con entrega a Telegram.
- `.github/workflows/build-beta-release.yml`: Flujo de **Release Beta** activado ante **Pre-releases de GitHub** o **ejecución manual (workflow_dispatch)**, inyección automática de notas desde `changelog-beta-release.md`, compilación limpia, firma de producción y entrega directa a Telegram.

---

## 🎯 Reglas de Calidad y Rendimiento
- **Burbuja Invisible en Grabación:** El jugador mantiene acceso al cronómetro y botones flotantes sin que aparezcan en el video grabado.
- **Protección Anti-Corrupción Garantizada:** Ninguna interrupción de batería, espacio o cierre de multitarea deja un archivo MP4 corrupto o ilegible.
- **60 FPS constantes:** Interfaz Jetpack Compose reactiva con `StateFlow` y sin bloqueos en el hilo principal.
- **Independencia de Google:** Sin dependencias de Play Services o Firebase; 100% autosuficiente para distribución en Uptodown, GitHub Releases y APKs independientes.
- **Licencia:** Licenciado bajo **GNU General Public License v3 (GPLv3)**.
