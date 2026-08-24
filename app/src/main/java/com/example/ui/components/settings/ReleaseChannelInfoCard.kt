package com.example.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReleaseChannel

/**
 * Tarjeta informativa de Canales de Lanzamiento y Versión de Vortex Studio.
 * Muestra los 4 canales configurados (Dev, Canary, Beta, Estable), identificadores de paquetes,
 * códigos de versión e indicadores de funciones experimentales.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReleaseChannelInfoCard(
    modifier: Modifier = Modifier,
    currentChannel: ReleaseChannel = ReleaseChannel.getCurrentChannel()
) {
    SettingsCard(
        title = "Canales de Versión y Distribución",
        subtitle = "Identificadores de paquete y ciclo de vida de versiones",
        icon = Icons.Default.Science,
        modifier = modifier.testTag("release_channel_info_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera con canal activo actual
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = currentChannel.getBadgeColor().copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentChannel.getBadgeColor().copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.VortexAppLogo(
                        size = 40.dp,
                        channel = currentChannel
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Canal Activo: ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = currentChannel.getBadgeColor(),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = " ${currentChannel.tag} ",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Versión: ${currentChannel.getFullVersionName()} (${currentChannel.getVersionCode()})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentChannel.fullPackageName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ecosistema de 4 Canales Multi-Instalación",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cada canal cuenta con su propio identificador de paquete independiente para permitir tener instaladas las 4 versiones a la vez en el mismo dispositivo.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista detallada de canales
            ReleaseChannel.getAllChannels().forEachIndexed { index, channel ->
                val isCurrent = channel == currentChannel
                ChannelItemRow(
                    channel = channel,
                    isCurrent = isCurrent
                )
                if (index < ReleaseChannel.getAllChannels().size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelItemRow(
    channel: ReleaseChannel,
    isCurrent: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            com.example.ui.components.VortexAppLogo(
                size = 28.dp,
                channel = channel,
                modifier = Modifier.padding(end = 6.dp)
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = channel.getBadgeColor().copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, channel.getBadgeColor().copy(alpha = 0.6f))
            ) {
                Text(
                    text = " ${channel.tag} ",
                    color = channel.getBadgeColor(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = channel.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isCurrent) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Instalado",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = channel.description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ID: ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = channel.fullPackageName,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "• Ver: ${channel.getFullVersionName()}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
