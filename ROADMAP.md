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
| **Fase 9** | Transmisión en Vivo RTMP / SRT con Rust & Buffer de Repetición (Clips 30s) | ⏳ En Progreso / Base Lista |

---

## 🌟 Detalle de la Fase 8: Matriz de 4 Canales de Distribución (Completada)

- [x] **Arquitectura de 4 Canales Multi-Instalación:**
  - `DEV` (`com.vortexstudio.recorder.dev` | `0.1.0-dev` | `1000`): Compilación debug y herramientas de diagnóstico.
  - `CANARY` (`com.vortexstudio.recorder.canary` | `0.1.0-canary.1` | `1001`): Canal experimental para recopilar feedback de la comunidad.
  - `BETA` (`com.vortexstudio.recorder.beta` | `0.1.0-beta.1` | `1002`): Versión candidata para pruebas de compatibilidad y 60 FPS en juegos.
  - `STABLE` (`com.vortexstudio.recorder` | `0.1.0` | `1003`): Versión oficial para tiendas de APKs (Uptodown, GitHub Releases).
- [x] **Identidad Visual Adaptativa (Opción 5: «Vórtice Dinámico Adaptativo»):**
  - Icono adaptativo del sistema (`adaptive-icon`) y composable vectorial `VortexAppLogo` que adapta sus tonos de neón y vórtice según el canal activo (Púrpura Dev, Naranja Canary, Azul Beta, Verde Estable).
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
  - Tarjeta de acceso en Ajustes para reabrir el tutorial y revisar permisos en cualquier momento.
- [x] **Limpieza y Desacoplamiento de Servicios de Google:**
  - Eliminación de dependencias innecesarias de Firebase, Auth, Firestore, Datastore y Play Services.
  - Compatibilidad total para distribución directa en tiendas de terceros como Uptodown y APK independiente.

---

## 🌟 Detalle de la Fase 6: Robustez, Almacenamiento y Compresión de Imagen (Completada)

- [x] **Formato y Compresión de Imagen Configurable (Screenshots / Capturas):**
  - Soporte de compresión seleccionable: **PNG (Sin pérdida)**, **JPEG (Calidad 10-100% personalizada)** y **WebP (Lossy / Lossless)**.
  - Componente Material 3 `ImageFormatSettingsCard` con slider de calidad %, presets rápidos (50%, 70%, 80% recomendado, 90%, 100%) y switch de WebP sin pérdida.
  - Asistente `ScreenshotHelper` actualizado con indexación automática en MediaStore y toasts informativos.
- [x] **Despliegue Automatizado a Telegram y GitHub Releases en CI/CD:**
  - Integración en `.github/workflows/build-apk.yml` de empaquetado en formato `.7z` Ultra Comprimido (LZMA2 nivel 9) y subida directa a Telegram vía Bot API (`sendDocument`).
  - Nuevo workflow `.github/workflows/build-beta-release.yml` para **Pre-releases de GitHub** (`on.release.types: [prereleased]`) con inyección automática de `changelog-beta-release.md` en el cuerpo del release, compilación limpia (Clean Build sin caché), firma Release de producción, subida directa de archivos `.apk` sin comprimir a la release y entrega a Telegram.
  - Reducción drástica del tamaño de descarga para conexiones móviles lentas con reporte de tamaño original vs comprimido y guía de instalación.
  - Capacidad de transferencia de hasta 2 GB con notificación formateada (archivo, commit, fecha).
- [x] **Protección contra Corrupción de Archivo (Graceful Finalize):**
  - Cierre seguro y finalización del átomo `moov` en `MediaMuxer`.
  - Receptores de eventos del sistema para `ACTION_BATTERY_LOW` y `ACTION_DEVICE_STORAGE_LOW`.
  - Manejo de `onTaskRemoved` (cierre de app en recientes) y JVM Shutdown Hook preventivo.
- [x] **Monitor de Espacio en Disco en Tiempo Real:**
  - Estimación dinámica del tiempo restante de grabación en horas/minutos según la tasa de bits (bitrate).
  - Componente Material 3 `DiskStorageMonitorCard` con barra de progreso por colores y alertas visuales.
  - Salvaguarda automática periódica durante la captura activa si se alcanza el umbral de emergencia (150 MB).

---

## 🌟 Detalle de la Fase 5: Suite de Edición de Video (Completada)

- [x] **Recorte Rápido sin Renderizado (Stream Copy):**
  - Recorte a nivel de contenedor MP4 con `MediaExtractor` / `MediaMuxer` y C++ NDK FFmpeg.
  - Velocidad instantánea en milisegundos sin recompresión ni consumo excesivo de batería.
- [x] **Conversor de Aspect Ratio con 1 Toque:**
  - Conversión instantánea a **9:16 (TikTok, Shorts, Reels)**, **16:9 (YouTube)**, **1:1 (Feed)**, **4:5 (Portrait)** y **4:3 (Classic)**.
  - Modos de encuadre con **Desenfoque Blur de fondo cinemático**, **Llenado completo (Crop)** y **Barras negras (Letterbox)**.
- [x] **Herramienta de División (Split Tool):**
  - Corte del video en el cursor de reproducción (*Playhead*), generando `Parte 1` y `Parte 2` independientes.
- [x] **Extractor de Miniaturas HD:**
  - Extracción de fotogramas exactos en 1080p/4K en formato JPEG de alta fidelidad.
- [x] **Línea de Tiempo Interactiva:**
  - Filmstrip dinámico de fotogramas con doble cursor táctil deslizable y sincronización de reproducción en tiempo real.
