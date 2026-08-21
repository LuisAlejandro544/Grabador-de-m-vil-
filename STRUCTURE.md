# 🏛️ Estructura del Proyecto (Architecture & File Tree)

Este archivo describe la organización de directorios, módulos y capas del proyecto **OBS Mobile**.

---

## 🌳 Árbol de Archivos

```
obs-mobile/
├── app/
│   ├── build.gradle.kts                     # Configuración de compilación Android & dependencias
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # Permisos, servicios en primer plano y componentes
│       │   ├── cpp/                         # Motor C++ nativo (Composición Gráfica OpenGL ES 3.0 & FFmpeg Puro)
│       │   │   ├── CMakeLists.txt           # Configuración de CMake para NDK (GLESv3, EGL, Log, Android)
│       │   │   ├── obs_compositor.hpp       # Definición de capas, máscara Facecam, Chroma Key y EGL
│       │   │   ├── obs_compositor.cpp       # Implementación de shaders GLSL, pipeline EGL y renderizado
│       │   │   ├── ffmpeg_engine.hpp        # Interfaz de procesamiento FFmpeg puro (libav*)
│       │   │   ├── ffmpeg_engine.cpp        # Implementación de pipeline de recorte, audio y transcodificación
│       │   │   └── obs_core.cpp             # JNI export bridge completo para Kotlin (OBS & FFmpeg)
│       │   ├── rust/                        # Motor Rust nativo (Streaming & Red)
│       │   │   ├── Cargo.toml               # Configuración Cargo (cdylib, JNI, logging)
│       │   │   └── src/
│       │   │       └── lib.rs               # Sesiones de streaming, RTMP/SRT y ABR
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt          # Entrada principal (Edge-to-edge Compose)
│       │   │   ├── model/
│       │   │   │   ├── RecordingConfig.kt   # Modelos de configuración (FPS, Bitrate, Audio, Burbuja)
│       │   │   │   └── RecordedVideo.kt     # Entidad de video grabado con helpers de formato
│       │   │   ├── nativecore/
│       │   │   │   ├── NativeOBSBridge.kt   # Puente JNI seguro hacia C++ (GLES3 / EGL / Transformaciones)
│       │   │   │   ├── NativeFFmpegBridge.kt# Puente JNI seguro hacia FFmpeg Puro (libav* NDK)
│       │   │   │   └── NativeRustNetwork.kt # Puente JNI seguro hacia Rust (RTMP/SRT)
│       │   │   ├── data/
│       │   │   │   └── InstalledAppItem.kt  # Modelo y detector de apps/juegos instalados
│       │   │   ├── service/
│       │   │   │   ├── ScreenRecordService.kt # Foreground Service (MediaProjection & MediaRecorder)
│       │   │   │   └── FloatingBubbleManager.kt # Widget flotante interactivo con WindowManager
│       │   │   └── ui/
│       │   │       ├── RecordViewModel.kt   # Gestión de estado (StateFlow) y lógica UI
│       │   │       ├── HomeScreen.kt        # Pantalla principal con pestañas y navegación
│       │   │       ├── components/
│       │   │       │   ├── RecordControlCard.kt # Tarjeta central de grabación y pulso
│       │   │       │   ├── GameLauncherCard.kt  # Pestaña de acceso rápido a juegos
│       │   │       │   ├── VideoItemCard.kt     # Elemento de lista en galería de videos
│       │   │       │   ├── VideoPlayerDialog.kt # Reproductor de video nativo
│       │   │       │   └── SettingsView.kt      # Ajustes de calidad, audio, burbuja y estado nativo
│       │   │       └── theme/
│       │   │           ├── Color.kt             # Paleta de colores M3
│       │   │           ├── Theme.kt             # Configuración de tema claro/oscuro
│       │   │           └── Type.kt              # Tipografía Material 3
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
└── AGENTS.md                                # Flujo de trabajo y normas para agentes
```

---

## 🧩 Responsabilidad por Capas

1. **Capa de Presentación (`ui/`):**
   - Construida 100% con **Jetpack Compose**.
   - No contiene lógica de bajo nivel; delega todas las acciones en `RecordViewModel`.
   - Implementa `testTag` en todos los componentes interactivos.

2. **Capa de Negocio y Estado (`RecordViewModel.kt`):**
   - Expone un único flujo reactivo inmutable `uiState: StateFlow<UiState>`.
   - Controla el temporizador de cuenta atrás y la comunicación con el servicio de fondo.

3. **Capa de Captura de Sistema (`service/ScreenRecordService.kt`):**
   - Ejecuta en un proceso con notificación `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`.
   - Coordina `MediaProjection`, `VirtualDisplay` y `MediaRecorder`.

4. **Capa Nativa C++ (`cpp/`):**
   - Diseñada para procesar texturas gráficas en tiempo real vía OpenGL ES sin saturar el recolector de basura de la JVM.

5. **Capa Nativa Rust (`rust/`):**
   - Diseñada para empaquetado de red a alta velocidad, control de congestión y streaming sin riesgo de punteros nulos o *data races*.
