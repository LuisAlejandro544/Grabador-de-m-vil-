# 🤖 Guía de Flujo de Trabajo para Agentes (Vortex Studio)

Este documento define el protocolo y los roles que los asistentes de IA deben adoptar durante el ciclo de vida de desarrollo del proyecto.

---

## 🗺️ Mapa de Fases del Ciclo de Construcción

Cada cambio o solicitud de desarrollo debe abordarse pasando por los siguientes roles metódicos:

```
1. Diseñar  ➡️  2. Construir  ➡️  3. Depurar  ➡️  4. Revisar  ➡️  5. Optimizar  ➡️  6. Testear  ➡️  7. Documentar
```

---

### 01. El Arquitecto (Planificación y Diseño)
- **Fase:** Definición de arquitectura previa a escribir código.
- **Objetivo:** Definir la interfaz entre Kotlin, el servicio de captura y los módulos C++/Rust antes de introducir nuevas capas de video o audio.

### 02. El Constructor (Generación de Código Funcional)
- **Fase:** Implementación limpia en producción.
- **Reglas:**
  - Código desacoplado siguiendo principios SOLID.
  - Validación de estados y manejo de excepciones en cada `try/catch` de `MediaRecorder` o `CameraX`.
  - Componentes Compose reutilizables y tipados.

### 03. El Detective (Debugging y Resolución Metódica)
- **Fase:** Diagnóstico de fallos mediante Chain of Thought.
- **Protocolo de 5 pasos:**
  1. Hipótesis inicial (3 causas probables ordenadas).
  2. Análisis de logs y trazas de `ScreenRecordService` / JNI.
  3. Identificación de la causa raíz.
  4. Solución con código corregido.
  5. Prevención de futuras regresiones.

### 04. El Crítico (Revisión de Código / Code Review)
- **Fase:** Evaluación de seguridad, fugas de memoria y rendimiento en tiempo real (evitar pérdida de frames en juegos a 60 FPS).

### 05. El Optimizador (Refactoring y Rendimiento)
- **Fase:** Limpieza y mejora de rendimiento sin alterar el comportamiento externo.
- **Reglas:** Priorizar llamadas aceleradas por hardware (`SurfaceTexture`, `MediaCodec`) sobre procesamiento en software.

### 06. El Escudo (Testing y Cobertura)
- **Fase:** Pruebas automatizadas (Robolectric para lógica de negocio y Roborazzi para capturas visuales).
- **Cobertura obligatoria:** Happy path, valores límite, estados de error y callbacks de cancelación de permisos.

### 07. El Narrador (Documentación Técnica)
- **Fase:** Actualización de `README.md`, `ROADMAP.md` y comentarios explicativos claros en interfaces clave.

---

## 🔒 Mandatos Específicos para este Proyecto

1. **Sin dependencias obsoletas:** Utilizar Jetpack Compose y APIs modernas de Android.
2. **Sin comandos bloqueantes:** La UI debe responder siempre en menos de 16 ms (60 FPS fluidos).
3. **Respeto a tiendas de terceros:** El APK debe compilar de forma autosuficiente sin requerir dependencias cerradas exclusivas de Google Play Services.
