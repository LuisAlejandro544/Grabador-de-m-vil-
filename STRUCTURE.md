# 🏛️ Estructura del Proyecto (Architecture & File Tree)

Este archivo describe la organización de directorios, módulos y capas del proyecto **OBS Mobile**.

---

## 🌳 Árbol de Archivos

```
obs-mobile/
├── .github/
│   └── workflows/
│       ├── build-apk.yml                    # Compilación automatizada de APK Debug con caché y keystore
│       └── override-commit.yml              # Sincronización del mensaje del commit desde commit_message.txt
├── app/
│   ├── build.gradle.kts                     # Configuración de compilación Android & dependencias
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # Permisos, servicios en primer plano y componentes
│       │   ├── cpp/                         # Motor C++ nativo (Composición Gráfica OpenGL ES 3.0, DSP Audio & FFmpeg Puro)
│       │   │   ├── CMakeLists.txt           # Configuración de CMake para NDK (GLESv3, EGL, Log, Android)
│       │   │   ├── obs_compositor.hpp       # Definición de capas, máscara Facecam, Chroma Key y EGL
│       │   │   ├── obs_compositor.cpp       # Implementación de shaders GLSL, pipeline EGL y renderizado
│       │   │   ├── audio_dsp_engine.hpp     # Interfaz del procesador digital de señales de audio (Noise Gate, Ducking, Limiter)
│       │   │   ├── audio_dsp_engine.cpp     # Implementación del motor DSP en tiempo real (48 kHz Estéreo)
│       │   │   ├── ffmpeg_engine.hpp        # Interfaz de procesamiento FFmpeg puro (libav*)
│       │   │   ├── ffmpeg_engine.cpp        # Implementación de pipeline de recorte, audio y transcodificación
│       │   │   └── obs_core.cpp             # JNI export bridge completo para Kotlin (OBS, DSP & FFmpeg)
│       │   ├── rust/                        # Motor Rust nativo (Streaming & Red)
│       │   │   ├── Cargo.toml               # Configuración Cargo (cdylib, JNI, logging)
│       │   │   └── src/
│       │   │       └── lib.rs               # Sesiones de streaming, RTMP/SRT y ABR
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt          # Entrada principal (Edge-to-edge Compose)
│       │   │   ├── model/
│       │   │   │   ├── RecordingConfig.kt   # Modelos de configuración (FPS, Bitrate, Audio, Facecam, Belleza, RGB, Toques)
│       │   │   │   └── RecordedVideo.kt     # Entidad de video grabado con helpers de formato
│       │   │   ├── nativecore/
│       │   │   │   ├── NativeOBSBridge.kt   # Puente JNI seguro hacia C++ (GLES3 / EGL / Transformaciones)
│       │   │   │   ├── NativeAudioDSPBridge.kt # Puente JNI seguro hacia C++ Audio DSP (Noise Gate, Ducking, Limiter)
│       │   │   │   ├── NativeFFmpegBridge.kt# Puente JNI seguro hacia FFmpeg Puro (libav* NDK)
│       │   │   │   └── NativeRustNetwork.kt # Puente JNI seguro hacia Rust (RTMP/SRT)
│       │   │   ├── data/
│       │   │   │   ├── InstalledGamesHelper.kt # Detector de juegos instalados en el dispositivo
│       │   │   │   ├── SettingsRepository.kt   # Persistencia centralizada de ajustes (Audio, Facecam, Belleza, RGB, Toques)
│       │   │   │   └── RecordingsRepository.kt # Acceso a videos grabados en MediaStore
│       │   │   ├── service/
│       │   │   │   ├── ScreenRecordService.kt     # Coordinador ligero de Foreground Service y conmutación de audio
│       │   │   │   ├── ScreenCaptureEngine.kt     # Motor modular de captura (MediaProjection, MediaRecorder y mezclador PCM dual de audio)
│       │   │   │   ├── RecordNotificationHelper.kt# Gestión modular de notificaciones persistentes con acciones (Pausa, Stop, Toggle Voz)
│       │   │   │   ├── RecordStorageHelper.kt     # Rutas seguras de archivos MP4 e indexación en MediaStore
│       │   │   │   ├── ScreenshotHelper.kt        # Captura instantánea de pantalla y extracción de fotogramas
│       │   │   │   ├── ScreenDrawingOverlay.kt    # Lienzo interactivo y pincel de dibujo en tiempo real sobre la pantalla
│       │   │   │   ├── FacecamOverlayManager.kt   # Gestor de Facecam flotante con Filtro de Belleza y Borde RGB animado
│       │   │   │   ├── TouchVisualizerOverlay.kt  # Overlay de toques táctiles animados sin opciones de desarrollador
│       │   │   │   ├── FloatingBubbleManager.kt   # Coordinador del ciclo de vida del widget flotante y herramientas
│       │   │   │   ├── BubbleOverlayView.kt       # Jerarquía visual del widget con submenús de herramientas, Facecam y toques
│       │   │   │   └── BubbleTouchHandler.kt      # Detección y cálculo de arrastre táctil y toques
│       │   │   └── ui/
│       │   │       ├── RecordViewModel.kt         # Gestión de estado (StateFlow) y lógica UI
│       │   │       ├── HomeScreen.kt              # Pantalla principal desacoplada (Orquestador)
│       │   │       ├── tabs/
│       │   │       │   ├── RecordTab.kt           # Pestaña principal de grabación, pulso y accesos directos
│       │   │       │   └── GalleryTab.kt          # Pestaña de galería de grabaciones y lista reactiva
│       │   │       ├── components/
│       │   │       │   ├── RecordTopBar.kt        # Barra superior con badge de estado en tiempo real
│       │   │       │   ├── RecordBottomBar.kt     # Barra inferior de navegación entre pestañas
│       │   │       │   ├── RecordControlCard.kt   # Tarjeta central de grabación y pulso
│       │   │       │   ├── GameLauncherCard.kt    # Pestaña y tarjetas de acceso rápido a juegos
│       │   │       │   ├── VideoItemCard.kt       # Elemento individual de video en galería
│       │   │       │   ├── VideoPlayerDialog.kt   # Reproductor de video nativo integrado
│       │   │       │   └── SettingsView.kt        # Ajustes de calidad, audio, Facecam, Belleza, RGB y Toques
│       │   │       └── theme/
│       │   │           ├── Color.kt               # Paleta de colores M3
│       │   │           ├── Theme.kt               # Configuración de tema claro/oscuro
│       │   │           └── Type.kt                # Tipografía Material 3
│       │   └── res/
│       │       ├── values/
│       │       │   └── strings.xml          # Recursos de texto localizados
│       │       └── drawable/                # Iconos vectoriales y recursos visuales
│       └── test/java/com/example/
│           ├── ExampleRobolectricTest.kt    # Pruebas unitarias de contexto y lógica
│           └── GreetingScreenshotTest.kt    # Pruebas de captura Roborazzi
├── README.md                                # Guía general del proyecto
├── ROADMAP.md                               # Plan de evolución a OBS Studio
├── STRUCTURE.md                             # Este archivo (Estructura técnica)
├── AI_CONTEXT.md                            # Contexto para asistentes de inteligencia artificial
├── AGENTS.md                                # Flujo de trabajo y normas para agentes
└── commit_message.txt                       # Registro descriptivo en español del último commit
```

