package com.example.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize

@Composable
fun FacecamSettingsCard(
    showFacecam: Boolean,
    isFrontCamera: Boolean,
    beautyFilterEnabled: Boolean,
    facecamRgbBorder: Boolean,
    facecamShape: FacecamShape,
    facecamSize: FacecamSize,
    facecamFps: FacecamFps,
    onToggleFacecam: (Boolean) -> Unit,
    onToggleFacecamCamera: () -> Unit,
    onToggleBeautyFilter: (Boolean) -> Unit,
    onToggleFacecamRgbBorder: (Boolean) -> Unit,
    onUpdateFacecamShape: (FacecamShape) -> Unit,
    onUpdateFacecamSize: (FacecamSize) -> Unit,
    onUpdateFacecamFps: (FacecamFps) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Facecam (Cámara Flotante)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Muestra tu rostro superpuesto durante la grabación con varias formas y efectos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = showFacecam,
                    onCheckedChange = { onToggleFacecam(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("facecam_switch")
                )
            }

            if (showFacecam) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Lente de cámara (Frontal / Trasera)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFrontCamera) "Cámara Frontal (Selfie)" else "Cámara Trasera (Principal)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FilledTonalButton(
                        onClick = onToggleFacecamCamera,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("flip_camera_btn")
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cambiar Lente", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Filtro de Belleza / Suavizado de Piel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Color(0xFFF472B6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Filtro de Belleza / Suavizado de Piel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Atenúa imperfecciones y mejora la luminosidad facial",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = beautyFilterEnabled,
                        onCheckedChange = { onToggleBeautyFilter(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF472B6)
                        ),
                        modifier = Modifier.testTag("beauty_filter_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Borde RGB / Arcoíris Animado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Borde RGB / Arcoíris Gamer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Marco con gradiente animado en rotación continua",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = facecamRgbBorder,
                        onCheckedChange = { onToggleFacecamRgbBorder(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier.testTag("rgb_border_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Diseño y Forma del Facecam",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FacecamShape.values().forEach { shape ->
                    SettingsRadioItem(
                        title = shape.label,
                        description = when (shape) {
                            FacecamShape.CIRCLE -> "Estilo avatar circular clásico"
                            FacecamShape.ROUNDED_SQUARE -> "Esquinas redondeadas modernas"
                            FacecamShape.SQUARE -> "Cuadrado 1:1 definido"
                            FacecamShape.RECTANGLE -> "Panorámico 16:9 estilo webcam streamer"
                        },
                        selected = facecamShape == shape,
                        onClick = { onUpdateFacecamShape(shape) },
                        testTag = "facecam_shape_${shape.name}"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Tamaño en Pantalla",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FacecamSize.values().forEach { size ->
                    SettingsRadioItem(
                        title = size.label,
                        description = when (size) {
                            FacecamSize.SMALL -> "Discreto, ideal para partidas competitivas"
                            FacecamSize.MEDIUM -> "Equilibrado y nítido para streaming"
                            FacecamSize.LARGE -> "Gran detalle para tutoriales o podcasts"
                        },
                        selected = facecamSize == size,
                        onClick = { onUpdateFacecamSize(size) },
                        testTag = "facecam_size_${size.name}"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Tasa de Cuadros de Facecam (FPS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FacecamFps.values().forEach { fFps ->
                    SettingsRadioItem(
                        title = fFps.label,
                        description = when (fFps) {
                            FacecamFps.FPS_30 -> "30 FPS - Estándar estable y eficiente en batería"
                            FacecamFps.FPS_45 -> "45 FPS - Mayor fluidez intermedia"
                            FacecamFps.FPS_50 -> "50 FPS - Alta fluidez cinematográfica"
                            FacecamFps.FPS_60 -> "60 FPS - Máxima fluidez profesional y sincronización perfecta"
                        },
                        selected = facecamFps == fFps,
                        onClick = { onUpdateFacecamFps(fFps) },
                        testTag = "facecam_fps_${fFps.name}"
                    )
                }
            }
        }
    }
}
