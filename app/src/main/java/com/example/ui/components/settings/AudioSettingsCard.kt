package com.example.ui.components.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import kotlin.math.roundToInt

@Composable
fun AudioSettingsCard(
    audioSource: AudioSourceType,
    audioSampleRate: AudioSampleRate,
    showFloatingVuMeter: Boolean = false,
    gameAudioGain: Float = 1.0f,
    micAudioGain: Float = 1.0f,
    noiseGateEnabled: Boolean = false,
    audioDuckingEnabled: Boolean = false,
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    onUpdateAudioSampleRate: (AudioSampleRate) -> Unit,
    onToggleFloatingVuMeter: (Boolean) -> Unit = {},
    onUpdateGameGain: (Float) -> Unit = {},
    onUpdateMicGain: (Float) -> Unit = {},
    onToggleNoiseGate: (Boolean) -> Unit = {},
    onToggleAudioDucking: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Fuente de Audio
        SettingsCard(
            title = "Fuente de Audio",
            subtitle = "Elige si deseas capturar el sonido interno del juego, tu voz o grabar en silencio",
            icon = Icons.Default.Mic
        ) {
            AudioSourceType.values().forEach { source ->
                val description = when (source) {
                    AudioSourceType.INTERNAL_AND_MIC -> "Graba el sonido del juego y tu voz simultáneamente. ¡Puedes mezclar ganancias en tiempo real desde el Vúmetro flotante!"
                    AudioSourceType.INTERNAL_GAME -> "Graba exclusivamente los efectos y música del juego sin registrar tu voz ni ruidos del ambiente"
                    AudioSourceType.MIC -> "Captura tus comentarios, voz en vivo y audio exterior mediante el micrófono"
                    AudioSourceType.NONE -> "Graba únicamente el video sin ninguna pista de audio (ahorro de espacio)"
                }
                SettingsRadioItem(
                    title = source.label,
                    description = description,
                    selected = audioSource == source,
                    onClick = { onUpdateAudioSource(source) },
                    testTag = "audio_option_${source.name}"
                )
            }
        }

        // 2. Mezclador de Audio Pro & Vúmetro Flotante
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (showFloatingVuMeter) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Vúmetro Flotante & Mezclador Pro",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Muestra barras LED de decibelios (dB) y faders de volumen en tiempo real sobre los juegos",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showFloatingVuMeter,
                        onCheckedChange = onToggleFloatingVuMeter,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("floating_vumeter_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                // Control Ganancia Juego
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ganancia de Audio del Juego",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${(gameAudioGain * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = gameAudioGain,
                    onValueChange = onUpdateGameGain,
                    valueRange = 0.0f..2.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("game_gain_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Control Ganancia Voz / Micrófono
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ganancia de Micrófono / Voz",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${(micAudioGain * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = micAudioGain,
                    onValueChange = onUpdateMicGain,
                    valueRange = 0.0f..2.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("mic_gain_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Filtros DSP Pro: Puerta de Ruido & Audio Ducking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Puerta de Ruido DSP (Noise Gate)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Elimina silenciosamente ruidos de fondo y estática cuando no estés hablando (-42 dB)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = noiseGateEnabled,
                        onCheckedChange = onToggleNoiseGate,
                        modifier = Modifier.testTag("noise_gate_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Auto-Ducking de Audio (Atenuación)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reduce automáticamente el volumen del juego un 65% cuando hablas para que tu voz destaque",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = audioDuckingEnabled,
                        onCheckedChange = onToggleAudioDucking,
                        modifier = Modifier.testTag("audio_ducking_switch")
                    )
                }
            }
        }

        // 3. Frecuencia de Muestreo de Audio (Sample Rate)
        SettingsCard(
            title = "Frecuencia de Muestreo de Audio (Sample Rate)",
            subtitle = "Controla la resolución acústica y fidelidad de los canales de audio",
            icon = Icons.Default.GraphicEq
        ) {
            AudioSampleRate.values().forEach { rate ->
                val desc = when (rate) {
                    AudioSampleRate.RATE_32000 -> "32.000 Hz - Bajo consumo de CPU y archivo más ligero"
                    AudioSampleRate.RATE_44100 -> "44.100 Hz - Calidad de sonido CD estándar"
                    AudioSampleRate.RATE_48000 -> "48.000 Hz - Estándar broadcast profesional para Android y gaming (Recomendado)"
                    AudioSampleRate.RATE_96000 -> "96.000 Hz - Calidad Hi-Res de alta fidelidad para captura acústica avanzada"
                }
                SettingsRadioItem(
                    title = rate.label,
                    description = desc,
                    selected = audioSampleRate == rate,
                    onClick = { onUpdateAudioSampleRate(rate) },
                    testTag = "sample_rate_option_${rate.name}"
                )
            }
        }
    }
}
