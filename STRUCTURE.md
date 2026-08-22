# 🏛️ Estructura del Proyecto (Architecture & File Tree)

Este archivo describe la organización de directorios, módulos y capas del proyecto **Vortex Studio** (Grabador de Pantalla & Streaming OBS).

---

## 🌳 Árbol de Archivos

```
vortex-studio/
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
│       │   │   │   ├── ScreenCaptureEngine.kt     # Fachada orquestadora modular de captura y grabación
│       │   │   │   ├── capture/                   # Submódulos desacoplados del motor de captura de video y audio
│       │   │   │   │   ├── VideoEncoderModule.kt  # Codificador de hardware H.264 / AVC, Input Surface y worker de video
│       │   │   │   │   ├── AudioPipelineModule.kt # Captura dual (juego interno + mic), DSP C++ y codificador AAC
│       │   │   │   │   └── MuxerManager.kt        # Gestor sincronizado thread-safe del contenedor MP4
│       │   │   │   ├── RecordNotificationHelper.kt# Gestión modular de notificaciones persistentes con acciones
│       │   │   │   ├── RecordStorageHelper.kt     # Rutas seguras de archivos MP4 e indexación en MediaStore
│       │   │   │   ├── ScreenshotHelper.kt        # Captura instantánea de pantalla y extracción de fotogramas
│       │   │   │   ├── ScreenDrawingOverlay.kt    # Lienzo interactivo y pincel de dibujo en tiempo real
│       │   │   │   ├── FacecamOverlayManager.kt   # Gestor de Facecam flotante desacoplado
│       │   │   │   ├── facecam/                   # Submódulos del sistema de Facecam superpuesto
│       │   │   │   │   ├── FacecamLifecycleOwner.kt# LifecycleOwner desacoplado para vincular CameraX en Service
│       │   │   │   │   ├── FacecamShapeHelper.kt  # Utilidades de dimensiones y recorte geométrico
│       │   │   │   │   ├── FacecamRgbBorderView.kt# Vista de borde dinámico con gradiente animado RGB arcoíris
│       │   │   │   │   ├── FacecamControlsBar.kt  # Barra inferior flotante de herramientas rápidas
│       │   │   │   │   └── FacecamTouchDragHelper.kt# Gestor táctil de arrastre magnético y eventos táctiles
│       │   │   │   ├── TouchVisualizerOverlay.kt  # Overlay de toques táctiles animados sin opciones de desarrollador
│       │   │   │   ├── WatermarkOverlayManager.kt # Gestor de marca de agua / logo flotante superpuesto
│       │   │   │   ├── watermark/                 # Submódulos de marca de agua
│       │   │   │   │   └── WatermarkTouchHelper.kt# Cálculo táctil y arrastre magnético de la marca de agua
│       │   │   │   ├── SceneOverlayManager.kt     # Gestor de overlays de escena (Marcos Neón, Banners, Live, Pausa)
│       │   │   │   ├── overlay/                   # Submódulos de dibujo de escena
│       │   │   │   │   └── SceneOverlayDrawables.kt# Renderizado vectorial de marcos y banners de streamer
│       │   │   │   ├── FloatingBubbleManager.kt   # Coordinador del ciclo de vida del widget flotante y herramientas
│       │   │   │   ├── BubbleOverlayView.kt       # Jerarquía visual del widget flotante modular
│       │   │   │   ├── bubble/                    # Submódulos del widget flotante
│       │   │   │   │   ├── BubbleColors.kt        # Constantes de color y paleta visual del widget
│       │   │   │   │   ├── BubbleDrawables.kt     # Generador de fondos, bordes redondeados y formas vectoriales
│       │   │   │   │   ├── BubbleMainBar.kt       # Barra horizontal con led pulsante, cronómetro y acciones rápidas
│       │   │   │   │   └── BubbleToolsSubmenu.kt  # Submenú desplegable de herramientas (Captura, Pincel, Facecam, RGB, Toques, Logo, Escenas)
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
│       │   │       │   ├── SettingsView.kt        # Orquestador modular de ajustes
│       │   │       │   └── settings/              # Tarjetas de configuración especializadas y desacopladas
│       │   │       │       ├── SettingsCard.kt    # Componentes base reutilizables de tarjeta y radio items
│       │   │       │       ├── GameModeCard.kt    # Switch maestro de optimización gamer a 60 FPS
│       │   │       │       ├── FloatingBubbleSettingsCard.kt # Control de burbuja y estado de permiso de superposición
│       │   │       │       ├── WatermarkSettingsCard.kt      # Configuración de logo / marca de agua, texto, opacidad y PNG
│       │   │       │       ├── SceneOverlaySettingsCard.kt   # Selector de marcos gamer, banners streamer y alertas
│       │   │       │       ├── FacecamSettingsCard.kt        # Configuración completa de Facecam (Lente, Formas, Belleza, RGB)
│       │   │       │       ├── TouchVisualizerSettingsCard.kt# Configuración y selector de color de toques táctiles
│       │   │       │       ├── VideoQualitySettingsCard.kt   # Selectores de resolución, FPS y bitrate
│       │   │       │       ├── AudioSettingsCard.kt          # Selector de fuentes de audio (Juego, Mic, Dual, Silencio)
│       │   │       │       ├── CountdownSettingsCard.kt      # Configuración de cuenta atrás antes de grabar
│       │   │       │       └── NativeModulesStatusCard.kt    # Monitor de estado de motores C++ GLES3, Rust y DSP
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
   - `SettingsView` divide su lógica en componentes específicos bajo `ui/components/settings/` (`GameModeCard`, `FacecamSettingsCard`, `TouchVisualizerSettingsCard`, etc.).
   - Implementa `testTag` en todos los componentes interactivos.

2. **Capa de Negocio y Estado (`RecordViewModel.kt`):**
   - Expone un flujo reactivo inmutable `uiState: StateFlow<UiState>`.
   - Coordina el inicio seguro del servicio de grabación en primer plano antes de la cuenta atrás para garantizar compatibilidad estricta con Android 14+.

3. **Capa de Servicios, Captura y Overlays (`service/`):**
   - `ScreenRecordService`: Servicio desacoplado tipo Media Projection, Microphone y Camera.
   - `ScreenCaptureEngine`: Fachada que delega responsabilidades a submódulos en `service/capture/`:
     - `VideoEncoderModule`: Manejo de hardware encoder AVC/H.264 y entrega de buffers de video.
     - `AudioPipelineModule`: Captura dual PCM, filtrado C++ DSP y encoder AAC.
     - `MuxerManager`: Empaquetado MP4 sincronizado y thread-safe con candado reentrante.
   - `FacecamOverlayManager`: Orquestador de cámara flotante con submódulos en `service/facecam/`:
     - `FacecamLifecycleOwner`: Ciclo de vida desacoplado para CameraX en Service.
     - `FacecamShapeHelper`: Máscaras geométricas y cálculo métrico.
     - `FacecamRgbBorderView`: Vista de borde dinámico con degradado RGB arcoíris animado.
     - `FacecamControlsBar`: Barra inferior flotante de herramientas.
     - `FacecamTouchDragHelper`: Detección táctil y arrastre magnético.
   - `BubbleOverlayView`: Widget flotante con submódulos en `service/bubble/`:
     - `BubbleMainBar`: Cronómetro monospace, led pulsante y botones de acción rápida.
     - `BubbleToolsSubmenu`: Desplegable de herramientas rápidas.
   - `TouchVisualizerOverlay`: Renderizado de ripples táctiles animados sin depuración USB.
