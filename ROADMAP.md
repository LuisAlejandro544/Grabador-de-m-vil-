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

## 📍 Fase 2: Control de Audio, Perfiles y Widget Flotante con Herramientas (Completada ✅)
- [x] Selector, mezcla y procesamiento de audio pro:
  - **Motor DSP en C++ Nativo:** Puerta de ruido (Noise Gate), Audio Ducking automático inteligente (-9 dB) y Soft Limiter / Saturation shaper anti-clipping.
  - **Juego + Micrófono Conmutables (Dinámico):** Mezcla dual PCM en tiempo real con conmutador en vivo (`Voz ON` / `Solo Juego`) sin reiniciar la grabación.
  - **Solo Audio del Juego (Interno):** Captura exclusiva del sonido de aplicaciones sin voz externa.
  - **Micrófono:** Captura de voz y ambiente con filtrado DSP en caliente.
  - **Mudo:** Video puro.
- [x] Perfil "Modo Juego" con activación rápida de 60 FPS y 12 Mbps.
- [x] Temporizador de cuenta atrás previo al inicio de grabación.
- [x] **Widget Flotante Arrastrable (Burbuja en Pantalla):** Control en vivo con cronómetro, conmutador de voz/juego, pausa, reanudación y parada sobre cualquier juego vía `WindowManager`.
- [x] **Menú de Herramientas en Vivo:**
  - **Captura de Pantalla Instantánea:** Guardado directo en `Pictures/Screenshots` y registro en `MediaStore`.
  - **Pincel / Lapicero en Pantalla:** Modo de dibujo interactivo con selector de colores, grosores y borrado rápido sobre la grabación activa.

---

## 📍 Fase 3: Composición de Escenas, Facecam y Efectos Visuales (Completada ✅)
- [x] **Facecam Flotante con Diseños Geométricos (CameraX & WindowManager):**
  - Superposición flotante arrastrable con cámara frontal y trasera en tiempo real.
  - Diseños configurables con clip nativo: Circular 1:1, Cuadrado redondeado, Cuadrado y Rectangular 16:9.
  - Control de activación/desactivación dinámica desde la burbuja flotante del grabador y persistencia de estado.
- [x] **Filtro de Belleza Facial y Suavizado de Piel:**
  - Capa de filtro cosmético con balance cromático y reducción de asperezas faciales.
  - Conmutable en Ajustes, en el Facecam y desde el menú rápido de la burbuja.
- [x] **Borde RGB / Arcoíris Animado para Facecam:**
  - Marco con gradiente rotativo continuo (`SweepGradient`) acelerado por hardware.
  - Conmutador en vivo en Ajustes y en el submenú de herramientas de la burbuja.
- [x] **Indicador de Toques Táctiles Animado (Touch Visualizer):**
  - Ondas de feedback táctil sobre pantalla completa sin necesidad de habilitar opciones de desarrollador.
  - 6 variantes de color neón/gamer configurables en Ajustes y conmutable en caliente desde la burbuja flotante.
- [x] **Motor Gráfico C++ con OpenGL ES 3.0 & EGL:**
  - Contexto EGL nativo y renderizado acelerado por GPU a 60 FPS.
  - Soporte de múltiples capas (*z-order*): Juego, Facecam, Overlays, Texto.
  - Shaders GLSL de vértice y fragmento con mezcla alfa y soporte para recortes de forma.
  - **Máscara Circular para Facecam:** Recorte circular con suavizado antialiasing (`smoothstep`) para la cámara frontal.
  - **Filtro Chroma Key en GPU:** Eliminación de fondo verde en tiempo real con tolerancia y suavizado configurable.
  - Métricas de rendimiento en tiempo real (FPS de renderizado del motor y tiempo de frame en ms).

---

## 📍 Fase 4: Transmisión en Vivo con Motor Rust (RTMP / SRT)
- [ ] **Integración del cliente RTMP en Rust:**
  - Handshake y conexión con plataformas de streaming (Twitch, YouTube Gaming, Kick, Servidores Personalizados).
- [ ] **Codificación por Hardware `MediaCodec` + Pipe a Rust:**
  - Extracción de paquetes NAL H.264/AAC y empaquetado seguro en memoria sin pasar por el Garbage Collector de Java.
- [ ] **Bitrate Adaptativo Dinámico (ABR):**
  - Ajuste automático de calidad según la estabilidad del Wi-Fi / red 5G para evitar cortes en el directo.

---

## 📍 Fase 5: Motor de Edición y Post-Producción con FFmpeg Puro Nativo (C/C++)
- [x] **Cimientos y Arquitectura de FFmpeg Nativo (`libav*`):**
  - Estructura C++ (`ffmpeg_engine.hpp` / `ffmpeg_engine.cpp`) sin librerías descontinuadas ni wrappers externos.
  - Puente JNI `NativeFFmpegBridge` con métodos de recorte, extracción de audio y compresión.
- [ ] **Recorte Rápido de Video (*Stream Copy & Frame Accurate*):**
  - Recorte instantáneo sin pérdida ni renderizado para clips rápidos.
- [ ] **Extracción y Procesamiento de Audio:**
  - Separación de pistas de voz/juego y exportación directa a MP3/AAC.
- [ ] **Conversión para Redes (Shorts / TikTok / Reels):**
  - Adaptación inteligente de formato 16:9 a 9:16 con desenfoque de fondo.

---

## 📍 Fase 6: Overlays y Widgets Interactivos
- [ ] **Widgets de Chat en Tiempo Real:** Overlay transparente para leer mensajes del chat de Twitch/YouTube sobre la pantalla del juego.
- [ ] **Alertas de Eventos:** Donaciones, seguidores y suscripciones mediante WebViews ligeras o texturas transparentes.
- [ ] **Buffer de Repetición (Replay Buffer):** Guardar los últimos 30-60 segundos de jugadas destacadas con un botón flotante.
