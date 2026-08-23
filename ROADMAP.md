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
| **Fase 6** | Transmisión en Vivo RTMP / SRT (Twitch, YouTube, Kick) con Rust | ⏳ En Progreso / Base Lista |
| **Fase 7** | Buffer de Repetición Instantánea (Instant Replay / Clips de 30s) | 📅 Planificado |

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
