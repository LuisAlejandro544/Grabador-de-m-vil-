# OBS Mobile & Game Recorder

> **OBS Mobile** es una plataforma de grabación de pantalla de alto rendimiento y transmisión en vivo para Android, diseñada para capturar partidas de videojuegos a 60 FPS con audio interno exclusivo, overlays visuales y cimientos nativos en **C++ (OpenGL ES 3.0)** y **Rust (RTMP/SRT)**.

---

## 🚀 Características Principales

- **🎮 Modo Optimizado para Juegos:** Configuración instantánea a 1080p Full HD, 60 FPS y 12 Mbps con un solo toque.
- **🔊 Captura de Audio Versátil:**
  - **Solo Audio del Juego (Interno):** Graba música y efectos del juego sin capturar tu voz ni ruidos del entorno.
  - **Micrófono:** Captura tu voz y comentarios en vivo para tutoriales y gameplays comentados.
  - **Mudo:** Graba únicamente el video para optimizar espacio de almacenamiento.
- **⚡ Control en Segundo Plano:** Servicio persistente con notificación interactiva para Pausar, Reanudar y Detener la grabación mientras juegas a pantalla completa.
- **🕹️ Lanzador de Juegos Rápido:** Detección de juegos y aplicaciones instaladas; permite iniciar la grabación y abrir el juego simultáneamente.
- **🎬 Galería y Reproductor Integrado:** Gestión de archivos grabados (MP4 H.264 / AAC), reproducción instantánea, renombrado y compartir.
- **⚙️ Cimientos Nativos C++ & Rust:**
  - **C++ (NDK / GLES3):** Motor de composición de escenas y capas de video en tiempo real.
  - **Rust (cdylib / JNI):** Motor de red seguro para transmisión RTMP/SRT con bitrate adaptativo.

---

## 🏗️ Stack Tecnológico

| Capa | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Frontend / UI** | Kotlin + Jetpack Compose (Material 3) | Interfaz moderna, reactiva y adaptativa para móviles |
| **Arquitectura** | MVVM + Coroutines + StateFlow | Estado unidireccional y reactividad desacoplada |
| **Captura de Pantalla** | Android `MediaProjection` + `MediaRecorder` | Captura acelerada por hardware de pantalla y audio |
| **Motor Gráfico** | C++ (OpenGL ES 3.0 / EGL) | Composición de capas: pantalla, facecam, overlays |
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
- **Ubicación:** `Android/data/com.example/files/Movies/ScreenRecordings/` (accesible para compartir y reproducir).
