package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla principal del flujo de bienvenida (Onboarding) y configuración de permisos de Vortex Studio.
 * Aparece la primera vez que el usuario ingresa a la app, guiándolo a través de las capacidades
 * del estudio gamer y configurando los accesos requeridos.
 */
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 4 // 3 diapositivas explicativas + 1 centro de permisos

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1117),
                        Color(0xFF090A0E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior del Onboarding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón Atrás
                if (currentPage > 0) {
                    IconButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }

                // Indicador de Pasos / Puntos de progreso
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = i == currentPage
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF2C3240)
                                )
                        )
                    }
                }

                // Botón Saltar (lleva a la página de permisos)
                if (currentPage < totalPages - 1) {
                    TextButton(
                        onClick = { currentPage = totalPages - 1 }
                    ) {
                        Text(
                            text = "Saltar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // Contenido dinámico con animación horizontal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboarding_step_transition"
                ) { targetPage ->
                    when (targetPage) {
                        0 -> OnboardingStepPage(
                            heroIcon = Icons.Default.Videocam,
                            accentColor = Color(0xFF00E5FF),
                            gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF00838F)),
                            badgeText = "Estudio Gamer 60 FPS",
                            title = "Grabación Ultra a 60 FPS",
                            subtitle = "Captura tus partidas en 1080p sin lag ni pérdida de fotogramas, con control granular de Bitrate.",
                            features = listOf(
                                OnboardingFeatureItem(
                                    title = "Grabación 60 FPS Fluida",
                                    description = "Pipeline optimizado por hardware que no sobrecalienta el teléfono ni ralentiza el juego."
                                ),
                                OnboardingFeatureItem(
                                    title = "Bitrate Granular (1 a 12 Mbps)",
                                    description = "Ajusta la calidad según el almacenamiento disponible de tu móvil con cálculo de tiempo restante."
                                ),
                                OnboardingFeatureItem(
                                    title = "Protección Anti-Corrupción",
                                    description = "Escritura segura del átomo MP4 ante batería baja o cierres imprevistos: nunca perderás una victoria."
                                )
                            ),
                            tags = listOf("60 FPS", "1080p", "Bitrate 1-12 Mbps", "Anti-Crash")
                        )

                        1 -> OnboardingStepPage(
                            heroIcon = Icons.Default.MicExternalOn,
                            accentColor = Color(0xFF7C4DFF),
                            gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF512DA8)),
                            badgeText = "Herramientas de Streaming",
                            title = "Facecam Pro & Audio DSP",
                            subtitle = "Herramientas de nivel profesional con Facecam RGB, avatar VTuber reactivo y mezclador de audio.",
                            features = listOf(
                                OnboardingFeatureItem(
                                    title = "Facecam Pro con Borde RGB",
                                    description = "Cámara frontal o trasera ajustable con formas (círculo, cuadrado, píldora) y filtros de belleza."
                                ),
                                OnboardingFeatureItem(
                                    title = "Avatar 2D / PNGtuber Reactivo",
                                    description = "Protege tu privacidad con un avatar animado que abre la boca y reacciona a tu voz al hablar."
                                ),
                                OnboardingFeatureItem(
                                    title = "Audio DSP & Ducking Automático",
                                    description = "Baja automáticamente el volumen del juego cuando hablas por el micro y elimina ruidos de fondo."
                                )
                            ),
                            tags = listOf("Facecam RGB", "PNGtuber 2D", "Audio Ducking", "Noise Gate", "Vúmetro")
                        )

                        2 -> OnboardingStepPage(
                            heroIcon = Icons.Default.ContentCut,
                            accentColor = Color(0xFFFF5722),
                            gradientColors = listOf(Color(0xFFFF5722), Color(0xFFD84315)),
                            badgeText = "Editor Rápido & Redes",
                            title = "Conversor 9:16 & Recorte",
                            subtitle = "Crea contenido para TikTok, Shorts y Reels directamente desde tu teléfono en segundos.",
                            features = listOf(
                                OnboardingFeatureItem(
                                    title = "Conversión a Vertical 9:16",
                                    description = "Transforma jugadas horizontales a formato vertical con fondo desenfocado cinemático en 1-tap."
                                ),
                                OnboardingFeatureItem(
                                    title = "Recorte & División Instantánea",
                                    description = "Cortes sin pérdida de calidad (Stream-Copy) en milisegundos sin renderizados pesados."
                                ),
                                OnboardingFeatureItem(
                                    title = "Extractor de Miniaturas HD",
                                    description = "Genera portadas y miniaturas nítidas para tus publicaciones en redes sociales."
                                )
                            ),
                            tags = listOf("TikTok / Shorts 9:16", "Stream-Copy", "Split & Trim", "Miniaturas HD")
                        )

                        3 -> PermissionsSetupPage(
                            onCompleteOnboarding = onCompleteOnboarding
                        )
                    }
                }
            }

            // Barra de acción inferior para las diapositivas 0, 1 y 2
            if (currentPage < totalPages - 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0D1117)
                        )
                    ) {
                        Text(
                            text = if (currentPage == totalPages - 2) "Configurar Permisos" else "Siguiente",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
