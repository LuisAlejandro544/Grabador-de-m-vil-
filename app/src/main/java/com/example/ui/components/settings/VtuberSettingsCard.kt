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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingConfig
import com.example.model.VtuberPreset
import com.example.model.VtuberSize

/**
 * Tarjeta de configuración para el Avatar 2D Reactivo / PNGtuber (Modo VTuber).
 * Permite seleccionar presets vectoriales o subir imágenes PNG transparentes de 4 estados.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VtuberSettingsCard(
    config: RecordingConfig,
    onToggleVtuber: (Boolean) -> Unit,
    onPresetSelected: (VtuberPreset) -> Unit,
    onSizeSelected: (VtuberSize) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onToggleBounce: (Boolean) -> Unit,
    onIdleImageSelected: (String?) -> Unit,
    onTalkImageSelected: (String?) -> Unit,
    onBlinkImageSelected: (String?) -> Unit,
    onBlinkTalkImageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val vtuberPurple = Color(0xFFA855F7)
    val vtuberPurpleLight = Color(0xFFC084FC)

    // Launchers para cada ranura de imagen
    val idlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onIdleImageSelected(uri.toString())
    }

    val talkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onTalkImageSelected(uri.toString())
    }

    val blinkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onBlinkImageSelected(uri.toString())
    }

    val blinkTalkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onBlinkTalkImageSelected(uri.toString())
    }

    Card(
        modifier = modifier.testTag("vtuber_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera con Switch principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(vtuberPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎭", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Avatar 2D / PNGtuber",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Avatar flotante reactivo a tu voz en tiempo real",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.showVtuber,
                    onCheckedChange = onToggleVtuber,
                    modifier = Modifier.testTag("vtuber_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = vtuberPurple
                    )
                )
            }

            AnimatedVisibility(
                visible = config.showVtuber,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 1. Selector de Modelo / Preset
                    Text(
                        text = "MODELO DE AVATAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = vtuberPurple
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VtuberPreset.entries.forEach { preset ->
                            val isSelected = config.vtuberPreset == preset
                            val iconEmoji = when (preset) {
                                VtuberPreset.CYBER_CAT -> "🐱"
                                VtuberPreset.ANIME_AOI -> "🦊"
                                VtuberPreset.PIXEL_SLIME -> "🤖"
                                VtuberPreset.CUSTOM -> "🎨"
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { onPresetSelected(preset) },
                                label = { Text("$iconEmoji ${preset.label}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vtuberPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // 2. Selector de Tamaño
                    Text(
                        text = "TAMAÑO DEL AVATAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = vtuberPurple
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VtuberSize.entries.forEach { sizeOption ->
                            val isSelected = config.vtuberSize == sizeOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSizeSelected(sizeOption) },
                                label = { Text(sizeOption.label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vtuberPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // 3. Sensibilidad del Micrófono
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SENSIBILIDAD DE VOZ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = vtuberPurple
                        )
                        Text(
                            text = "${(config.vtuberSensitivity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = vtuberPurple
                        )
                    }

                    Slider(
                        value = config.vtuberSensitivity,
                        onValueChange = onSensitivityChanged,
                        valueRange = 0.05f..0.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = vtuberPurple,
                            activeTrackColor = vtuberPurple,
                            inactiveTrackColor = vtuberPurple.copy(alpha = 0.25f)
                        )
                    )

                    // 4. Animación de Rebote (Bounce)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rebote Elástico al Hablar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Efecto squash & stretch dinámico al detectar voz",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.vtuberBounceEnabled,
                            onCheckedChange = onToggleBounce,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = vtuberPurple
                            )
                        )
                    }

                    // 5. Configuración de PNGs Personalizados (cuando el preset es CUSTOM)
                    if (config.vtuberPreset == VtuberPreset.CUSTOM) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Text(
                            text = "IMÁGENES PNG DEL AVATAR (4 ESTADOS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = vtuberPurple
                        )

                        // Banner de ayuda informativa sobre fallback
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = vtuberPurpleLight,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "¿Tienes solo 2 imágenes?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Solo necesitas 'Reposo' y 'Hablando'. Si no tienes los parpadeos, la app usará automáticamente las imágenes con ojos abiertos sin errores.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        // Ranuras de Carga de Imágenes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. Reposo (Boca cerrada + Ojos abiertos)
                            PngSlotRow(
                                title = "1. Reposo (Boca Cerrada)",
                                subtitle = "Ojos abiertos / En silencio (Obligatorio)",
                                emoji = "👁️🤐",
                                uri = config.vtuberIdleImageUri,
                                onSelectClicked = { idlePickerLauncher.launch("image/*") },
                                onClearClicked = { onIdleImageSelected(null) }
                            )

                            // 2. Hablando (Boca abierta + Ojos abiertos)
                            PngSlotRow(
                                title = "2. Hablando (Boca Abierta)",
                                subtitle = "Ojos abiertos / Al hablar (Obligatorio)",
                                emoji = "👁️🗣️",
                                uri = config.vtuberTalkImageUri,
                                onSelectClicked = { talkPickerLauncher.launch("image/*") },
                                onClearClicked = { onTalkImageSelected(null) }
                            )

                            // 3. Parpadeo Reposo (Boca cerrada + Ojos cerrados)
                            PngSlotRow(
                                title = "3. Parpadeo Reposo",
                                subtitle = "Ojos cerrados / En silencio (Opcional)",
                                emoji = "😌🤐",
                                uri = config.vtuberBlinkImageUri,
                                onSelectClicked = { blinkPickerLauncher.launch("image/*") },
                                onClearClicked = { onBlinkImageSelected(null) }
                            )

                            // 4. Parpadeo Hablando (Boca abierta + Ojos cerrados)
                            PngSlotRow(
                                title = "4. Parpadeo Hablando",
                                subtitle = "Ojos cerrados / Al hablar (Opcional)",
                                emoji = "😆🗣️",
                                uri = config.vtuberBlinkTalkImageUri,
                                onSelectClicked = { blinkTalkPickerLauncher.launch("image/*") },
                                onClearClicked = { onBlinkTalkImageSelected(null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PngSlotRow(
    title: String,
    subtitle: String,
    emoji: String,
    uri: String?,
    onSelectClicked: () -> Unit,
    onClearClicked: () -> Unit
) {
    val isLoaded = !uri.isNullOrEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isLoaded) Color(0xFF132F24) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (isLoaded) Color(0xFF10B981) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isLoaded) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Cargada",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cambiar",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSelectClicked() }
                            .padding(4.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = onSelectClicked,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFA855F7)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir PNG", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
