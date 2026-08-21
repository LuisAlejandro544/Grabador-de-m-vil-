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
│       │   │   │   ├── InstalledGamesHelper.kt # Detector de juegos instalados en el dispositivo
│       │   │   │   └── RecordingsRepository.kt # Acceso a videos grabados en MediaStore
│       │   │   ├── service/
│       │   │   │   ├── ScreenRecordService.kt     # Coordinador ligero de Foreground Service
│       │   │   │   ├── ScreenCaptureEngine.kt     # Motor modular de captura (MediaProjection, VirtualDisplay y MediaRecorder)
│       │   │   │   ├── RecordNotificationHelper.kt# Gestión modular de canales y notificaciones persistentes con acciones
│       │   │   │   ├── RecordStorageHelper.kt     # Rutas seguras de archivos MP4 e indexación en MediaStore
│       │   │   │   ├── ScreenshotHelper.kt        # Captura instantánea de pantalla y extracción de fotogramas
│       │   │   │   ├── ScreenDrawingOverlay.kt    # Lienzo interactivo y pincel de dibujo en tiempo real sobre la pantalla
│       │   │   │   ├── FloatingBubbleManager.kt   # Coordinador del ciclo de vida del widget flotante y herramientas
│       │   │   │   ├── BubbleOverlayView.kt       # Jerarquía visual del widget, cronómetro en vivo, pulsos y menú de herramientas
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
│       │   │       │   └── SettingsView.kt        # Ajustes de calidad, audio, burbuja y estado nativo
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
   - Completamente modularizada: `HomeScreen.kt` actúa como orquestador liviano delegando en `RecordTab`, `GalleryTab`, `RecordTopBar` y `RecordBottomBar`.
   - Implementa `testTag` en todos los componentes interactivos.

2. **Capa de Negocio y Estado (`RecordViewModel.kt`):**
   - Expone un flujo reactivo inmutable `uiState: StateFlow<UiState>`.
   - Coordina temporizadores de cuenta atrás y la comunicación con el servicio de grabación.

3. **Capa de Servicio y Captura (`service/`):**
   - **`ScreenRecordService`:** Foreground Service liviano con tipo `MEDIA_PROJECTION`.
   - **`ScreenCaptureEngine`:** Encapsula de forma exclusiva `MediaProjection`, `VirtualDisplay` y `MediaRecorder`.
   - **`RecordNotificationHelper`:** Construye notificaciones interactivas con botones de acción (Pausar/Reanudar/Detener).
   - **`RecordStorageHelper`:** Gestiona el sistema de archivos y sincronización con `MediaStore`.
   - **`ScreenshotHelper`:** Genera instantáneas en alta calidad (.png) y las indexa automáticamente en la galería de imágenes.
   - **`ScreenDrawingOverlay`:** Monta un lienzo transparente acelerado por hardware en `WindowManager` para dibujar trazos, anotaciones o marcas con selección de colores y grosores durante grabaciones o gameplays.
   - **`FloatingBubbleManager` & `BubbleOverlayView` & `BubbleTouchHandler`:** Widget flotante desacoplado con menú de herramientas expandible (*Captura*, *Pincel*), arrastre suave y cronómetro en vivo sobre cualquier juego o aplicación.

4. **Capa Nativa C++ (`cpp/`):**
   - Diseñada para procesar texturas gráficas en tiempo real vía OpenGL ES 3.0 (máscaras de cámara y Chroma Key) y motor FFmpeg puro (libav*) para manipulación de video.

5. **Capa Nativa Rust (`rust/`):**
   - Diseñada para empaquetado de red a alta velocidad, control de congestión y streaming sin riesgo de punteros nulos o *data races*.
