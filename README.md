# 🌪️ Vortex Studio & Game Recorder

> **Vortex Studio** es una suite completa de grabación de pantalla de alto rendimiento, edición rápida y transmisión en vivo para Android, diseñada para capturar partidas de videojuegos a 60 FPS con audio interno exclusivo, overlays visuales, herramientas en vivo, **Filtro de Belleza facial**, **Borde RGB Arcoíris animado**, **Indicador de Toques Táctiles animado**, **Editor de Video con Conversor de Aspect Ratio 1-Toque y División de Clips**, y cimientos nativos en **C++ (OpenGL ES 3.0 / FFmpeg Core)** y **Rust (RTMP/SRT)**.

---

## 🚀 Características Principales

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
  - Configura el formato exacto de las capturas de pantalla tomadas durante el juego o desde el menú de la app.
  - **PNG:** Calidad máxima sin pérdida fotograma a fotograma.
  - **JPEG Personalizable (10% - 100%):** Ahorra hasta un 70% de espacio eligiendo compresiones equilibradas (como 80% recomendado).
  - **WebP:** Formato de última generación con soporte para compresión con pérdida y compresión pura sin pérdida (*WebP Lossless*).
- **🛡️ Protección contra Corrupción de Archivo (Graceful Finalize):**
  - Cierre seguro del contenedor MP4 y escritura garantizada del átomo `moov` ante batería baja, falta de almacenamiento, cierre de la app desde multitarea (`onTaskRemoved`) o presión extrema de memoria RAM.
  - Salvaguarda mediante JVM Shutdown Hook para evitar la pérdida o corrupción de videos de partidas largas.
- **💾 Monitor de Espacio en Disco en Tiempo Real:**
  - Tarjeta de almacenamiento con barra de progreso reactiva en colores (Verde / Ámbar / Rojo).
  - Cálculo dinámico de horas/minutos restantes de grabación según el bitrate de video configurado.
  - Salvaguarda automática preventiva si el espacio restante desciende del umbral de seguridad (150 MB).
- **🎮 Modo Optimizado para Juegos:** Configuración instantánea a 1080p Full HD, 60 FPS y 12 Mbps con un solo toque.
- **⚡ Tasa de Bits Personalizada (1 - 12 Mbps):** Control milimétrico de bitrate mediante un deslizador interactivo con saltos enteros y botones de acceso rápido (1M, 2M, 4M, 6M, 8M, 10M, 12M).
- **✨ Filtro de Belleza & Suavizado de Piel:** Capa de post-procesado facial que atenúa imperfecciones, suaviza texturas y balancea la luminosidad del rostro.
- **🌈 Borde RGB / Arcoíris Gamer Animado:** Marco con gradiente `SweepGradient` rotativo continuo que bordea la ventana flotante del Facecam adaptándose automáticamente a su forma.
- **📹 Facecam / Cámara Flotante con FPS Configurable (30 - 60 FPS):** Superposición de cámara en tiempo real mediante **CameraX** y `WindowManager` sin cortes de fluidez.
- **🎭 Avatar 2D / PNGtuber Reactivo por Voz:** Alternativa ligera y privada a la cámara real: sustituye el Facecam por un avatar 2D animado interactivo que reacciona al volumen RMS del micrófono y parpadea de forma natural.
- **👆 Indicador de Toques Táctiles Animado (Touch Visualizer):** Ondas y ripples dinámicos de alta respuesta que se renderizan sobre la pantalla mediante overlay `WindowManager` transparente sin requerir opciones de desarrollador.
- **🏷️ Marca de Agua / Logo Personalizado:** Superpone tu marca personal, tag de streamer o logo en pantalla con posición libre (arrastrable con el dedo) y opacidad ajustable.
- **🖼️ Overlays de Escena Personalizados:** Marcos Neón Cyberpunk, Banners de Redes Sociales inferiores, Badges animados "🔴 EN VIVO" y carteles de pausa.

### 🔊 Sistema de Audio Profesional y DSP
- **📊 Vúmetro de Audio Flotante & Mezclador en Vivo:** Widget superpuesto interactivo con barras LED dinámicas de decibelios (dB) con gradiente verde-amarillo-rojo.
- **🎛️ Motor Nativo de Audio DSP (C++):** Procesamiento estéreo con **Noise Gate** (puerta de ruido para respiración y ventiladores), **Audio Ducking Automático** (baja el juego -9 dB cuando hablas) y **Soft Limiter** para evitar distorsión digital.
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
│   │   ├── service/                 # Servicios en segundo plano y overlays flotantes
│   │   └── nativecore/              # Puentes JNI con módulos C++ y Rust
│   ├── cpp/                         # Motor C++ NDK (FFmpeg Core, OBS Compositor, Audio DSP)
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

## 🤖 CI/CD y Despliegue Automatizado a Telegram (Ultra Comprimido .7z)

El proyecto cuenta con un workflow de GitHub Actions (`.github/workflows/build-apk.yml`) que compila de forma nativa todo el código C++ (NDK), Rust (Cargo NDK) y Kotlin, empaqueta el APK en un archivo `.7z` con **Compresión Ultra LZMA2 (Nivel 9)** para ahorrar hasta 50-70% de datos en la descarga móvil, y lo envía directamente a un chat o canal de **Telegram** (con capacidad de hasta 2 GB por archivo).

### 🔑 Secretos requeridos en el repositorio de GitHub:
Para activar el envío a Telegram, añade las siguientes variables en **Settings -> Secrets and variables -> Actions**:
| Secreto | Descripción | Ejemplo |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Token de acceso de tu Bot de Telegram creado con [@BotFather](https://t.me/BotFather) | `123456789:ABCdefGhIJKlmNoPQRstuVWXyz` |
| `TELEGRAM_CHAT_ID` | ID numérico de tu chat privado, grupo o canal donde se entregará el APK | `987654321` o `-100123456789` |

### 📱 Cómo instalar desde tu teléfono:
1. Descarga el archivo `Vortex-Studio-Debug-UltraCompressed.7z` adjunto en tu chat de Telegram.
2. Ábrelo con **ZArchiver** (o el explorador de archivos integrado de tu teléfono).
3. Extrae e instala el archivo APK con 1 toque.

---

## 🤝 Contribuciones
Por el momento no estamos aceptando Pull Requests (PRs) mientras estabilizamos la arquitectura base del proyecto, pero eres completamente libre de hacer **forks** y experimentar. Consulta [CONTRIBUTING.md](CONTRIBUTING.md) para más detalles.

---

## 📄 Licencia
Este proyecto está distribuido y protegido bajo los términos de la **Licencia Pública General de GNU v3 (GPLv3)**. Consulta el archivo [LICENSE](LICENSE) para ver el texto legal completo.


