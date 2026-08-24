package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Matriz de Canales de Lanzamiento y Distribución de Vortex Studio.
 *
 * Canales disponibles:
 * 1. [DEV]: Exclusivo de desarrollo y compilación debug local.
 * 2. [CANARY]: Funciones tempranas/experimentales para recopilar feedback de la comunidad.
 * 3. [BETA]: Versión candidata a lanzamiento con funciones pulidas.
 * 4. [STABLE]: Versión oficial y probada para distribución general (Uptodown, GitHub Releases).
 */
enum class ReleaseChannel(
    val channelId: String,
    val displayName: String,
    val shortName: String,
    val packageNameSuffix: String,
    val fullPackageName: String,
    val versionSuffix: String,
    val tag: String,
    val description: String,
    val badgeColorHex: Long,
    val hasExperimentalFeatures: Boolean,
    val allowsDebugLogs: Boolean,
    val isCommunityFeedbackChannel: Boolean
) {
    DEV(
        channelId = "dev",
        displayName = "Desarrollador (Debug)",
        shortName = "Vortex (Dev)",
        packageNameSuffix = ".dev",
        fullPackageName = "com.vortexstudio.recorder.dev",
        versionSuffix = "-dev",
        tag = "DEV",
        description = "Canal exclusivo para desarrollo y entorno de compilación. Incluye logs detallados, herramientas de depuración e inspección de memoria.",
        badgeColorHex = 0xFF9C27B0, // Púrpura Material
        hasExperimentalFeatures = true,
        allowsDebugLogs = true,
        isCommunityFeedbackChannel = false
    ),
    CANARY(
        channelId = "canary",
        displayName = "Canary (Experimental / Lab)",
        shortName = "Vortex Canary",
        packageNameSuffix = ".canary",
        fullPackageName = "com.vortexstudio.recorder.canary",
        versionSuffix = "-canary.1",
        tag = "CANARY",
        description = "Funciones preliminares para que los usuarios prueben y den feedback temprano. Características en evolución que pueden fallar o modificarse.",
        badgeColorHex = 0xFFFF9800, // Naranja vibrante
        hasExperimentalFeatures = true,
        allowsDebugLogs = true,
        isCommunityFeedbackChannel = true
    ),
    BETA(
        channelId = "beta",
        displayName = "Beta (Release Candidate)",
        shortName = "Vortex Beta",
        packageNameSuffix = ".beta",
        fullPackageName = "com.vortexstudio.recorder.beta",
        versionSuffix = "-beta.1",
        tag = "BETA",
        description = "Versión en fase de pulido final y optimización de rendimiento a 60 FPS. Se evalúa estabilidad antes del despliegue masivo.",
        badgeColorHex = 0xFF2196F3, // Azul Material
        hasExperimentalFeatures = false,
        allowsDebugLogs = false,
        isCommunityFeedbackChannel = true
    ),
    STABLE(
        channelId = "stable",
        displayName = "Estable (Producción)",
        shortName = "Vortex Studio",
        packageNameSuffix = "",
        fullPackageName = "com.vortexstudio.recorder",
        versionSuffix = "",
        tag = "STABLE",
        description = "Versión oficial y sólida para el público general. Distribuible en tiendas de APKs (Uptodown, GitHub Releases, APKMirror).",
        badgeColorHex = 0xFF4CAF50, // Verde esmeralda
        hasExperimentalFeatures = false,
        allowsDebugLogs = false,
        isCommunityFeedbackChannel = false
    );

    fun getBadgeColor(): Color = Color(badgeColorHex)

    fun matchesRelease(tagName: String, releaseName: String, isPrerelease: Boolean): Boolean {
        val lowerTag = tagName.lowercase()
        val lowerName = releaseName.lowercase()
        return when (this) {
            BETA -> (lowerTag.contains("beta") || lowerName.contains("beta"))
            CANARY -> (lowerTag.contains("canary") || lowerName.contains("canary"))
            DEV -> (lowerTag.contains("dev") || lowerName.contains("dev"))
            STABLE -> (!isPrerelease && !lowerTag.contains("beta") && !lowerTag.contains("canary") && !lowerTag.contains("dev"))
        }
    }

    fun getFullVersionName(baseVersion: String = BASE_VERSION_NAME): String {
        return "$baseVersion$versionSuffix"
    }

    fun getVersionCode(major: Int = 0, minor: Int = 1, patch: Int = 0): Int {
        val channelOffset = when (this) {
            DEV -> 0
            CANARY -> 1
            BETA -> 2
            STABLE -> 3
        }
        return (major * 100000) + (minor * 10000) + (patch * 1000) + channelOffset
    }

    companion object {
        const val BASE_VERSION_NAME = "0.1.0"
        const val CURRENT_STAGE = "v0.1.0"

        /**
         * Retorna el canal activo del entorno actual según la configuración de compilación.
         */
        fun getCurrentChannel(): ReleaseChannel {
            val channelId = try {
                com.example.BuildConfig.RELEASE_CHANNEL
            } catch (_: Exception) {
                "dev"
            }
            return entries.find { it.channelId.equals(channelId, ignoreCase = true) } ?: DEV
        }

        /**
         * Retorna la lista de todos los canales ordenados por ciclo de vida.
         */
        fun getAllChannels(): List<ReleaseChannel> = entries.toList()
    }
}
