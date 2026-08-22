package com.example.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoQualitySettingsCard(
    resolution: VideoResolution,
    fps: VideoFps,
    bitrate: VideoBitrate,
    bitrateMbps: Int,
    onUpdateResolution: (VideoResolution) -> Unit,
    onUpdateFps: (VideoFps) -> Unit,
    onUpdateBitrate: (VideoBitrate) -> Unit,
    onUpdateBitrateMbps: (Int) -> Unit,
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
                    description = if (vFps == VideoFps.FPS_60) "Recomendado para gameplays (60 FPS fluidos)" else "Ahorra batería y almacenamiento",
                    selected = fps == vFps,
                    onClick = { onUpdateFps(vFps) },
                    testTag = "fps_option_${vFps.name}"
                )
            }
        }

        // Bitrate Section (Personalizable de 1 a 12 Mbps)
        SettingsCard(
            title = "Tasa de Bits Personalizada (1 - 12 Mbps)",
            subtitle = "Ajusta con precisión el bitrate para equilibrar nitidez y peso de archivo",
            icon = Icons.Default.Storage
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header con valor actual
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bitrate Actual",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$bitrateMbps Mbps (${bitrateMbps * 1000} Kbps)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = when {
                            bitrateMbps >= 10 -> "Ultra Nitidez"
                            bitrateMbps >= 6 -> "Calidad Equilibrada"
                            else -> "Ahorro de Espacio"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider interactivo del 1 al 12
                Slider(
                    value = bitrateMbps.toFloat(),
                    onValueChange = { newValue ->
                        onUpdateBitrateMbps(newValue.roundToInt().coerceIn(1, 12))
                    },
                    valueRange = 1f..12f,
                    steps = 10, // Valores enteros 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bitrate_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 Mbps (Bajo)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("6 Mbps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("12 Mbps (Máx)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Acceso Rápido a Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 4, 6, 8, 10, 12).forEach { mbpsValue ->
                        FilterChip(
                            selected = bitrateMbps == mbpsValue,
                            onClick = { onUpdateBitrateMbps(mbpsValue) },
                            label = { Text("$mbpsValue M") },
                            modifier = Modifier.testTag("preset_bitrate_${mbpsValue}m"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
