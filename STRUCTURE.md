# 📂 Estructura del Proyecto — Vortex Studio

```
vortex-studio/
├── .github/workflows/          # CI/CD Workflows (build-apk.yml y build-beta-release.yml)
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
│       │   │   │   ├── RecordingConfig.kt        # Modelo con ImageFormatOption, Bitrate y Overlays
│       │   │   │   └── ReleaseChannel.kt         # Canales de versión (Dev, Canary, Beta, Estable) y Package IDs
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
│       │   │   │   ├── FacecamOverlayManager.kt  # Orquestador de la ventana flotante de cámara
│       │   │   │   ├── FloatingBubbleManager.kt  # Gestor de burbuja y menú de herramientas
│       │   │   │   ├── TouchVisualizerOverlay.kt # Indicador táctil de pantalla
│       │   │   │   ├── WatermarkOverlayManager.kt # Marca de agua y logotipo dinámico
│       │   │   │   ├── SceneOverlayManager.kt    # Marcos PNG y alertas de escena
│       │   │   │   ├── controller/               # Controladores de Notificaciones y Screenshots
│       │   │   │   │   ├── ServiceNotificationController.kt # Actualización reactiva de notificación
│       │   │   │   │   └── ServiceScreenshotController.kt   # Captura dual (ImageReader / Video Fallback)
│       │   │   │   ├── state/                    # Gestor de Estado Centralizado
│       │   │   │   │   └── ServiceStateManager.kt      # StateFlows reactivos desacoplados
│       │   │   │   ├── receiver/                 # Receptores de Eventos del Sistema
│       │   │   │   │   └── ServiceEmergencyReceiver.kt # Batería baja y almacenamiento crítico
│       │   │   │   ├── timer/                    # Cronómetro y Monitoreo Periódico
│       │   │   │   │   └── ServiceChronometerTimer.kt  # Tiempo y salvaguarda periódica de disco
│       │   │   │   ├── dispatcher/               # Despachador de Acciones y Tipos de Servicio
│       │   │   │   │   └── ServiceActionDispatcher.kt  # Foreground Types (A14+) y Overlays
│       │   │   │   ├── facecam/                  # Submódulos de la Facecam Flotante
│       │   │   │   │   ├── FacecamCameraEngine.kt      # Streaming CameraX, lente y target FPS
│       │   │   │   │   ├── FacecamWindowHost.kt        # LayoutParams y gestión en WindowManager
│       │   │   │   │   ├── FacecamViewHierarchy.kt     # Jerarquía de vistas, RGB y controles
│       │   │   │   │   ├── FacecamControlsBar.kt       # Botonera desplegable de acciones
│       │   │   │   │   ├── FacecamLifecycleOwner.kt    # LifecycleOwner desacoplado de Compose
│       │   │   │   │   ├── FacecamRgbBorderView.kt     # Borde animado RGB arcoíris
│       │   │   │   │   ├── FacecamShapeHelper.kt       # Recorte geométrico y dimensiones
│       │   │   │   │   └── FacecamTouchDragHelper.kt   # Arrastre táctil y snap magnético
│       │   │   │   ├── vtuber/                   # Sistema de Avatar 2D / PNGtuber
│       │   │   │   ├── vumeter/                  # Vúmetro LED y Mezclador Flotante
│       │   │   │   └── capture/                  # Codificadores, DSP y MuxerManager (Zero-Latency AV Sync Engine)
│       │   │   │       ├── AudioEncoderWorker.kt     # Hilo dedicado de codificación AAC con PTS continuo y sample-accurate
│       │   │   │       ├── AudioPipelineModule.kt    # Orquestador de captura de audio, compensador de delay y DSP
│       │   │   │       ├── AudioDspMixer.kt          # Mezclador DSP C++ y ducking
│       │   │   │       ├── InternalAudioWorker.kt    # Captura de audio interno de juegos
│       │   │   │       ├── MicAudioWorker.kt         # Captura de micrófono y amplitudes
│       │   │   │       ├── VideoEncoderModule.kt     # Codificador H.264/AVC por hardware con KEY_REPEAT_PREVIOUS_FRAME_AFTER
│       │   │   │       └── MuxerManager.kt           # Multiplexor MP4 seguro con anclaje de reloj y sincronización de pausas real
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
│       │   │           ├── RecordTopBar.kt                 # Barra superior con estado, logo adaptativo y timer
│       │   │           ├── VortexAppLogo.kt                # Logo vectorial adaptativo (Vórtice neón multi-canal)
│       │   │           ├── SettingsView.kt                 # Vista modular de configuración tipada
│       │   │           ├── HomeModalsHost.kt               # Hospedaje modular de diálogos (Player, Editor y Countdown)
│       │   │           ├── CountdownOverlayModal.kt        # Modal animado de cuenta atrás global y preparación
│       │   │           ├── GameLauncherCard.kt             # Pantalla orquestadora de juegos
│       │   │           ├── gamelauncher/                   # Componentes modulares del lanzador
│       │   │           │   ├── GameLauncherHeaderBanner.kt # Banner y botón de refresco
│       │   │           │   ├── GameLauncherFilterBar.kt    # Filtros y buscador
│       │   │           │   ├── GameLauncherItemCard.kt     # Tarjeta individual con lanzamiento directo
│       │   │           │   └── GameLauncherEmptyState.kt   # Estado vacío y acciones rápidas
│       │   │           └── settings/                       # Paneles modulares de configuración
│       │   │               ├── SettingsEventHolders.kt     # Contenedores tipados de eventos de ajustes
│       │   │               ├── OnboardingTutorialCard.kt   # Acceso directo al tutorial y permisos
│       │   │               ├── VideoQualitySettingsCard.kt # Resolución, FPS y Bitrate
│       │   │               ├── ImageFormatSettingsCard.kt  # PNG, JPG % y WebP Lossless
│       │   │               ├── AudioSettingsCard.kt        # Audio DSP, Noise Gate y Ducking
│       │   │               ├── FacecamSettingsCard.kt      # Facecam, RGB y Belleza
│       │   │               ├── VtuberSettingsCard.kt       # PNGtuber 2D reactivo
│       │   │               ├── TouchVisualizerSettingsCard.kt # Toques táctiles
│       │   │               ├── WatermarkSettingsCard.kt    # Marca de agua superpuesta
│       │   │               ├── SceneOverlaySettingsCard.kt # Marcos y alertas de escena
│       │   │               ├── FloatingBubbleSettingsCard.kt # Burbuja flotante
│       │   │               ├── GameModeCard.kt             # Modo juego
│       │   │               ├── CountdownSettingsCard.kt    # Conteo regresivo
│       │   │               ├── NativeModulesStatusCard.kt  # Monitor C++ y Rust
│       │   │               ├── ReleaseChannelInfoCard.kt   # Canales de versión, Package IDs y ciclo de vida
│       │   │               └── SettingsCard.kt             # Contenedor base de tarjeta
│       │   └── res/                              # Recursos gráficos, iconos y estilos
│       └── test/                                 # Tests unitarios y de arquitectura
├── AGENTS.md                   # Protocolo y roles de desarrollo
├── AI_CONTEXT.md               # Memoria de arquitectura del proyecto
├── changelog-beta-release.md   # Notas y novedades de la versión Beta para Pre-releases
├── commit_message.txt          # Historial de cambios en español
├── CONTRIBUTING.md             # Guía de contribución comunitaria y política de PRs
├── LICENSE                     # Licencia Pública General de GNU v3 (GPLv3)
├── README.md                   # Documentación general del producto
├── ROADMAP.md                  # Mapa de fases y objetivos
└── STRUCTURE.md                # Árbol y descripción del código
```
