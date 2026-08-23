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
- **`VideoEditorDialog.kt`**:
  * Visor interactivo adaptativo al aspect ratio seleccionado.
  * Barra de chips de Aspect Ratio de 1-Toque y selector de modo de ajuste Blur/Crop/Letterbox.
  * Botón directo de División (Split ✂️) con diálogo de confirmación y exportación de ambas partes.
  * Transport controls (-1s, +1s, play/pause) y deslizador dual de rango (RangeSlider In/Out).

### 2. Motor Nativo C++ (`cpp/`) & JNI
- **`ffmpeg_engine.hpp` / `ffmpeg_engine.cpp`**: Métodos nativos para `trimVideo`, `splitVideo`, `convertAspectRatio`, `extractAudio`, `compressVideo` y `applyWatermark`.
- **`obs_compositor.hpp` / `obs_compositor.cpp`**: Pipeline de renderizado con OpenGL ES 3.0 para composición de capas y aceleración por hardware.
- **`audio_dsp_engine.hpp` / `audio_dsp_engine.cpp`**: Puerta de ruido (Noise Gate), ducking automático (-9 dB) y limitador suave (Soft Limiter).

### 3. Motor Nativo Rust (`rust/`)
- **`lib.rs` / `NativeRustNetwork.kt`**:
  * Gestión de sockets para streaming seguro en memoria RTMP/SRT.
  * Función `rustCalculateTargetDimensions` para empaquetado seguro y cálculo de resoluciones de aspect ratio de video.

### 4. Overlays Flotantes y Servicios en Segundo Plano
- `ScreenRecordService.kt`: Servicio principal en primer plano para captura mediante `MediaProjection`. Posee salvaguardas reactivas contra batería baja (`ACTION_BATTERY_LOW`), almacenamiento lleno (`ACTION_DEVICE_STORAGE_LOW`), `onTaskRemoved` y `onTrimMemory`.
- `ScreenCaptureEngine.kt` & `MuxerManager.kt`: Coordinación de codificadores con **Protección contra Corrupción de Archivo (Graceful Finalize)**, asegurando la escritura del átomo `moov` y cierre de pistas en contenedores MP4 con JVM Shutdown Hook.
- `StorageMonitorHelper.kt` & `DiskStorageMonitorCard.kt`: Monitorización de espacio en disco en tiempo real con estimación de tiempo de grabación restante según la tasa de bits y alerta visual interactiva.
- `ScreenshotHelper.kt` & `ImageFormatSettingsCard.kt`: Selector y motor de compresión multiformato para capturas de pantalla (PNG sin pérdida, JPEG 10-100% configurable, WebP lossy y WebP lossless), integrado en `SettingsRepository`.
- `FacecamOverlayManager.kt`: Cámara frontal/trasera con CameraX, FPS configurable (30-60 FPS), marco RGB animado y filtro de belleza.
- `VtuberOverlayManager.kt`: Avatar 2D reactivo a voz y parpadeo ocular automático.
- `FloatingVuMeterManager.kt`: Vúmetro LED en vivo y mezclador de volumen flotante con control de ganancia.
- `TouchVisualizerOverlay.kt`: Visualizador táctil con ripples animados sin necesidad de opciones de desarrollador.
- `WatermarkOverlayManager.kt` y `SceneOverlayManager.kt`: Marca de agua arrastrable y overlays de escena.

### 5. Flujo de Bienvenida (Onboarding) y Centro de Permisos (`com.example.ui.onboarding`)
- **`OnboardingScreen.kt`**: Orquestador visual con transiciones horizontales animadas entre pasos informativos y el centro de permisos.
- **`OnboardingStepPage.kt`**: Diapositivas explicativas que detallan las ventajas gamer: Grabación a 60 FPS con Bitrate Granular, Facecam Pro RGB/Avatar VTuber con DSP, y Conversión 9:16 para TikTok/Shorts.
- **`PermissionsSetupPage.kt`**: Centro interactivo con detección en tiempo real (`onResume` / `LifecycleEventObserver`) de permisos de Superposición (`SYSTEM_ALERT_WINDOW`), Micrófono, Cámara, Notificaciones y Almacenamiento, permitiendo su concesión individual o en lote con 1 toque.
- **Persistencia Reactiva**: Gestionado por `SettingsRepository` (`KEY_ONBOARDING_COMPLETED`), propagado mediante `StateFlow` en `RecordViewModel` y con acceso directo de reapertura desde la pestaña de Ajustes.

---

## 🎯 Reglas de Calidad y Rendimiento
- **Protección Anti-Corrupción Garantizada:** Ninguna interrupción de batería, espacio o cierre de multitarea deja un archivo MP4 corrupto o ilegible.
- **60 FPS constantes:** Interfaz Jetpack Compose reactiva con `StateFlow` y sin bloqueos en el hilo principal.
- **Independencia y Cero Basura de Google:** Eliminadas dependencias obsoletas de Firebase, Auth, Play Services y Firestore. 100% autosuficiente para distribución en Uptodown, GitHub Releases y APKs de terceros.
- **Almacenamiento Público:** Los videos recortados, divididos y miniaturas se registran automáticamente en `MediaStore` / `MediaScannerConnection`.
