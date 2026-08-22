package com.example.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution

@Composable
fun VideoQualitySettingsCard(
    resolution: VideoResolution,
    fps: VideoFps,
    bitrate: VideoBitrate,
    onUpdateResolution: (VideoResolution) -> Unit,
    onUpdateFps: (VideoFps) -> Unit,
    onUpdateBitrate: (VideoBitrate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Resolution Section
        SettingsCard(
            title = "Resolución de Video",
            subtitle = "Define el nivel de nitidez de la grabación",
            icon = Icons.Default.HighQuality
        ) {
            VideoResolution.values().forEach { res ->
                SettingsRadioItem(
                    title = res.label,
                    description = "${res.width} x ${res.height} píxeles",
                    selected = resolution == res,
                    onClick = { onUpdateResolution(res) },
                    testTag = "resolution_option_${res.name}"
                )
            }
        }

        // FPS Section
        SettingsCard(
            title = "Tasa de Cuadros (FPS)",
            subtitle = "Mayor FPS otorga mayor fluidez en juegos de acción",
            icon = Icons.Default.Speed
        ) {
            VideoFps.values().forEach { vFps ->
                SettingsRadioItem(
                    title = vFps.label,
                    description = if (vFps == VideoFps.FPS_60) "Recomendado para gameplays" else "Ahorra batería y almacenamiento",
                    selected = fps == vFps,
                    onClick = { onUpdateFps(vFps) },
                    testTag = "fps_option_${vFps.name}"
                )
            }
        }

        // Bitrate Section
        SettingsCard(
            title = "Tasa de Bits (Calidad)",
            subtitle = "Controla el balance entre calidad de imagen y peso del archivo",
            icon = Icons.Default.Storage
        ) {
            VideoBitrate.values().forEach { vBitrate ->
                SettingsRadioItem(
                    title = vBitrate.label,
                    description = "${vBitrate.bps / 1_000_000} Megabits por segundo",
                    selected = bitrate == vBitrate,
                    onClick = { onUpdateBitrate(vBitrate) },
                    testTag = "bitrate_option_${vBitrate.name}"
                )
            }
        }
    }
}
