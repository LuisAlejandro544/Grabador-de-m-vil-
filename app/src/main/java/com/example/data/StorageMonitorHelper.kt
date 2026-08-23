package com.example.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

/**
 * Modelo de datos inmutable que representa el estado del almacenamiento en disco del dispositivo
 * y el tiempo estimado de grabación disponible según el bitrate activo.
 */
data class StorageSpaceInfo(
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercentage: Float = 0f,
    val freePercentage: Float = 1f,
    val formattedAvailable: String = "0 GB",
    val formattedTotal: String = "0 GB",
    val formattedUsed: String = "0 GB",
    val estimatedSecondsRemaining: Long = 0L,
    val formattedEstimatedTime: String = "0 min",
    val isLowSpace: Boolean = false,
    val isCriticalSpace: Boolean = false
)

/**
 * Utilidad de consulta y cálculo de almacenamiento en disco en tiempo real para Vortex Studio.
 */
object StorageMonitorHelper {

    /** Umbral de advertencia de espacio bajo (1 GB) */
    const val LOW_SPACE_THRESHOLD_BYTES = 1024L * 1024L * 1024L

    /** Umbral crítico de espacio en disco (150 MB) */
    const val CRITICAL_SPACE_THRESHOLD_BYTES = 150L * 1024L * 1024L

    /** Umbral de parada de emergencia para salvaguardar y finalizar el MP4 antes de saturar el disco (50 MB) */
    const val EMERGENCY_STOP_THRESHOLD_BYTES = 50L * 1024L * 1024L

    /**
     * Consulta el espacio disponible en la partición donde se guardan las grabaciones
     * y calcula el tiempo de grabación restante estimado según el bitrate seleccionado.
     */
    fun queryStorageInfo(context: Context, bitrateBps: Int): StorageSpaceInfo {
        return try {
            val targetDir = getTargetStorageDirectory(context)
            val stat = StatFs(targetDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

            val usedFraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
            val freeFraction = if (totalBytes > 0) (availableBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 1f

            // Bitrate total efectivo = video bitrate + 192 kbps aproximados para audio AAC estéreo
            val effectiveBitrateBps = (bitrateBps + 192_000).coerceAtLeast(1_000_000)
            val bytesPerSecond = effectiveBitrateBps / 8L

            // Dejamos el margen de seguridad de parada de emergencia en el cálculo utilizable
            val usableBytes = (availableBytes - EMERGENCY_STOP_THRESHOLD_BYTES).coerceAtLeast(0L)
            val estimatedSeconds = if (bytesPerSecond > 0) usableBytes / bytesPerSecond else 0L

            StorageSpaceInfo(
                availableBytes = availableBytes,
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                usedPercentage = usedFraction,
                freePercentage = freeFraction,
                formattedAvailable = formatFileSize(availableBytes),
                formattedTotal = formatFileSize(totalBytes),
                formattedUsed = formatFileSize(usedBytes),
                estimatedSecondsRemaining = estimatedSeconds,
                formattedEstimatedTime = formatEstimatedTime(estimatedSeconds),
                isLowSpace = availableBytes < LOW_SPACE_THRESHOLD_BYTES,
                isCriticalSpace = availableBytes < CRITICAL_SPACE_THRESHOLD_BYTES
            )
        } catch (_: Exception) {
            StorageSpaceInfo(
                formattedAvailable = "N/A",
                formattedTotal = "N/A",
                formattedEstimatedTime = "Calculando..."
            )
        }
    }

    /**
     * Retorna el directorio objetivo de almacenamiento de grabaciones.
     */
    fun getTargetStorageDirectory(context: Context): File {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val recordingDir = File(moviesDir, "ScreenRecorder")
        return if (recordingDir.exists() || recordingDir.mkdirs()) {
            recordingDir
        } else {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "ScreenRecorder")
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            fallbackDir
        }
    }

    /**
     * Formatea una cantidad en bytes a una representación legible (B, KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Convierte segundos estimados de grabación en un formato compacto legible (ej. "2h 45m", "35 min").
     */
    fun formatEstimatedTime(totalSec: Long): String {
        if (totalSec <= 0) return "0 min"
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        return when {
            hours > 24 -> {
                val days = hours / 24
                val remHours = hours % 24
                "${days}d ${remHours}h"
            }
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1 min"
        }
    }
}
