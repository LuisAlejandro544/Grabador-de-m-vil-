package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution

@Composable
fun SettingsView(
    config: RecordingConfig,
    onUpdateResolution: (VideoResolution) -> Unit,
    onUpdateFps: (VideoFps) -> Unit,
    onUpdateBitrate: (VideoBitrate) -> Unit,
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    onUpdateCountdown: (Int) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de Grabación",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Game Mode Master Switch
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (config.isGameMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Optimización para Juegos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ajusta automáticamente 60 FPS y 12 Mbps para máxima fluidez",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = config.isGameMode,
                    onCheckedChange = onToggleGameMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("game_mode_switch")
                )
            }
        }

        // Floating Controls Bubble Switch & Permission Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (config.showFloatingBubble) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
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
                            imageVector = Icons.Default.ControlCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Burbuja de Control Flotante",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Muestra cronómetro en vivo, pausa, reanudación y parada sobre los juegos",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = config.showFloatingBubble,
                        onCheckedChange = onToggleFloatingBubble,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("floating_bubble_switch")
                    )
                }

                if (config.showFloatingBubble && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasOverlayPermission) "Permiso de superposición activo" else "Requiere permiso para mostrarse sobre juegos",
                                fontSize = 12.sp,
                                color = if (hasOverlayPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!hasOverlayPermission) {
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("grant_overlay_perm_btn")
                            ) {
                                Text("Activar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

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
                    selected = config.resolution == res,
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
            VideoFps.values().forEach { fps ->
                SettingsRadioItem(
                    title = fps.label,
                    description = if (fps == VideoFps.FPS_60) "Recomendado para gameplays" else "Ahorra batería y almacenamiento",
                    selected = config.fps == fps,
                    onClick = { onUpdateFps(fps) },
                    testTag = "fps_option_${fps.name}"
                )
            }
        }

        // Bitrate Section
        SettingsCard(
            title = "Tasa de Bits (Calidad)",
            subtitle = "Controla el balance entre calidad de imagen y peso del archivo",
            icon = Icons.Default.Storage
        ) {
            VideoBitrate.values().forEach { bitrate ->
                SettingsRadioItem(
                    title = bitrate.label,
                    description = "${bitrate.bps / 1_000_000} Megabits por segundo",
                    selected = config.bitrate == bitrate,
                    onClick = { onUpdateBitrate(bitrate) },
                    testTag = "bitrate_option_${bitrate.name}"
                )
            }
        }

        // Audio Source Section
        SettingsCard(
            title = "Fuente de Audio",
            subtitle = "Elige si deseas capturar el sonido interno del juego, tu voz o grabar en silencio",
            icon = Icons.Default.Mic
        ) {
            AudioSourceType.values().forEach { source ->
                val description = when (source) {
                    AudioSourceType.INTERNAL_GAME -> "Graba exclusivamente los efectos y música del juego sin registrar tu voz ni ruidos del ambiente"
                    AudioSourceType.MIC -> "Captura tus comentarios, voz en vivo y audio exterior mediante el micrófono"
                    AudioSourceType.NONE -> "Graba únicamente el video sin ninguna pista de audio (ahorro de espacio)"
                }
                SettingsRadioItem(
                    title = source.label,
                    description = description,
                    selected = config.audioSource == source,
                    onClick = { onUpdateAudioSource(source) },
                    testTag = "audio_option_${source.name}"
                )
            }
        }

        // Countdown Section
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
                    selected = config.countdownSeconds == seconds,
                    onClick = { onUpdateCountdown(seconds) },
                    testTag = "countdown_option_$seconds"
                )
            }
        }

        // Native OBS Core Engine Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Módulos Nativos OBS (C++ & Rust)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estructuras de bajo nivel listas para composición de escenas y streaming en vivo.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Motor Gráfico C++ (GLES3):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = com.example.nativecore.NativeOBSBridge.getEngineVersion(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Red & Streaming Rust (RTMP/SRT):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = com.example.nativecore.NativeRustNetwork.getEngineVersion(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Motor FFmpeg Puro (C/C++ NDK):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = com.example.nativecore.NativeFFmpegBridge.getFFmpegVersion(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRadioItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
