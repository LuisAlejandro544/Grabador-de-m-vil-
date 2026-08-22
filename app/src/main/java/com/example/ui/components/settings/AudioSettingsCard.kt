package com.example.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType

@Composable
fun AudioSettingsCard(
    audioSource: AudioSourceType,
    audioSampleRate: AudioSampleRate,
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    onUpdateAudioSampleRate: (AudioSampleRate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsCard(
            title = "Fuente de Audio",
            subtitle = "Elige si deseas capturar el sonido interno del juego, tu voz o grabar en silencio",
            icon = Icons.Default.Mic
        ) {
            AudioSourceType.values().forEach { source ->
                val description = when (source) {
                    AudioSourceType.INTERNAL_AND_MIC -> "Graba el sonido del juego y tu voz simultáneamente. ¡Puedes silenciar o activar tu voz en tiempo real desde la burbuja flotante!"
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

        // Advanced Sample Rate Card
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
