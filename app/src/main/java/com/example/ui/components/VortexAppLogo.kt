package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ReleaseChannel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Logo Vectorial Dinámico y Adaptativo de Vortex Studio.
 *
 * Representa un vórtice geométrico con aspas espirales convergentes hacia un punto central de captura.
 * Su color y acento se adaptan reactivamente al canal de lanzamiento (Dev, Canary, Beta, Estable).
 */
@Composable
fun VortexAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    channel: ReleaseChannel = ReleaseChannel.getCurrentChannel()
) {
    val badgeColor = channel.getBadgeColor()
    val secondaryColor = when (channel) {
        ReleaseChannel.DEV -> Color(0xFFE040FB)    // Magenta neón
        ReleaseChannel.CANARY -> Color(0xFFFFD54F) // Amarillo ámbar
        ReleaseChannel.BETA -> Color(0xFF00E5FF)   // Cian eléctrico
        ReleaseChannel.STABLE -> Color(0xFF69F0AE) // Verde menta brillante
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.82f)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Dibujar 3 aspas en espiral vórtice
            for (i in 0 until 3) {
                val startAngle = (i * 120.0) * (Math.PI / 180.0)
                val endAngle = ((i * 120.0) + 110.0) * (Math.PI / 180.0)

                val outerX = center.x + (radius * 0.88f) * cos(startAngle).toFloat()
                val outerY = center.y + (radius * 0.88f) * sin(startAngle).toFloat()

                val controlX = center.x + (radius * 0.95f) * cos(startAngle + 0.6).toFloat()
                val controlY = center.y + (radius * 0.95f) * sin(startAngle + 0.6).toFloat()

                val innerX = center.x + (radius * 0.36f) * cos(endAngle).toFloat()
                val innerY = center.y + (radius * 0.36f) * sin(endAngle).toFloat()

                val path = Path().apply {
                    moveTo(outerX, outerY)
                    quadraticTo(controlX, controlY, innerX, innerY)
                }

                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(badgeColor, secondaryColor),
                        start = Offset(outerX, outerY),
                        end = Offset(innerX, innerY)
                    ),
                    style = Stroke(width = radius * 0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            // Punto central de captura REC
            drawCircle(
                color = Color.White,
                radius = radius * 0.18f,
                center = center
            )
            drawCircle(
                color = badgeColor,
                radius = radius * 0.11f,
                center = center
            )
        }
    }
}
