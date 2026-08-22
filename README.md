# 🌪️ Vortex Studio & Game Recorder

> **Vortex Studio** es una plataforma de grabación de pantalla de alto rendimiento y transmisión en vivo para Android, diseñada para capturar partidas de videojuegos a 60 FPS con audio interno exclusivo, overlays visuales, herramientas en vivo (captura de pantalla y pincel/anotaciones en tiempo real), **Filtro de Belleza facial**, **Borde RGB Arcoíris animado**, **Indicador de Toques Táctiles animado (sin opciones de desarrollador)** y cimientos nativos en **C++ (OpenGL ES 3.0)** y **Rust (RTMP/SRT)**.

---

## 🚀 Características Principales

- **🎮 Modo Optimizado para Juegos:** Configuración instantánea a 1080p Full HD, 60 FPS y 12 Mbps con un solo toque.
- **⚡ Tasa de Bits Personalizada (1 - 12 Mbps):** Control milimétrico de bitrate mediante un deslizador interactivo con saltos enteros y botones de acceso rápido (1M, 2M, 4M, 6M, 8M, 10M, 12M) para balancear a la perfección calidad de imagen y peso de archivo.
- **✨ Filtro de Belleza & Suavizado de Piel:**
  - Capa de post-procesado facial que atenúa imperfecciones, suaviza texturas y balancea la luminosidad del rostro.
  - Conmutable y configurable tanto en la pestaña de **Ajustes** como en tiempo real desde la **Burbuja Flotante** o controles del Facecam.
- **🌈 Borde RGB / Arcoíris Gamer Animado:**
  - Marco con gradiente `SweepGradient` rotativo continuo que bordea la ventana flotante del Facecam adaptándose automáticamente a su forma (Circular, Cuadrado redondeado o Rectangular).
  - Control de activación inmediata desde los Ajustes y el submenú de la Burbuja Flotante.
- **📹 Facecam / Cámara Flotante con FPS Configurable (30 - 60 FPS):**
  - Superposición de cámara en tiempo real mediante **CameraX** y `WindowManager` sin cortes de fluidez.
  - **Tasa de Cuadros de Cámara Ajustable:** Selección directa de 30 FPS (estándar de ahorro), 45 FPS, 50 FPS y 60 FPS (máxima fluidez) configurada por hardware mediante `Camera2Interop` (`CONTROL_AE_TARGET_FPS_RANGE`).
  - **Múltiples Diseños Geométricos:** Selector de formas con un solo toque (Circular 1:1, Cuadrado Redondeado Moderno, Cuadrado Clásico 1:1 y Rectangular Panorámico 16:9 estilo webcam streamer).
  - **Dimensiones y Lentes:** Tamaños configurables (Pequeño 100dp, Mediano 140dp, Grande 180dp) y conmutación instantánea entre cámara frontal (selfie) y trasera.
- **🎭 Avatar 2D / PNGtuber Reactivo por Voz:**
  - Alternativa ligera y privada a la cámara real: sustituye el Facecam por un avatar 2D animado interactivo que reacciona a tu voz en tiempo real.
  - **Detección Acústica Instantánea:** Abre y mueve la boca sincrónicamente al hablar analizando el nivel RMS del micrófono.
  - **Parpadeo Ocular Automático:** Simula parpadeos naturales aleatorios (cada 3-5 segundos) para dar vida al personaje.
  - **Presets Gamer Incluidos:** *Gamer Cat*, *Cyber Fox* y *Chibi Bot* renderizados con gráficos vectoriales nativos ultra-ligeros.
  - **Soporte de PNGs Personalizados de 4 Estados:** Importa tus propias ilustraciones desde el almacenamiento (`Boca Cerrada + Ojos Abiertos`, `Boca Abierta + Ojos Abiertos`, `Boca Cerrada + Ojos Cerrados`, `Boca Abierta + Ojos Cerrados`).
  - **Totalmente Arrastrable:** Reposicionable libremente en pantalla mediante toques y conmutador rápido desde el menú de la Burbuja Flotante.
- **👆 Indicador de Toques Táctiles Animado (Touch Visualizer):**
  - Ondas y ripples dinámicos de alta respuesta que se renderizan sobre la pantalla mediante overlay `WindowManager` transparente.
  - **Sin opciones de desarrollador:** Funciona directamente sin requerir activar "Mostrar toques" en el sistema operativo.
  - **Paleta de Colores Personalizable:** Azul Neón, Verde Gamer, Púrpura Neón, Rojo Fuego, Amarillo Eléctrico y Blanco Puro, configurable en Ajustes y conmutable en caliente desde la Burbuja Flotante.
