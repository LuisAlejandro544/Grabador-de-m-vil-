package com.example.ui.components.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingConfig
import com.example.model.SceneOverlayType

@Composable
fun SceneOverlaySettingsCard(
    config: RecordingConfig,
    onToggleSceneOverlay: (Boolean) -> Unit,
    onTypeSelected: (SceneOverlayType) -> Unit,
    onTextChanged: (String) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onImageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri.toString())
        }
    }

    Card(
        modifier = modifier.testTag("scene_overlay_settings_card"),
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
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Overlays de Escena",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Marcos Gamer, alertas Live, carteles de pausa y PNGs",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.showSceneOverlay,
                    onCheckedChange = onToggleSceneOverlay,
                    modifier = Modifier.testTag("scene_overlay_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEC4899)
                    )
                )
            }

            AnimatedVisibility(
                visible = config.showSceneOverlay,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Selector de Tipo de Escena
                    Text(
                        text = "ESTILO DE OVERLAY DE ESCENA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF472B6)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SceneOverlayTypeOptionChip(
                                title = "Marco Neón",
                                isSelected = config.sceneOverlayType == SceneOverlayType.GAMER_NEON_FRAME,
                                onClick = { onTypeSelected(SceneOverlayType.GAMER_NEON_FRAME) },
                                modifier = Modifier.weight(1f)
                            )
                            SceneOverlayTypeOptionChip(
                                title = "Banner Redes",
                                isSelected = config.sceneOverlayType == SceneOverlayType.STREAMER_BANNER,
                                onClick = { onTypeSelected(SceneOverlayType.STREAMER_BANNER) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SceneOverlayTypeOptionChip(
                                title = "🔴 Badge Live",
                                isSelected = config.sceneOverlayType == SceneOverlayType.LIVE_BADGE,
                                onClick = { onTypeSelected(SceneOverlayType.LIVE_BADGE) },
                                modifier = Modifier.weight(1f)
                            )
                            SceneOverlayTypeOptionChip(
                                title = "⏸️ Pausa Standby",
                                isSelected = config.sceneOverlayType == SceneOverlayType.STANDBY_PAUSE,
                                onClick = { onTypeSelected(SceneOverlayType.STANDBY_PAUSE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SceneOverlayTypeOptionChip(
                                title = "🖼️ PNG Personalizado",
                                isSelected = config.sceneOverlayType == SceneOverlayType.CUSTOM_IMAGE,
                                onClick = { onTypeSelected(SceneOverlayType.CUSTOM_IMAGE) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (config.sceneOverlayType == SceneOverlayType.STREAMER_BANNER ||
                        config.sceneOverlayType == SceneOverlayType.STANDBY_PAUSE
                    ) {
                        OutlinedTextField(
                            value = config.sceneOverlayText,
                            onValueChange = onTextChanged,
                            label = { Text("Texto del Banner / Alerta") },
                            placeholder = { Text("🔴 EN VIVO | @TuCanal") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scene_overlay_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFEC4899),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (config.sceneOverlayType == SceneOverlayType.CUSTOM_IMAGE) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (config.sceneOverlayImageUri != null) "Marco cargado correctamente" else "Selecciona un archivo PNG transparente de pantalla completa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cargar Marco PNG")
                                }
                                if (config.sceneOverlayImageUri != null) {
                                    Button(
                                        onClick = { onImageSelected(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // Opacidad / Transparencia
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Opacidad del Overlay",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(config.sceneOverlayOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF472B6)
                            )
                        }
                        Slider(
                            value = config.sceneOverlayOpacity,
                            onValueChange = onOpacityChanged,
                            valueRange = 0.2f..1.0f,
                            steps = 16,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEC4899),
                                activeTrackColor = Color(0xFFEC4899)
                            )
                        )
                    }

                    // Vista Previa de la Escena
                    Text(
                        text = "VISTA PREVIA DEL ENTORNO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    val accentColor = Color(config.touchVisualizerColor.colorInt)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(config.sceneOverlayOpacity)
                        ) {
                            when (config.sceneOverlayType) {
                                SceneOverlayType.GAMER_NEON_FRAME -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(2.dp, accentColor, RoundedCornerShape(8.dp))
                                    )
                                }
                                SceneOverlayType.STREAMER_BANNER -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color(0xEE1E293B), RoundedCornerShape(6.dp))
                                            .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = config.sceneOverlayText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                SceneOverlayType.LIVE_BADGE -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xCCEF4444), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔴 LIVE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                SceneOverlayType.STANDBY_PAUSE -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color(0xDD0F172A), RoundedCornerShape(8.dp))
                                            .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⏸️ PAUSA - VOLVEMOS",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Marco Activo",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneOverlayTypeOptionChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(title) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFDB2777),
            selectedLabelColor = Color.White
        )
    )
}
