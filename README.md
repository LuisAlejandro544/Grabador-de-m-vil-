# OBS Mobile & Game Recorder

> **OBS Mobile** es una plataforma de grabación de pantalla de alto rendimiento y transmisión en vivo para Android, diseñada para capturar partidas de videojuegos a 60 FPS con audio interno exclusivo, overlays visuales, herramientas en vivo (captura de pantalla y pincel/anotaciones en tiempo real) y cimientos nativos en **C++ (OpenGL ES 3.0)** y **Rust (RTMP/SRT)**.

---

## 🚀 Características Principales

- **🎮 Modo Optimizado para Juegos:** Configuración instantánea a 1080p Full HD, 60 FPS y 12 Mbps con un solo toque.
- **🔊 Captura, Mezcla y Procesamiento de Audio Pro en C++ (DSP):**
  - **🎛️ Motor Nativo de Audio DSP (C++):** Procesamiento de señales a 48 kHz estéreo con **Noise Gate** (puerta de ruido para atenuar respiración y ventiladores), **Audio Ducking Automático** (atenúa el juego -9 dB cuando hablas para dar prioridad a tu voz) y **Soft Limiter / Saturation Shaper** para prevenir distorsión y saturación digital al mezclar.
  - **🎙️ Conmutador Dinámico de Voz en Vivo (Exclusivo):** Graba el audio del juego y conmuta con un toque desde la burbuja flotante o la notificación si deseas grabar tu voz (`Voz ON`) o dejar únicamente el sonido limpio del juego (`Solo Juego`), mezclando audio PCM en tiempo real sin cortar el video ni reiniciar codificadores.
  - **Solo Audio del Juego (Interno):** Graba música y efectos del juego sin capturar tu voz ni ruidos del entorno.
  - **Micrófono:** Captura tu voz y comentarios en vivo para tutoriales y gameplays comentados con reducción de ruido en caliente.
  - **Mudo:** Graba únicamente el video para optimizar espacio de almacenamiento.
- **⚡ Control en Segundo Plano & Burbuja Flotante con Herramientas:** 
  - Servicio persistente con notificación interactiva y widget flotante superpuesto en pantalla (`WindowManager`) con cronómetro en vivo, botón de conmutación rápida de voz/juego, pausa, reanudación y parada directa sin salir del juego.
  - **🛠️ Menú de Herramientas en Vivo:**
    - 📸 **Captura Rápida (Screenshot):** Captura instantáneas en alta calidad durante la grabación y las guarda en la galería de imágenes del dispositivo.
    - ✏️ **Pincel / Lapicero en Pantalla:** Lienzo transparente acelerado por hardware para dibujar marcas, flechas y notas sobre cualquier juego en tiempo real con selector de colores, grosores, deshacer y limpiar trazos.
- **🕹️ Lanzador de Juegos Rápido:** Detección de juegos y aplicaciones instaladas; permite iniciar la grabación y abrir el juego simultáneamente.
- **🎬 Galería y Reproductor Integrado:** Gestión de archivos grabados (MP4 H.264 / AAC), reproducción instantánea, renombrado y compartir.
- **⚙️ Motor Gráfico Nativo C++ (OpenGL ES 3.0 / EGL):**
  - Composición de escenas multicapa con ordenamiento de profundidad (*z-order*).
  - Máscara circular con suavizado (*antialiasing*) para Facecam de la cámara frontal.
  - Filtro de eliminación de fondo verde (*Chroma Key*) acelerado por GPU vía fragment shaders.
  - Monitoreo en tiempo real de FPS de renderizado y tiempo por fotograma (ms).
- **⚙️ Motor FFmpeg Puro Nativo (C/C++ NDK / libav*):**
  - Cimientos nativos directos de `libavcodec`, `libavformat`, `libavfilter` y `libswscale` para edición precisa al fotograma, recorte sin recodificar, extracción de audio y compresión sin dependencias obsoletas ni descontinuadas.
- **⚙️ Motor de Red Nativo Rust (Cargo / JNI):**
  - Arquitectura de red segura y concurrente para transmisión RTMP/SRT con bitrate adaptativo.

---

## 🏗️ Stack Tecnológico

| Capa | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Frontend / UI** | Kotlin + Jetpack Compose (Material 3) | Interfaz moderna, reactiva y adaptativa para móviles |
| **Arquitectura** | MVVM + Coroutines + StateFlow | Estado unidireccional y reactividad desacoplada |
| **Captura de Pantalla** | Android `MediaProjection` + `MediaRecorder` | Captura acelerada por hardware de pantalla y audio |
| **Herramientas en Vivo** | `WindowManager` + Hardware Canvas Overlay | Pincel táctil en pantalla y capturas de pantalla instantáneas |
| **Motor Gráfico** | C++ (OpenGL ES 3.0 / EGL) | Composición de capas: pantalla, facecam, overlays |
| **Audio DSP Pro** | C++ Nativo (NDK DSP / Oboe) | Puerta de ruido (Noise Gate), Audio Ducking y Soft Limiter |
| **Motor de Edición** | FFmpeg Puro C/C++ (`libav*`) | Recorte de video, transcodificación, filtros y compresión |
| **Motor de Red** | Rust (Cargo / JNI) | Protocolos de streaming de baja latencia (RTMP, SRT) |
| **Testing** | Robolectric + Roborazzi | Pruebas unitarias en JVM y pruebas de regresión visual |

---

## 📱 Requisitos Previos

- **Dispositivo / Emulador:** Android 8.0 (API 26) o superior (Recomendado Android 10+ para captura de audio interno).
- **Entorno de Compilación:**
  - Android Studio Hedgehog o superior / AI Studio Build Environment
  - JDK 17+
  - Android SDK (API 34 / 35)
  - Android NDK (para módulos C++) y Rust Toolchain (`cargo-ndk` opcional para streaming)

---

## 🛠️ Compilación y Ejecución

```bash
# Compilar el proyecto y generar el APK
gradle assembleDebug

# Ejecutar pruebas unitarias locales (Robolectric)
gradle :app:testDebugUnitTest

# Verificar capturas de pantalla de UI (Roborazzi)
gradle :app:verifyRoborazziDebug
```

---

## 📂 Formato de Grabaciones

- **Contenedor:** MP4
- **Códec de Video:** H.264 / AVC (Codificación por Hardware)
- **Códec de Audio:** AAC (48 kHz, hasta 192 Kbps)
- **Ubicación de Videos:** `Movies/ScreenRecorder/` (accesible para compartir y reproducir).
- **Ubicación de Capturas:** `Pictures/Screenshots/` (indexado en MediaStore).
