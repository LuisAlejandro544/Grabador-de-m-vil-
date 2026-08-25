# 🗺️ Roadmap de Desarrollo — Vortex Studio

Este documento detalla el progreso actual y las fases de desarrollo de **Vortex Studio**.

---

## 📌 Estado de Fases

| Fase | Descripción | Estado |
| :--- | :--- | :--- |
| **Fase 1** | Captura 60 FPS, Bitrate Personalizable & Motor Base de Grabación | ✅ Completado |
| **Fase 2** | Facecam Pro (FPS, RGB, Belleza) & Avatar 2D / PNGtuber Reactivo | ✅ Completado |
| **Fase 3** | Audio DSP (Noise Gate, Ducking), Sincronización A/V & Vúmetro / Mezclador Flotante | ✅ Completado |
| **Fase 4** | Overlays de Escena, Marca de Agua y Visualizador Táctil | ✅ Completado |
| **Fase 5** | Editor de Video Avanzado (Recorte, División Split y Aspect Ratio 9:16) | ✅ Completado |
| **Fase 6** | Protección Anti-Corrupción, Monitor de Disco & Formato de Imagen Multiformato | ✅ Completado |
| **Fase 7** | Flujo de Onboarding, Centro de Permisos & Limpieza de Dependencias Google | ✅ Completado |
| **Fase 8** | Matriz de 4 Canales (Dev, Canary, Beta, Estable) & Despliegue CI/CD a Telegram | ✅ Completado |
| **Fase 9** | Burbuja Flotante con Exclusión en Grabación (Android 14+ y Versiones Anteriores) | ✅ Completado |
| **Fase 10** | Seguimiento Facial por IA Local (Face Mesh NDK C++) para Avatar VTuber | ✅ Completado |
| **Fase 11** | Transmisión en Vivo RTMP / SRT con Rust & Buffer de Repetición (Clips 30s) | ⏳ En Progreso / Base Lista |

---

## 🌟 Detalle de la Fase 10: Seguimiento Facial por IA Local (Face Mesh NDK C++) (Completada)

- [x] **Motor de Visión por Computador e IA On-Device (`vtuber_face_mesh.hpp` / `.cpp`):**
  - Procesamiento nativo en C++ de alta velocidad a 60 FPS con extracción de landmarks faciales.
  - Normalización geométrica de apertura de ojos (parpadeo independiente izquierdo/derecho), apertura bucal y cálculo de ángulo de giro/inclinación de cabeza (*Head Tilt / Roll*).
  - Filtro exponencial anti-vibración (*Temporal Smoothing*) para evitar micro-temblores.
- [x] **Pipeline de Captura con CameraX & NDK JNI:**
  - Analizador de imágenes `VtuberCameraTracker.kt` operando sobre `ImageAnalysis` (YUV) en un ejecutor background dedicado.
  - Puente JNI `NativeVTuberFaceBridge.kt` para transporte eficiente de coordenadas y pose.
- [x] **Modos de Seguimiento y Calibración en Tiempo Real:**
  - Selector en ajustes `VtuberTrackingMode` con opciones: Solo Voz, IA Local Seguimiento Facial y Modo Híbrido.
  - Calibración interactiva de sensibilidad de parpadeo, sensibilidad de boca y switch de inclinación de cabeza.
  - Renderizado de rotación física en canvas sobre la ventana flotante `VtuberOverlayView`.
- [x] **100% Offline & Privacidad Garantizada:**
  - Cero telemetría, sin modelos pesados ni conexión a servidores externos. Operación totalmente autosuficiente para tiendas de APKs alternativas.

---

## 🌟 Detalle de la Fase 9: Burbuja Flotante con Exclusión en Grabación (Completada)

- [x] **Modo Ocultar Burbuja en el Video Final:**
  - Opción conmutable en ajustes para que el jugador visualice y controle la grabación desde la burbuja flotante, pero esta quede excluida del archivo MP4 final.
  - **Android 14+ (API 34+):** Exclusión nativa de ventanas de superposición (`FLAG_SECURE` / exclusión de `VirtualDisplay`).
  - **Versiones Anteriores a Android 14:** Atenuación y minimización automática durante la captura activa para no obstruir el metraje del juego.
- [x] **Integración Integral en la Arquitectura:**
  - Modelo `RecordingConfig.hideBubbleInFinalVideo`, persistencia reactiva en `OverlaySettingsStore` / `SettingsRepository`, propagación en `RecordViewModel` y control visual en `FloatingBubbleSettingsCard` y `SettingsView`.
  - Integración en el ciclo de vida del servicio: `ScreenRecordService`, `ServiceOverlayCoordinator`, `FloatingBubbleManager` y `ServiceActionDispatcher`.

