package com.example.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImageFormatOption
import kotlin.math.roundToInt

/**
 * Componente Material 3 para la configuración avanzada de formato y compresión de capturas e imágenes.
 * Permite seleccionar entre PNG (sin pérdida), JPEG (10-100% configurable) y WebP (alta compresión / sin pérdida).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageFormatSettingsCard(
    imageFormat: ImageFormatOption,
    imageQuality: Int,
    imageWebpLossless: Boolean,
    onUpdateImageFormat: (ImageFormatOption) -> Unit,
    onUpdateImageQuality: (Int) -> Unit,
    onToggleWebpLossless: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = "Formato y Compresión de Imagen",
        subtitle = "Ajusta el formato (PNG, JPG, WebP) y porcentaje de compresión para capturas de pantalla",
        icon = Icons.Default.Image,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Opciones de Formato de Imagen
            ImageFormatOption.values().forEach { format ->
                SettingsRadioItem(
                    title = format.label,
                    description = format.description,
                    selected = imageFormat == format,
                    onClick = { onUpdateImageFormat(format) },
                    testTag = "image_format_option_${format.name}"
                )
            }

            // Sección de Calidad y Compresión (Visible para JPG y WebP)
            AnimatedVisibility(
                visible = imageFormat == ImageFormatOption.JPEG || imageFormat == ImageFormatOption.WEBP,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Switch de WebP sin pérdida
                    if (imageFormat == ImageFormatOption.WEBP) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modo WebP Sin Pérdida (Lossless)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Conserva 100% de la información gráfica original",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = imageWebpLossless,
                                onCheckedChange = onToggleWebpLossless,
                                modifier = Modifier.testTag("switch_webp_lossless"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Slider de Calidad (si no es WebP Lossless)
                    if (imageFormat == ImageFormatOption.JPEG || !imageWebpLossless) {
                        // Tarjeta resumen de calidad
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nivel de Calidad",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$imageQuality%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = when {
                                    imageQuality >= 90 -> "Alta Fidelidad"
                                    imageQuality >= 75 -> "Recomendado (Balance)"
                                    imageQuality >= 50 -> "Ahorro Ligero"
                                    else -> "Máximo Ahorro"
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = imageQuality.toFloat(),
                            onValueChange = { newValue ->
                                onUpdateImageQuality(newValue.roundToInt().coerceIn(10, 100))
                            },
                            valueRange = 10f..100f,
                            steps = 17, // Pasos de 5% (10, 15, ..., 95, 100)
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_quality_slider"),
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
                            Text("10% (Muy bajo)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("80% (Estándar)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100% (Máx)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Presets Rápidos de Compresión",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(50, 70, 80, 90, 100).forEach { presetVal ->
                                FilterChip(
                                    selected = imageQuality == presetVal,
                                    onClick = { onUpdateImageQuality(presetVal) },
                                    label = {
                                        Text(if (presetVal == 80) "$presetVal% ⭐" else "$presetVal%")
                                    },
                                    modifier = Modifier.testTag("preset_image_quality_${presetVal}"),
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

            // Resumen informativo de ventajas
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (imageFormat) {
                        ImageFormatOption.PNG -> "💡 PNG guarda cada píxel intacto. Ideal para textos nítidos y edición posterior."
                        ImageFormatOption.JPEG -> "💡 JPG al ${imageQuality}% reduce significativamente el peso del archivo manteniendo una excelente claridad visual."
                        ImageFormatOption.WEBP -> if (imageWebpLossless) "💡 WebP Sin Pérdida ofrece la nitidez de PNG ocupando hasta un 26% menos espacio." else "💡 WebP al ${imageQuality}% es un formato moderno que optimiza al máximo el almacenamiento."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
