package com.example.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.AudioSourceType

@Composable
fun AudioSettingsCard(
    audioSource: AudioSourceType,
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    modifier: Modifier = Modifier
) {
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
}
