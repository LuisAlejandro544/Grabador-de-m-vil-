# 🌪️ Vortex Studio & Game Recorder

> **Vortex Studio** es una suite completa de grabación de pantalla de alto rendimiento, edición rápida y transmisión en vivo para Android, diseñada para capturar partidas de videojuegos a 60 FPS con audio interno exclusivo, overlays visuales, herramientas en vivo, **Burbuja Flotante con Control de Cronómetro y Menú Rápido**, **Aclaración y Modo de Grabación 100% Limpio desde Notificaciones**, **Filtro de Belleza facial**, **Borde RGB Arcoíris animado**, **Indicador de Toques Táctiles animado**, **Editor de Video con Conversor de Aspect Ratio 1-Toque y División de Clips**, y cimientos nativos en **C++ (OpenGL ES 3.0 / FFmpeg Core)** y **Rust (RTMP/SRT)**.

---

## 🚀 Características Principales

### 🕹️ Burbuja de Control Flotante & Grabaciones Limpias
- **⚡ Control Rápido en Pantalla:** Acceso instantáneo a cronómetro en vivo, pausa, reanudación, silenciador de micrófono, conmutador de fuentes de audio, activación de cámara Facecam o avatar VTuber y capturas con 1 toque sobre cualquier videojuego.
- **✨ Modo de Grabación 100% Limpio:** Guía integrada en los ajustes para realizar grabaciones de partidas sin ningún elemento superpuesto, controlando el inicio, pausa y finalización cómodamente desde la barra de notificaciones persistente del sistema.

### ✂️ Editor de Video Móvil Avanzado (Vortex Studio Pro)
- **📱 Conversión de Relación de Aspecto con 1 Toque (1-Tap Aspect Ratio Converter):**
  - Adapta cualquier video horizontal o de gameplay a formato vertical para **TikTok, YouTube Shorts e Instagram Reels (9:16)**, **YouTube / TV (16:9)**, **Instagram Feed (1:1)**, **Retrato Vertical (4:5)** o **Clásico (4:3)**.
  - **Modos de Encuadre Inteligentes:**
    * **Desenfoque Blur Cinemático:** Mantiene el video centrado con sus proporciones intactas sobre un fondo ampliado desenfocado en tiempo real (estilo gaming profesional).
    * **Llenar Pantalla (Crop Fill):** Recorte central sin bandas para ocupar la totalidad del lienzo.
    * **Barras Negras (Letterbox):** Encuadre limpio con bandas negras superiores/inferiores o laterales.
- **✂️ Herramienta de División de Clips (Split Tool):**
  - Corta y separa el video en 2 partes independientes en la posición exacta del cabezal de reproducción (*Playhead*).
  - Genera automáticamente `Parte 1` (desde el inicio al corte) y `Parte 2` (desde el corte al final) mediante **Lossless Stream-Copy** en milisegundos sin renderizado ni pérdida de calidad.
- **⚡ Recorte Rápido sin Renderizado (Lossless Stream-Copy):**
  - Recorta segmentos específicos usando manipulación de contenedores MP4 con `MediaExtractor` / `MediaMuxer` y puente NDK C++ FFmpeg sin degradar los 60 FPS ni recalentar el teléfono.
- **📸 Extractor de Miniaturas en HD (Thumbnail Grabber):**
  - Captura fotogramas exactos en resolución nativa (1080p / 4K) y los guarda como JPEG de máxima calidad en `Pictures/ScreenRecorder` para portadas de YouTube y TikTok.
- **🎞️ Línea de Tiempo Visual Interactiva:**
  - Tira dinámica de fotogramas (*Filmstrip*) generada en segundo plano con marcadores táctiles de entrada/salida (*Dual RangeSlider*) y cursor de tiempo en tiempo real.

### 🎮 Grabación y Rendimiento Gamer
- **🖼️ Selector y Compresor de Imagen (PNG / JPG % / WebP):**
  - Configura el formato exacto de las capturas de pantalla tomadas durante el juego o desde el menú de la app (PNG sin pérdida, JPEG 10%-100% y WebP lossless).
- **🛡️ Protección contra Corrupción de Archivo (Graceful Finalize):**
  - Cierre seguro del contenedor MP4 y escritura garantizada del átomo `moov` ante batería baja, falta de almacenamiento, cierre de la app desde multitarea (`onTaskRemoved`) o presión extrema de memoria RAM.
  - Salvaguarda mediante JVM Shutdown Hook para evitar la pérdida o corrupción de videos de partidas largas.
- **💾 Monitor de Espacio en Disco en Tiempo Real:**
  - Tarjeta de almacenamiento con barra de progreso reactiva en colores (Verde / Ámbar / Rojo).
  - Cálculo dinámico de horas/minutos restantes de grabación según el bitrate de video configurado.
  - Salvaguarda automática preventiva si el espacio restante desciende del umbral de seguridad (150 MB).
