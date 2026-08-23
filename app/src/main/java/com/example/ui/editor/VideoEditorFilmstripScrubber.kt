package com.example.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Línea de tiempo visual interactiva: Filmstrip con miniaturas, sombreado de límites,
 * playhead en tiempo real, selector deslizable dual (RangeSlider) y marcadores In/Playhead/Out.
 */
@Composable
fun VideoEditorFilmstripScrubber(
    startMs: Long,
    endMs: Long,
    currentPlaybackMs: Long,
    durationMs: Long,
    filmstripBitmaps: List<Bitmap>,
    sliderRange: ClosedFloatingPointRange<Float>,
    onSliderRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    formatMs: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Línea de Tiempo (Recorte / Selección)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Duración: ${formatMs(endMs - startMs)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filmstrip con cabezales de recorte interactivos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                // Tira de miniaturas de fondo
                Row(modifier = Modifier.fillMaxSize()) {
                    if (filmstripBitmaps.isNotEmpty()) {
                        filmstripBitmaps.forEach { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF22222B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cargando fotogramas...",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Sombra fuera del rango recortado
                val leftRatio = (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val rightRatio = ((durationMs - endMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                Row(modifier = Modifier.fillMaxSize()) {
                    if (leftRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(leftRatio.coerceAtLeast(0.001f))
                                .background(Color.Black.copy(alpha = 0.65f))
                        )
                    }

                    // Zona activa seleccionada con marco verde Neón
                    val activeRatio = ((endMs - startMs).toFloat() / durationMs.toFloat()).coerceIn(0.01f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(activeRatio)
                            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                    )

                    if (rightRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(rightRatio.coerceAtLeast(0.001f))
                                .background(Color.Black.copy(alpha = 0.65f))
                        )
                    }
                }

                // Indicador de aguja de reproducción actual (Playhead)
                val playheadRatio = (currentPlaybackMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(playheadRatio)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Selector de rango deslizable dual
            RangeSlider(
                value = sliderRange,
                onValueChange = onSliderRangeChange,
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E676),
                    activeTrackColor = Color(0xFF00E676),
                    inactiveTrackColor = Color(0xFF2E2E38)
                ),
                modifier = Modifier.testTag("editor_range_slider")
            )

            // Marcadores de tiempo inferior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "In: ${formatMs(startMs)}",
                    fontSize = 11.sp,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Playhead: ${formatMs(currentPlaybackMs)}",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Out: ${formatMs(endMs)}",
                    fontSize = 11.sp,
                    color = Color(0xFFFF5252),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