---

## 🌟 Detalle de la Fase 8: Matriz de 4 Canales de Distribución (Completada)

- [x] **Arquitectura de 4 Canales Multi-Instalación:**
  - `DEV` (`com.vortexstudio.recorder.dev` | `0.1.0-dev` | `1000`): Compilación debug y herramientas de diagnóstico.
  - `CANARY` (`com.vortexstudio.recorder.canary` | `0.1.0-canary.1` | `1001`): Canal experimental para recopilar feedback de la comunidad.
  - `BETA` (`com.vortexstudio.recorder.beta` | `0.1.0-beta.1` | `1002`): Versión candidata para pruebas de compatibilidad y 60 FPS en juegos.
  - `STABLE` (`com.vortexstudio.recorder` | `0.1.0` | `1003`): Versión oficial para tiendas de APKs (Uptodown, GitHub Releases).
- [x] **Identidad Visual Adaptativa (Opción 5: «Vórtice Dinámico Adaptativo»):**
  - Icono adaptativo del sistema (`adaptive-icon`) y composable vectorial `VortexAppLogo` que adapta sus tonos de neón y vórtice según el canal activo.
- [x] **Visualización en UI y Ajustes:**
  - Tarjeta `ReleaseChannelInfoCard` en los ajustes y badge identificador reactivo en la barra superior `RecordTopBar`.
- [x] **Despliegue Automatizado a Telegram en CI/CD:**
  - Empaquetado `.7z` con compresión ultra LZMA2 (Nivel 9) y entrega directa a Telegram sin depender de Google Play.

---

## 🌟 Detalle de la Fase 7: Experiencia de Primer Inicio y Autonomía (Completada)

- [x] **Flujo de Bienvenida Interactivo (Onboarding):**
  - Diapositivas explicativas con animación horizontal (`OnboardingScreen`, `OnboardingStepPage`): Grabación 60 FPS fluida, Facecam Pro RGB/Avatar VTuber con DSP y Editor 9:16 para redes.
- [x] **Centro Unificado de Permisos (`PermissionsSetupPage`):**
  - Detección en tiempo real de permisos del sistema: Superposición sobre otras apps (`SYSTEM_ALERT_WINDOW`), Micrófono, Cámara, Notificaciones y Almacenamiento.
  - Botón de concesión unificada en lote ("⚡ Conceder Todos los Permisos") y enlace directo a ajustes del sistema.
- [x] **Limpieza y Desacoplamiento de Servicios de Google:**
  - Eliminación de dependencias innecesarias de Firebase, Auth, Firestore, Datastore y Play Services.
  - Compatibilidad total para distribución directa en tiendas de terceros como Uptodown y APK independiente.

---

## 🌟 Detalle de la Fase 6: Robustez, Almacenamiento y Compresión de Imagen (Completada)

- [x] **Formato y Compresión de Imagen Configurable:**
  - Soporte de compresión seleccionable: PNG (Sin pérdida), JPEG (10-100%) y WebP (Lossy / Lossless).
- [x] **Protección contra Corrupción de Archivo (Graceful Finalize):**
  - Cierre seguro y finalización del átomo `moov` en `MediaMuxer`.
  - Receptores de eventos del sistema para `ACTION_BATTERY_LOW` y `ACTION_DEVICE_STORAGE_LOW`.
  - Manejo de `onTaskRemoved` (cierre de app en recientes) y JVM Shutdown Hook preventivo.
- [x] **Monitor de Espacio en Disco en Tiempo Real:**
  - Estimación dinámica del tiempo restante de grabación en horas/minutos según la tasa de bits (bitrate).
  - Componente Material 3 `DiskStorageMonitorCard` con barra de progreso por colores y alertas visuales.

---

## 🌟 Detalle de la Fase 5: Suite de Edición de Video (Completada)

- [x] **Recorte Rápido sin Renderizado (Stream Copy):** Recorte a nivel de contenedor MP4 con `MediaExtractor` / `MediaMuxer` y C++ NDK FFmpeg.
- [x] **Conversor de Aspect Ratio con 1 Toque:** Conversión instantánea a 9:16, 16:9, 1:1, 4:5 y 4:3 con Blur cinemático, Crop y Letterbox.
- [x] **Herramienta de División (Split Tool):** Corte del video en el cursor de reproducción (*Playhead*).
- [x] **Extractor de Miniaturas HD:** Extracción de fotogramas exactos en 1080p/4K en formato JPEG.
