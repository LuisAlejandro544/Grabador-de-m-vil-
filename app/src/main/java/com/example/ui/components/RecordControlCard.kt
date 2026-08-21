package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig
import com.example.model.RecordingStatus
import com.example.ui.theme.RecordActiveAmber
import com.example.ui.theme.RecordActiveGreen
import com.example.ui.theme.RecordActiveRed
import java.util.Locale

@Composable
fun RecordControlCard(
    config: RecordingConfig,
    status: RecordingStatus,
    elapsedSeconds: Int,
    countdownNumber: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = status == RecordingStatus.RECORDING
    val isPaused = status == RecordingStatus.PAUSED
    val isCountingDown = status == RecordingStatus.COUNTDOWN

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_control_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode selector: Juego vs Pantalla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeToggleItem(
                    title = "Modo Juegos",
                    subtitle = "60 FPS • Alta Calidad",
                    icon = Icons.Default.SportsEsports,
                    isSelected = config.isGameMode,
                    onClick = { if (!isRecording && !isPaused) onToggleGameMode(true) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                ModeToggleItem(
                    title = "Estándar",
                    subtitle = "30 FPS • Normal",
                    icon = Icons.Default.Videocam,
                    isSelected = !config.isGameMode,
                    onClick = { if (!isRecording && !isPaused) onToggleGameMode(false) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Central Interactive Record Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer subtle glowing aura when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(RecordActiveRed.copy(alpha = 0.18f))
                    )
                }

                // Inner Main Circular Button
                val buttonBg = when {
                    isRecording -> Brush.linearGradient(listOf(RecordActiveRed, Color(0xFFB71C1C)))
                    isPaused -> Brush.linearGradient(listOf(RecordActiveAmber, Color(0xFFD97706)))
                    isCountingDown -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                    else -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C)))
                }

                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = !isCountingDown,
                            onClick = {
                                if (isRecording || isPaused) {
                                    onStopClick()
                                } else {
                                    onStartClick()
                                }
                            }
                        )
                        .testTag("main_record_button"),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(buttonBg)
                            .border(
                                width = 3.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCountingDown) {
                            Text(
                                text = "$countdownNumber",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else if (isRecording || isPaused) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener grabación",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "DETENER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = "Iniciar grabación",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "GRABAR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer & Status Badge
            if (isRecording || isPaused) {
                val formattedTime = formatSeconds(elapsedSeconds)
                Text(
                    text = formattedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("recording_timer_text")
                )

                Spacer(modifier = Modifier.height(6.dp))

                StatusChip(
                    text = if (isPaused) "EN PAUSA" else "GRABANDO AHORA",
                    color = if (isPaused) RecordActiveAmber else RecordActiveRed
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pause / Resume and Stop quick action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPaused) {
                        FilledTonalButton(
                            onClick = onResumeClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = RecordActiveGreen.copy(alpha = 0.15f),
                                contentColor = RecordActiveGreen
                            ),
                            modifier = Modifier.testTag("resume_recording_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reanudar", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onPauseClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("pause_recording_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = onStopClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecordActiveRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("stop_recording_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terminar", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (isCountingDown) {
                Text(
                    text = "Iniciando en $countdownNumber segundos...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancelCountdown,
                    modifier = Modifier.testTag("cancel_countdown_button")
                ) {
                    Text("Cancelar Cuenta Atrás")
                }
            } else {
                Text(
                    text = "Listo para Grabar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Presiona el botón para iniciar la captura de pantalla o juegos",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs badges bar (Resolution, FPS, Audio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpecBadge(label = config.resolution.label.split(" ")[0])
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpecBadge(label = "${config.fps.fps} FPS")
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, label) = when (config.audioSource) {
                        AudioSourceType.INTERNAL_AND_MIC -> Pair(Icons.Default.Mic, "Juego + Voz")
                        AudioSourceType.INTERNAL_GAME -> Pair(Icons.Default.SportsEsports, "Audio Juego")
                        AudioSourceType.MIC -> Pair(Icons.Default.Mic, "Micrófono")
                        AudioSourceType.NONE -> Pair(Icons.Default.MicOff, "Mudo")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SpecBadge(label = label)
                }
            }
        }
    }
}

@Composable
private fun ModeToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun SpecBadge(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatSeconds(totalSec: Int): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