---

## 🧩 Responsabilidad por Capas

1. **Capa de Presentación (`ui/`):**
   - Construida 100% con **Jetpack Compose (Material Design 3)**.
   - Completamente modularizada: `HomeScreen.kt` actúa como orquestador liviano delegando en `RecordTab`, `GalleryTab`, `GameLauncherCard` y `SettingsView`.
   - Implementa `testTag` en todos los componentes interactivos.

2. **Capa de Negocio y Estado (`RecordViewModel.kt`):**
   - Expone un flujo reactivo inmutable `uiState: StateFlow<UiState>`.
   - Coordina el inicio seguro del servicio de grabación en primer plano antes de la cuenta atrás para garantizar compatibilidad estricta con Android 14+.

3. **Capa de Servicios y Overlays (`service/`):**
   - `ScreenRecordService`: Servicio desacoplado tipo Media Projection, Microphone y Camera.
   - `FacecamOverlayManager`: Ventana flotante de cámara con máscaras geométricas, Filtro de Belleza y Borde RGB animado.
   - `TouchVisualizerOverlay`: Renderizado de ripples y retroalimentación táctil de alta velocidad en pantalla completa sin requerir depuración USB ni opciones de desarrollador.
   - `FloatingBubbleManager`: Controlador de widget flotante con submenú interactivo para alternar herramientas, voz y efectos en vivo.
