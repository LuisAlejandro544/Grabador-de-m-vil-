# 🗺️ Roadmap de Desarrollo — Vortex Studio

Este documento detalla el progreso actual y las fases de desarrollo de **Vortex Studio**.

---

## 📌 Estado de Fases

| Fase | Descripción | Estado |
| :--- | :--- | :--- |
| **Fase 1** | Captura 60 FPS, Bitrate Personalizable & Motor Base de Grabación | ✅ Completado |
| **Fase 2** | Facecam Pro (FPS, RGB, Belleza) & Avatar 2D / PNGtuber Reactivo | ✅ Completado |
| **Fase 3** | Audio DSP (Noise Gate, Ducking) & Vúmetro / Mezclador Flotante | ✅ Completado |
| **Fase 4** | Overlays de Escena, Marca de Agua y Visualizador Táctil | ✅ Completado |
| **Fase 5** | Editor de Video Avanzado (Recorte, División Split y Aspect Ratio 9:16) | ✅ Completado |
| **Fase 6** | Protección Anti-Corrupción, Monitor de Disco & Formato de Imagen Multiformato | ✅ Completado |
| **Fase 7** | Despliegue Automatizado a Telegram (CI/CD) & Transmisión en Vivo RTMP / SRT con Rust | ⏳ En Progreso / Base Lista |
| **Fase 8** | Buffer de Repetición Instantánea (Instant Replay / Clips de 30s) | 📅 Planificado |

---

## 🌟 Detalle de la Fase 6: Robustez, Almacenamiento y Compresión de Imagen (Completada)

- [x] **Formato y Compresión de Imagen Configurable (Screenshots / Capturas):**
  - Soporte de compresión seleccionable: **PNG (Sin pérdida)**, **JPEG (Calidad 10-100% personalizada)** y **WebP (Lossy / Lossless)**.
  - Componente Material 3 `ImageFormatSettingsCard` con slider de calidad %, presets rápidos (50%, 70%, 80% recomendado, 90%, 100%) y switch de WebP sin pérdida.
  - Asistente `ScreenshotHelper` actualizado con indexación automática en MediaStore y toasts informativos.
- [x] **Despliegue Automatizado a Telegram en CI/CD:**
  - Integración en `.github/workflows/build-apk.yml` de empaquetado en formato `.7z` Ultra Comprimido (LZMA2 nivel 9) y subida directa a Telegram vía Bot API (`sendDocument`).
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