- **🎮 Modo Optimizado para Juegos:** Configuración instantánea a 1080p Full HD, 60 FPS y 12 Mbps con un solo toque.
- **⚡ Tasa de Bits Personalizada (1 - 12 Mbps):** Control milimétrico de bitrate mediante un deslizador interactivo con saltos enteros y botones de acceso rápido.
- **✨ Filtro de Belleza & Suavizado de Piel:** Capa de post-procesado facial que atenúa imperfecciones, suaviza texturas y balancea la luminosidad del rostro.
- **🌈 Borde RGB / Arcoíris Gamer Animado:** Marco con gradiente `SweepGradient` rotativo continuo que bordea la ventana flotante del Facecam adaptándose automáticamente a su forma.
- **📹 Facecam / Cámara Flotante con FPS Configurable (30 - 60 FPS):** Superposición de cámara en tiempo real mediante **CameraX** y `WindowManager` sin cortes de fluidez.
- **🎭 Avatar 2D / PNGtuber con IA Local (Face Mesh NDK C++ & Reactivo a Voz):**
  - **100% On-Device & Offline:** Motor de visión por IA desarrollado en C++ nativo sin conexión a servidores externos ni envío de datos privados.
  - **Seguimiento Facial en Tiempo Real:** Detección de parpadeo de ojos independientes, apertura de boca y estimación de pose de cabeza (*Head Tilt / Roll angle*) a 60 FPS con suavizado anti-vibración exponencial.
  - **Modos Flexibles (`VtuberTrackingMode`):** Solo Voz (Micrófono), Seguimiento Facial Completo (Cámara + IA Local) y Modo Híbrido (Visión + Audio para máxima expresividad).
  - **Presets y Personalización:** Presets vectoriales de alta calidad (Cyber Cat, Mecha Robot, Chibi Gamer, etc.) o carga de 4 estados PNG transparentes con rebote dinámico *squash & stretch*.
- **🐱 Avatar VTuber Reactivo a Toques Táctiles (Handcam Bongo Cat por Género de Juego):**
  - **Reacción Táctil en Vivo sin Carga de Assets:** Dibujo procedural 100% en vector/Canvas (0 MB de peso añadido al APK) que mueve sus patitas, brazos o dedos al pulsar sobre la pantalla.
  - **Adaptación por Género de Videojuego (Sin Marcas Hardcodeadas):**
    * **Juegos de Ritmo / 4 Teclas:** Teclado táctil reactivo con teclas D-F-J-K iluminadas dinámicamente y patas que presionan con precisión rítmica.
    * **Shooter / FPS / Battle Royale:** Joystick táctil en mano izquierda y gatillo/botón de disparo reactivo en mano derecha.
    * **Arcade / Lucha / Plataformas:** D-Pad cruz direccional izquierda y botones A-B en la derecha.
    * **Casual / Táctil Libre:** Patitas reactivas directas al punto de contacto.
  - **Interceptación Táctil Global No Invasiva:** Detección transparente mediante `WindowManager` con `FLAG_WATCH_OUTSIDE_TOUCH` sin bloquear ni restar sensibilidad a los controles del juego.
  - **Sincronización Opcional por Micrófono:** Abre la boca y reacciona cuando hablas mientras juegas.
  - **Personalización Flexible:** Tamaño configurable (Pequeño, Mediano, Grande), opacidad ajustable y selector para cargar avatares PNG personalizados locales.
- **👆 Indicador de Toques Táctiles Animado (Touch Visualizer):** Ondas y ripples dinámicos de alta respuesta que se renderizan sobre la pantalla mediante overlay `WindowManager` transparente.
- **🏷️ Marca de Agua / Logo Personalizado:** Superpone tu marca personal, tag de streamer o logo en pantalla con posición libre y opacidad ajustable.
- **🖼️ Overlays de Escena Personalizados:** Marcos Neón Cyberpunk, Banners de Redes Sociales inferiores, Badges animados "🔴 EN VIVO" y carteles de pausa.

### 🔊 Sistema de Audio Profesional y DSP
- **🔄 Calibración y Sincronización A/V con Cero Desfase (Zero-Latency Sync Engine):**
  - Motor de sincronización exacta entre pistas de video H.264 y audio AAC anclado al reloj de inicio.
  - Eliminación de jitter y desfase mediante cálculo lineal y continuo de PTS por recuento de muestras PCM.
  - Control de compensación manual configurable (-200 ms a +200 ms) en los ajustes de audio.
- **📊 Vúmetro de Audio Flotante & Mezclador en Vivo:** Widget superpuesto interactivo con barras LED dinámicas de decibelios (dB) con gradiente verde-amarillo-rojo.
- **🎛️ Motor Nativo de Audio DSP (C++):** Procesamiento estéreo con **Noise Gate**, **Audio Ducking Automático** y **Soft Limiter**.
- **🎙️ Conmutador Dinámico de Voz en Vivo:** Conmuta con un toque si deseas grabar tu voz (`Voz ON`) o dejar únicamente el sonido limpio del juego (`Solo Juego`).

---

## 🛠️ Arquitectura Técnica

