# 🚀 Vortex Studio — Changelog Oficial (Versión Beta)

---

## 📌 Información de la Versión Oficial

- **Versión Oficial:** `v0.1.0-beta.1`
- **Código de Versión (Version Code):** `1002`
- **Canal de Distribución:** `BETA`
- **Identificador de Aplicación (Package ID):** `com.vortexstudio.recorder.beta`
- **Compatibilidad Mínima:** Android 10.0 (API 29) o superior
- **Arquitecturas Nativas:** `arm64-v8a`, `armeabi-v7a`, `x86_64`
- **Destino:** Totalmente autosuficiente, libre de dependencias cerradas, optimizado para instalación directa, Uptodown y GitHub Releases.

---

## 🌟 ¿Qué ofrece Vortex Studio? (Suite Completa de Funcionalidades)

Vortex Studio es una grabadora de pantalla profesional y suite de producción móvil de alto rendimiento impulsada por Jetpack Compose, módulos nativos C++ (NDK) y Rust.

### 🎮 1. Motor de Captura de Video de Alto Rendimiento (60 FPS)
- **Codificación por Hardware:** Motor H.264 / AVC acelerado por GPU para grabación ultra fluida a 60 FPS sin pérdida de frames ni sobrecalentamiento.
- **Resolución Adaptativa:** Soporte para resoluciones SD (480p), HD (720p), Full HD (1080p), 2K (1440p) y 4K (2160p) adaptadas a la pantalla de tu teléfono.
- **Tasa de Bits Personalizable (Bitrate):** Ajuste fino de bitrate desde 4 Mbps hasta 50 Mbps para capturar videojuegos competitivos con máxima nitidez.
- **Refresco Continuo en Pantallas Estáticas:** Inyección automática de fotogramas (`KEY_REPEAT_PREVIOUS_FRAME_AFTER`) para evitar congelamientos en interfaces de usuario estáticas.

### 🔄 2. Sincronización A/V de Latencia Cero (Zero-Latency Sync Engine)
- **Alineación de Reloj Precisa:** Anclaje del reloj base al primer fotograma de video en `MuxerManager`, garantizando sincronización exacta entre audio y video.
- **Timestamps Continuos PCM:** Cálculo de marcas de tiempo basado en muestras acústicas para eliminar el jitter y desfases producidos por fluctuaciones del sistema operativo.
- **Calibración Manual de Delay A/V:** Control deslizante interactivo en los ajustes de audio con compensación configurable de **-200 ms a +200 ms**.

### 🔊 3. Sistema de Audio Profesional y DSP Nativo en C++
- **Fuentes de Audio Flexibles:** Grabación de Sonido Interno del Juego, Micrófono o Modo Mixto (Juego + Voz).
- **Noise Gate (Puerta de Ruido):** Filtro DSP nativo que silencia automáticamente ruidos de fondo, respiración y ventiladores cuando no estás hablando.
- **Audio Ducking Inteligente:** Atenúa de forma suave el volumen del juego (-9 dB) en tiempo real al detectar tu voz en el micrófono.
- **Vúmetro Flotante en Vivo:** Barra LED dinámica superpuesta en pantalla que mide decibelios (dB) con gradiente verde-amarillo-rojo.
- **Resolución Acústica de Estudio:** Selección de frecuencias de muestreo (44.1 kHz, 48 kHz y 96 kHz).
- **Conmutador Rápido de Voz:** Botón de un toque para alternar entre `Voz ON` y `Solo Juego` durante la sesión.

### 📷 4. Facecam Pro y Avatar 2D / PNGtuber Reactivo
- **Facecam con CameraX:** Soporte para cámara frontal y trasera con tasa de cuadros configurable (30 o 60 FPS).
- **Borde RGB Cyberpunk & Filtro de Belleza:** Marcos animados multicolor con gradiente neón y suavizado de piel.
- **Avatar 2D / PNGtuber para Creadores de Contenido:** Avatar virtual interactivo que abre y cierra la boca reactivamente al volumen de tu voz (con ajuste de umbral dB).
- **Geometría y Arrastre Libre:** Formatos Circular, Cuadrado y Redondeado con snap magnético a los bordes de la pantalla.

### 🎨 5. Overlays de Escena, Marcas de Agua y Visualizador Táctil
- **Marcos de Escena:** Diseños predefinidos (Neón Cyberpunk, Gamer, Minimalista) para enriquecer directos y grabaciones.
- **Banners y Badges Animados:** Cartel "🔴 EN VIVO" con efecto parpadeante y banners inferiores para tus redes sociales.
- **Marca de Agua Personalizada:** Superposición de texto o imagen con opacidad regulable y posicionamiento en 4 esquinas.
- **Visualizador de Toques:** Muestra los puntos de contacto en pantalla con estelas de color en tiempo real.

### ✂️ 6. Editor de Video y Post-Producción
- **Herramienta de Recorte (Trim):** Selección precisa del inicio y fin de la grabación.
- **División de Video (Split):** Corte de grabaciones largas en múltiples segmentos independientes.
- **Adaptación de Formato (Aspect Ratio):** Conversión entre formato apaisado (16:9) y vertical (9:16) optimizado para TikTok, Instagram Reels y YouTube Shorts impulsado por Rust.

### 🛡️ 7. Seguridad, Almacenamiento y Protección Anti-Corrupción
- **Graceful Finalize:** Cierre seguro y escritura garantizada del átomo `moov` en contenedores MP4 mediante Shutdown Hooks, evitando archivos corruptos ante apagados o cierre inesperado.
- **Monitor de Almacenamiento en Vivo:** Cálculo estimado de minutos restantes de grabación según el espacio libre y el bitrate elegido.
- **Capturas de Pantalla Multiformato:** Guardado instantáneo en formato PNG (sin pérdida), JPEG (calidad 10-100%) y WebP (lossy/lossless).

### 🕹️ 8. Control Rápido y Burbuja Flotante
- **Burbuja Flotante Translúcida:** Menú radial accesible sobre cualquier juego o app para pausar, reanudar, silenciar micrófono o detener la grabación.
- **Diseño Moderno Material 3:** Interfaz moderna con tema Cyberpunk Dark, feedback háptico y navegación fluida sin pantallas de carga pesadas.

---

## 📥 Instalación

1. Descarga el archivo `Vortex-Studio-Beta-Release.apk` desde los **Assets** de esta Pre-Release o desde la notificación directa en Telegram.
2. Abre el archivo `.apk` en tu dispositivo Android y pulsa **Instalar**.
