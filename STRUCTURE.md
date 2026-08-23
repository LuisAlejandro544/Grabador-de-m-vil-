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
│       │   │   ├── editor/                       # Motor de Edición de Video
│       │   │   │   └── VideoEditorManager.kt     # Stream-Copy, Aspect Ratio 1-Tap, Split y Thumbnails
│       │   │   ├── model/
│       │   │   │   ├── RecordedVideo.kt
│       │   │   │   └── RecordingConfig.kt
│       │   │   ├── data/
│       │   │   │   ├── InstalledGamesHelper.kt
│       │   │   │   ├── RecordingsRepository.kt
│       │   │   │   └── SettingsRepository.kt
│       │   │   ├── nativecore/                   # Puentes JNI
│       │   │   │   ├── NativeAudioDSPBridge.kt
│       │   │   │   ├── NativeFFmpegBridge.kt
│       │   │   │   ├── NativeOBSBridge.kt
│       │   │   │   └── NativeRustNetwork.kt
│       │   │   ├── service/                      # Servicio de Grabación y Overlays
│       │   │   │   ├── ScreenRecordService.kt
│       │   │   │   ├── ScreenCaptureEngine.kt
│       │   │   │   ├── FacecamOverlayManager.kt
│       │   │   │   ├── FloatingBubbleManager.kt
│       │   │   │   ├── TouchVisualizerOverlay.kt
│       │   │   │   ├── WatermarkOverlayManager.kt
│       │   │   │   ├── SceneOverlayManager.kt
│       │   │   │   ├── vtuber/                   # Sistema de Avatar 2D / PNGtuber
│       │   │   │   ├── vumeter/                  # Vúmetro LED y Mezclador Flotante
│       │   │   │   └── capture/                  # Codificadores y DSP
│       │   │   └── ui/
│       │   │       ├── HomeScreen.kt
│       │   │       ├── RecordViewModel.kt
│       │   │       ├── editor/                   # UI del Editor Avanzado
│       │   │       │   └── VideoEditorDialog.kt  # Aspect ratio chips, split tool, filmstrip
│       │   │       ├── tabs/                     # Tabs de Grabación y Galería
│       │   │       └── components/               # Tarjetas y controles Compose
│       │   └── res/                              # Recursos gráficos, iconos y estilos
├── AGENTS.md                   # Protocolo y roles de desarrollo
├── AI_CONTEXT.md               # Memoria de arquitectura del proyecto
├── commit_message.txt          # Historial de cambios en español
├── README.md                   # Documentación general del producto
├── ROADMAP.md                  # Mapa de fases y objetivos
└── STRUCTURE.md                # Árbol y descripción del código
```