```
vortex-studio/
├── app/src/main/
│   ├── java/com/example/
│   │   ├── editor/                  # Motor de edición y transcodificación de video
│   │   │   └── VideoEditorManager.kt # Stream-Copy, Aspect Ratio Converter, Split y Thumbnail Grabber
│   │   ├── ui/
│   │   │   ├── editor/              # Interfaz de usuario del editor avanzado
│   │   │   │   └── VideoEditorDialog.kt # Aspect ratio chips, monitor adaptativo, split y scrubber
│   │   │   ├── tabs/                # Tabs principales (RecordTab, GalleryTab)
│   │   │   └── components/          # Componentes Jetpack Compose (settings, cards, controls)
│   │   ├── service/                 # Servicios en segundo plano, Facecam, VTuber AI Tracker y overlays
│   │   └── nativecore/              # Puentes JNI con módulos C++ y Rust (NativeVTuberFaceBridge)
│   ├── cpp/                         # Motor C++ NDK (Face Mesh AI, FFmpeg Core, OBS Compositor, Audio DSP)
│   └── rust/                        # Motor Rust (RTMP/SRT Streaming y cálculo de aspect ratio)
```

---

## 🎨 Identidad Visual y Logo: «Vórtice Dinámico Adaptativo» (Multi-Canal)

Vortex Studio utiliza un diseño de logo basado en un vórtice geométrico con tres aspas neón en espiral convergentes hacia un punto central de captura (*REC*). El icono es adaptativo a nivel de sistema (`adaptive-icon`) y a nivel de interfaz Jetpack Compose (`VortexAppLogo.kt`), adaptando su paleta de colores reactivamente según el canal de la aplicación instalada:
- 🟣 **Canal Dev:** Vórtice en púrpura eléctrico y magenta neón (`#9C27B0` / `#E040FB`).
- 🟠 **Canal Canary:** Vórtice en naranja ámbar y amarillo cálido (`#FF9800` / `#FFD54F`).
- 🔵 **Canal Beta:** Vórtice en azul zafiro y cian brillante (`#2196F3` / `#00E5FF`).
- 🟢 **Canal Estable:** Vórtice en verde esmeralda y menta fluorescente (`#4CAF50` / `#69F0AE`).

---

## 🏷️ Canales de Lanzamiento y Distribución Multi-Instalación (v0.1.0)

Vortex Studio cuenta con una arquitectura de 4 canales de distribución con `applicationId` independientes, permitiendo tener instaladas las 4 versiones al mismo tiempo en el teléfono:

| Canal | Nombre Visible | `applicationId` (Package) | Versión (`versionName`) | `versionCode` | Propósito |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 🟣 **Desarrollador (Dev)** | `Vortex (Dev)` | `com.vortexstudio.recorder.dev` | `0.1.0-dev` | `1000` | Exclusivo para compilación debug local, logs detallados e inspección NDK. |
| 🟠 **Canary (Lab / Exp.)** | `Vortex Canary` | `com.vortexstudio.recorder.canary` | `0.1.0-canary.1` | `1001` | Funciones experimentales tempranas para recopilar feedback de la comunidad. |
| 🔵 **Beta (RC)** | `Vortex Beta` | `com.vortexstudio.recorder.beta` | `0.1.0-beta.1` | `1002` | Versión candidata para pruebas de rendimiento en juegos y compatibilidad. |
| 🟢 **Estable (Release)** | `Vortex Studio` | `com.vortexstudio.recorder` | `0.1.0` | `1003` | Versión oficial probada para tiendas de APKs (Uptodown, GitHub Releases). |

---

## 📦 Distribución y Compatibilidad
- **100% Autónomo y Autosuficiente:** No depende de Google Play Services ni servicios propietarios, ideal para distribución directa en APK, Uptodown o tiendas de terceros.
- **Jetpack Compose + Material Design 3:** Interfaz fluida a 60 FPS.
- **Android 8.0 (API 26) o superior.**

---

## 🤖 CI/CD y Despliegue Automatizado (Debug y Beta Release)

El proyecto cuenta con dos flujos de trabajo de GitHub Actions automatizados:
1. **Compilación Debug (`.github/workflows/build-apk.yml`):** Compilación rápida para desarrollo con caché opcional y entrega comprimida a Telegram.
2. **Compilación Beta Release Firmada (`.github/workflows/build-beta-release.yml`):** Activación ante **Pre-releases de GitHub** (`on.release.types: [prereleased]`) o **ejecución manual (workflow_dispatch)** para generar el APK Release sin crear una release formal. Cuenta con inyección automática de `changelog-beta-release.md`, compilación limpia (Clean Build), firma de producción y entrega directa a Telegram.

---

## 🤝 Contribuciones
Por el momento no estamos aceptando Pull Requests (PRs) mientras estabilizamos la arquitectura base del proyecto, pero eres completamente libre de hacer **forks** y experimentar. Consulta [CONTRIBUTING.md](CONTRIBUTING.md) para más detalles.

---

## 📄 Licencia
Este proyecto está distribuido y protegido bajo los términos de la **Licencia Pública General de GNU v3 (GPLv3)**. Consulta el archivo [LICENSE](LICENSE) para ver el texto legal completo.
