package com.example.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CountdownSettingsCard(
    countdownSeconds: Int,
    onUpdateCountdown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = "Cuenta Atrás al Iniciar",
        subtitle = "Tiempo para prepararte o abrir tu juego antes de grabar",
        icon = Icons.Default.AvTimer
    ) {
        listOf(0, 3, 5).forEach { seconds ->
            val label = if (seconds == 0) "Sin cuenta atrás (Inmediato)" else "$seconds Segundos"
            SettingsRadioItem(
                title = label,
                description = if (seconds == 0) "Inicia al instante" else "Muestra temporizador antes de comenzar",
                selected = countdownSeconds == seconds,
                onClick = { onUpdateCountdown(seconds) },
                testTag = "countdown_option_$seconds"
            )
        }
    }
}