- **🏷️ Marca de Agua / Logo Personalizado Superpuesto:**
  - **Texto o Logo PNG:** Superpone tu marca personal, tag de streamer o logo en pantalla con posición libre (arrastrable con el dedo).
  - **Personalización Completa:** Selector de texto, fuente, tamaño (Pequeño, Mediano, Grande), paleta de colores neón y control de opacidad/transparencia (15% - 100%).
  - **Conmutador Rápido:** Configurable desde la pestaña de Ajustes y activable/desactivable en tiempo real desde el menú de la Burbuja Flotante.
- **🖼️ Overlays de Escena Personalizados (Marcos PNG y Banners de Streamer):**
  - **Presets Gamer Integrados:** Marco Neón Cyberpunk para bordes de pantalla, Banner de Redes Sociales inferior, Badge animado "🔴 EN VIVO" y Cartel de Pausa ("⏸️ STANDBY").
  - **Soporte de PNG Personalizado:** Carga de imágenes PNG transparentes completas desde la galería.
  - **Control de Opacidad:** Ajuste fino de transparencia para no obstruir el gameplay, conmutable en caliente desde la Burbuja Flotante.
- **🔊 Captura, Mezcla, Frecuencia Ajustable y DSP en C++:**
  - **📊 Vúmetro de Audio Flotante & Mezclador OBS en Vivo:**
    - Widget superpuesto interactivo y arrastrable sobre cualquier juego que muestra barras LED dinámicas de decibelios (dB) con gradiente verde-amarillo-rojo para monitoreo en vivo de señales acústicas.
    - **Control de Ganancia Independiente:** Faders deslizantes en pantalla para regular de forma precisa el volumen del juego (0% a 200%) y del micrófono/voz (0% a 200%) sin detener la grabación.
    - **Conmutación Rápida de Filtros DSP:** Botones táctiles para encender/apagar en caliente la Puerta de Ruido (Noise Gate), el Auto-Ducking y el silenciamiento del micrófono (Mute).
  - **🎛️ Frecuencia de Muestreo (Sample Rate):** Selección de tasa de muestreo entre 32.000 Hz, 44.100 Hz (calidad CD), 48.000 Hz (estándar broadcast recomendado) y 96.000 Hz (Hi-Res para alta fidelidad acústica).
  - **🎛️ Motor Nativo de Audio DSP (C++):** Procesamiento de señales estéreo con **Noise Gate** (puerta de ruido para atenuar respiración y ventiladores), **Audio Ducking Automático** (atenúa el juego -9 dB cuando hablas para dar prioridad a tu voz) y **Soft Limiter / Saturation Shaper** para prevenir distorsión y saturación digital al mezclar.
  - **🎙️ Conmutador Dinámico de Voz en Vivo:** Graba el audio del juego y conmuta con un toque desde la burbuja flotante, el vúmetro o la notificación si deseas grabar tu voz (`Voz ON`) o dejar únicamente el sonido limpio del juego (`Solo Juego`), mezclando audio PCM en tiempo real sin cortar el video ni reiniciar codificadores.
  - **Solo Audio del Juego (Interno):** Graba música y efectos del juego sin capturar tu voz ni ruidos del entorno.
  - **Micrófono:** Captura tu voz y comentarios en vivo con reducción de ruido en caliente.
  - **Mudo:** Graba únicamente el video para optimizar espacio de almacenamiento.
- **⚡ Control en Segundo Plano & Burbuja Flotante con Herramientas:** 
  - Servicio persistente con notificación interactiva y widget flotante superpuesto en pantalla (`WindowManager`) con cronómetro en vivo, botón de conmutación rápida de voz/juego, pausa, reanudación y parada directa sin salir del juego.
  - **📹 Facecam / Cámara Flotante en Vivo:**
    - Superposición de cámara en tiempo real mediante **CameraX** y `WindowManager` sin cortes de FPS.
    - **Múltiples Diseños Geométricos:** Selector de formas con un solo toque (Circular 1:1, Cuadrado Redondeado Moderno, Cuadrado Clásico 1:1 y Rectangular Panorámico 16:9 estilo webcam streamer).
    - **Dimensiones y Lentes:** Tamaños configurables (Pequeño 100dp, Mediano 140dp, Grande 180dp) y conmutación instantánea entre cámara frontal (selfie) y trasera.
    - **Controles en Vivo:** Activación/desactivación instantánea del Facecam, Filtro de Belleza y Borde RGB desde el submenú de herramientas.
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
| **Efectos Visuales & Overlays** | `WindowManager` + Custom Hardware Views | Filtro de belleza, Borde RGB animado y Visualizador de toques táctiles |
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
