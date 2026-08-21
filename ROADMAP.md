# 🗺️ Roadmap: De Grabador de Pantalla a OBS Studio para Android

Este documento detalla las fases de evolución del proyecto para transformar la aplicación en una suite completa de producción de video y transmisión en vivo móvil estilo OBS Studio.

---

## 📍 Fase 1: Motor Base y Grabación Móvil (Completada ✅)
- [x] Captura de pantalla nativa con `MediaProjection` y `VirtualDisplay`.
- [x] Configuración de resolución (1080p, 720p, 480p), FPS (60 / 30) y tasa de bits (12M, 8M, 4M).
- [x] Servicio en primer plano (`ScreenRecordService`) con notificación persistente para control durante el juego.
- [x] Lanzador de juegos integrado y galería con reproductor interno.
- [x] Cimientos nativos: Módulos C++ (NDK/CMake) y Rust (Cargo/JNI) configurados con carga segura.

---

## 📍 Fase 2: Control de Audio y Perfiles (Completada ✅)
- [x] Selector de fuente de audio:
  - **Solo Audio del Juego (Interno):** Captura exclusiva del sonido de aplicaciones sin voz externa.
  - **Micrófono:** Captura de voz y ambiente.
  - **Mudo:** Video puro.
- [x] Perfil "Modo Juego" con activación rápida de 60 FPS y 12 Mbps.
- [x] Temporizador de cuenta atrás previo al inicio de grabación.

---

## 📍 Fase 3: Composición de Escenas en C++ (Próxima 🔜)
- [ ] **Facecam Flotante / Cámara Frontal:** Renderizar la cámara delantera en una ventana circular o rectangular sobre el juego.
- [ ] **Motor Gráfico C++ con OpenGL ES 3.0:**
  - Mezcla de texturas en tiempo real (`SurfaceTexture` + shaders GLSL).
  - Soporte de múltiples capas (*z-order*): Fondo, Juego, Facecam, Marco/PNG, Texto.
- [ ] **Editor Visual de Escenas en Compose:**
  - Arrastrar, redimensionar y posicionar la cámara y elementos en un lienzo previo antes de grabar.

---

## 📍 Fase 4: Transmisión en Vivo con Motor Rust (RTMP / SRT)
- [ ] **Integración del cliente RTMP en Rust:**
  - Handshake y conexión con plataformas de streaming (Twitch, YouTube Gaming, Kick, Servidores Personalizados).
- [ ] **Codificación por Hardware `MediaCodec` + Pipe a Rust:**
  - Extracción de paquetes NAL H.264/AAC y empaquetado seguro en memoria sin pasar por el Garbage Collector de Java.
- [ ] **Bitrate Adaptativo Dinámico (ABR):**
  - Ajuste automático de calidad según la estabilidad del Wi-Fi / red 5G para evitar cortes en el directo.

---

## 📍 Fase 5: Overlays y Widgets Interactivos
- [ ] **Widgets de Chat en Tiempo Real:** Overlay transparente para leer mensajes del chat de Twitch/YouTube sobre la pantalla del juego.
- [ ] **Alertas de Eventos:** Donaciones, seguidores y suscripciones mediante WebViews ligeras o texturas transparentes.
- [ ] **Buffer de Repetición (Replay Buffer):** Guardar los últimos 30-60 segundos de jugadas destacadas con un botón flotante.
