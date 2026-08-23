# 📂 Estructura del Proyecto — Vortex Studio

```
vortex-studio/
├── .github/workflows/          # CI/CD Workflows para compilación de APK
├── app/
│   ├── build.gradle.kts        # Configuración de compilación Android y dependencias
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── cpp/            # Motor Nativo C++ NDK
│       │   │   ├── CMakeLists.txt
│       │   │   ├── audio_dsp_engine.cpp / .hpp   # Noise Gate, Ducking y Soft Limiter
│       │   │   ├── ffmpeg_engine.cpp / .hpp      # Trim, Split, Aspect Ratio y transcode
│       │   │   ├── obs_compositor.cpp / .hpp     # Renderizado OpenGL ES 3.0
│       │   │   └── obs_core.cpp                  # Exportaciones JNI para Kotlin
│       │   ├── rust/           # Motor Nativo Rust
│       │   │   ├── Cargo.toml
│       │   │   └── src/lib.rs                    # RTMP/SRT Engine y cálculo de Aspect Ratio
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── editor/                       # Motor de Edición de Video (Arquitectura Modular)
│       │   │   │   ├── AspectRatioModels.kt      # Enums y cálculo de dimensiones de aspect ratio
│       │   │   │   ├── VideoEditorManager.kt     # Fachada orquestadora del editor
│       │   │   │   └── engine/                   # Submotores de procesamiento de video
│       │   │   │       ├── VideoStreamCopyEngine.kt   # Recorte, Split y Aspect Ratio Stream-Copy
│       │   │   │       └── VideoThumbnailEngine.kt    # Captura de fotogramas HD y Filmstrip
│       │   │   ├── model/
│       │   │   │   ├── RecordedVideo.kt
│       │   │   │   └── RecordingConfig.kt        # Modelo con ImageFormatOption, Bitrate y Overlays
│       │   │   ├── data/
│       │   │   │   ├── InstalledGamesHelper.kt
│       │   │   │   ├── RecordingsRepository.kt
│       │   │   │   ├── SettingsRepository.kt     # Fachada de configuración y reactividad
│       │   │   │   ├── StorageMonitorHelper.kt   # Cálculo de espacio en disco y tiempo restante
│       │   │   │   └── settings/                 # Subalmacenes modulares de persistencia
│       │   │   │       ├── VideoAudioSettingsStore.kt    # Persistencia de video y audio DSP
│       │   │   │       ├── OverlaySettingsStore.kt       # Persistencia de Facecam, VTuber y Overlays
│       │   │   │       └── GameAndImageSettingsStore.kt  # Modo juego, timer y capturas
│       │   │   ├── nativecore/                   # Puentes JNI
│       │   │   │   ├── NativeAudioDSPBridge.kt
│       │   │   │   ├── NativeFFmpegBridge.kt
│       │   │   │   ├── NativeOBSBridge.kt
│       │   │   │   └── NativeRustNetwork.kt
│       │   │   ├── service/                      # Servicio de Grabación y Overlays
│       │   │   │   ├── ScreenRecordService.kt    # Orquestador del Foreground Service
│       │   │   │   ├── ScreenCaptureEngine.kt    # Coordinador con Shutdown Hook y cierre seguro
│       │   │   │   ├── ScreenshotHelper.kt       # Capturador multiformato (PNG, JPG % y WebP)
│       │   │   │   ├── ServiceParamsExtractor.kt # Validador y extractor de parámetros
│       │   │   │   ├── FacecamOverlayManager.kt
│       │   │   │   ├── FloatingBubbleManager.kt
│       │   │   │   ├── TouchVisualizerOverlay.kt
│       │   │   │   ├── WatermarkOverlayManager.kt
│       │   │   │   ├── SceneOverlayManager.kt
│       │   │   │   ├── state/                    # Gestor de Estado Centralizado
│       │   │   │   │   └── ServiceStateManager.kt      # StateFlows reactivos desacoplados
│       │   │   │   ├── receiver/                 # Receptores de Eventos del Sistema
│       │   │   │   │   └── ServiceEmergencyReceiver.kt # Batería baja y almacenamiento crítico
│       │   │   │   ├── timer/                    # Cronómetro y Monitoreo Periódico
│       │   │   │   │   └── ServiceChronometerTimer.kt  # Tiempo y salvaguarda periódica de disco
│       │   │   │   ├── dispatcher/               # Despachador de Acciones y Tipos de Servicio
│       │   │   │   │   └── ServiceActionDispatcher.kt  # Foreground Types (A14+) y Overlays
│       │   │   │   ├── vtuber/                   # Sistema de Avatar 2D / PNGtuber
│       │   │   │   ├── vumeter/                  # Vúmetro LED y Mezclador Flotante
│       │   │   │   └── capture/                  # Codificadores, DSP y MuxerManager (Graceful Finalize)
│       │   │   │       ├── AudioEncoderWorker.kt     # Hilo dedicado de codificación AAC y drenado
│       │   │   │       ├── AudioPipelineModule.kt    # Orquestador de captura de audio y DSP
│       │   │   │       ├── AudioDspMixer.kt          # Mezclador DSP C++ y ducking
│       │   │   │       ├── InternalAudioWorker.kt    # Captura de audio interno de juegos
│       │   │   │       ├── MicAudioWorker.kt         # Captura de micrófono y amplitudes
│       │   │   │       ├── VideoEncoderModule.kt     # Codificador H.264/AVC por hardware
│       │   │   │       └── MuxerManager.kt           # Multiplexor MP4 seguro
│       │   │   └── ui/
│       │   │       ├── HomeScreen.kt             # Pantalla principal desacoplada
│       │   │       ├── RecordViewModel.kt        # Orquestador MVVM desacoplado
│       │   │       ├── RecordCountdownManager.kt # Gestor del conteo regresivo
│       │   │       ├── RecordServiceLauncher.kt  # Despachador de Intents del servicio
│       │   │       ├── launcher/                 # Lanzadores y Permisos Reutilizables
│       │   │       │   └── ScreenRecordPermissionHelper.kt # ActivityResultLaunchers de captura
│       │   │       ├── delegates/                # Delegados de Estado y Operaciones
│       │   │       │   ├── VideoGalleryDelegate.kt     # Galería, reproducción y edición
│       │   │       │   └── SettingsActionsDelegate.kt  # Ajustes de video, audio, facecam y avatares
│       │   │       ├── editor/                   # UI Modular del Editor Avanzado
│       │   │       │   ├── VideoEditorDialog.kt            # Contenedor orquestador del editor
│       │   │       │   ├── VideoEditorHeader.kt            # Barra superior con exportación
│       │   │       │   ├── AspectRatioSelectorRow.kt       # Selector 1-Tap y modos de ajuste
│       │   │       │   ├── VideoEditorPreviewPlayer.kt     # Monitor central con Blur reactivo
│       │   │       │   ├── VideoEditorPlaybackControls.kt  # Botones de transporte, Split y Foto HD
│       │   │       │   ├── VideoEditorFilmstripScrubber.kt  # Timeline, RangeSlider y Filmstrip
│       │   │       │   └── VideoEditorModals.kt            # Confirmación de Split y Overlay de Progreso
│       │   │       ├── onboarding/               # Flujo de Bienvenida y Centro de Permisos
│       │   │       │   ├── OnboardingScreen.kt         # Orquestador del flujo y transiciones
│       │   │       │   ├── OnboardingStepPage.kt       # Diapositivas explicativas (60 FPS, DSP, 9:16)
│       │   │       │   └── PermissionsSetupPage.kt     # Centro interactivo de concesión de permisos
│       │   │       ├── tabs/                     # Tabs de Grabación y Galería
│       │   │       └── components/               # Tarjetas y controles Compose
│       │   │           ├── HomeModalsHost.kt               # Hospedaje modular de diálogos (Player y Editor)
│       │   │           ├── GameLauncherCard.kt             # Pantalla orquestadora de juegos
│       │   │           ├── gamelauncher/                   # Componentes modulares del lanzador
│       │   │           │   ├── GameLauncherHeaderBanner.kt # Banner y botón de refresco
│       │   │           │   ├── GameLauncherFilterBar.kt    # Filtros y buscador
│       │   │           │   ├── GameLauncherItemCard.kt     # Tarjeta individual con lanzamiento directo
│       │   │           │   └── GameLauncherEmptyState.kt   # Estado vacío y acciones rápidas
│       │   │           └── settings/                       # Paneles modulares de configuración
│       │   └── res/                              # Recursos gráficos, iconos y estilos
│       └── test/                                 # Tests unitarios y de arquitectura
├── AGENTS.md                   # Protocolo y roles de desarrollo
├── AI_CONTEXT.md               # Memoria de arquitectura del proyecto
├── commit_message.txt          # Historial de cambios en español
├── README.md                   # Documentación general del producto
├── ROADMAP.md                  # Mapa de fases y objetivos
└── STRUCTURE.md                # Árbol y descripción del código
```
