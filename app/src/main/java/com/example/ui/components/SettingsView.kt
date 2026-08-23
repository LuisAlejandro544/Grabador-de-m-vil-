package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.RecordingConfig
import com.example.model.SceneOverlayType
import com.example.model.TouchColorOption
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.VtuberPreset
import com.example.model.VtuberSize
import com.example.model.WatermarkSize
import com.example.model.WatermarkType
import com.example.ui.components.settings.AudioSettingsCard
import com.example.ui.components.settings.CountdownSettingsCard
import com.example.ui.components.settings.FacecamSettingsCard
import com.example.ui.components.settings.FloatingBubbleSettingsCard
import com.example.ui.components.settings.GameModeCard
import com.example.ui.components.settings.ImageFormatSettingsCard
import com.example.ui.components.settings.NativeModulesStatusCard
import com.example.ui.components.settings.SceneOverlaySettingsCard
import com.example.ui.components.settings.TouchVisualizerSettingsCard
import com.example.ui.components.settings.VideoQualitySettingsCard
import com.example.ui.components.settings.VtuberSettingsCard
import com.example.ui.components.settings.WatermarkSettingsCard

/**
 * Vista modular de Ajustes de Grabación y Streaming.
 * Compone tarjetas desacopladas para cada aspecto de configuración.
 */
@Composable
fun SettingsView(
    config: RecordingConfig,
    onUpdateResolution: (VideoResolution) -> Unit,
    onUpdateFps: (VideoFps) -> Unit,
    onUpdateBitrate: (VideoBitrate) -> Unit,
    onUpdateBitrateMbps: (Int) -> Unit = {},
    onUpdateImageFormat: (com.example.model.ImageFormatOption) -> Unit = {},
    onUpdateImageQuality: (Int) -> Unit = {},
    onToggleImageWebpLossless: (Boolean) -> Unit = {},
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    onUpdateAudioSampleRate: (AudioSampleRate) -> Unit = {},
    onToggleFloatingVuMeter: (Boolean) -> Unit = {},
    onUpdateGameGain: (Float) -> Unit = {},
    onUpdateMicGain: (Float) -> Unit = {},
    onToggleNoiseGate: (Boolean) -> Unit = {},
    onToggleAudioDucking: (Boolean) -> Unit = {},
    onUpdateCountdown: (Int) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit = {},
    onToggleFacecam: (Boolean) -> Unit = {},
    onUpdateFacecamShape: (FacecamShape) -> Unit = {},
    onUpdateFacecamSize: (FacecamSize) -> Unit = {},
    onUpdateFacecamFps: (FacecamFps) -> Unit = {},
    onToggleFacecamCamera: () -> Unit = {},
    onToggleBeautyFilter: (Boolean) -> Unit = {},
    onToggleFacecamRgbBorder: (Boolean) -> Unit = {},
    onToggleVtuber: (Boolean) -> Unit = {},
    onUpdateVtuberPreset: (VtuberPreset) -> Unit = {},
    onUpdateVtuberSize: (VtuberSize) -> Unit = {},
    onUpdateVtuberSensitivity: (Float) -> Unit = {},
    onToggleVtuberBounce: (Boolean) -> Unit = {},
    onUpdateVtuberIdleImage: (String?) -> Unit = {},
    onUpdateVtuberTalkImage: (String?) -> Unit = {},
    onUpdateVtuberBlinkImage: (String?) -> Unit = {},
    onUpdateVtuberBlinkTalkImage: (String?) -> Unit = {},
    onToggleTouchVisualizer: (Boolean) -> Unit = {},
    onUpdateTouchVisualizerColor: (TouchColorOption) -> Unit = {},
    onToggleWatermark: (Boolean) -> Unit = {},
    onUpdateWatermarkType: (WatermarkType) -> Unit = {},
    onUpdateWatermarkText: (String) -> Unit = {},
    onUpdateWatermarkOpacity: (Float) -> Unit = {},
    onUpdateWatermarkSize: (WatermarkSize) -> Unit = {},
    onUpdateWatermarkColor: (TouchColorOption) -> Unit = {},
    onUpdateWatermarkImageUri: (String?) -> Unit = {},
    onToggleSceneOverlay: (Boolean) -> Unit = {},
    onUpdateSceneOverlayType: (SceneOverlayType) -> Unit = {},
    onUpdateSceneOverlayText: (String) -> Unit = {},
    onUpdateSceneOverlayOpacity: (Float) -> Unit = {},
    onUpdateSceneOverlayImageUri: (String?) -> Unit = {},
    onReopenOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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

        // Switch Maestro para Modo Juego
        GameModeCard(
            isGameMode = config.isGameMode,
            onToggleGameMode = onToggleGameMode
        )

        // Burbuja Flotante y Permiso de Superposición
        FloatingBubbleSettingsCard(
            showFloatingBubble = config.showFloatingBubble,
            onToggleFloatingBubble = onToggleFloatingBubble
        )

        // Guía de Inicio y Permisos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF181B24)
            ),
            border = BorderStroke(1.dp, Color(0xFF2B303C))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Guía de Inicio y Permisos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Revisa el tutorial explicativo de Vortex Studio y el estado de todos los permisos del sistema.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onReopenOnboarding,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Ver",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Marca de Agua / Logo Personalizado Superpuesto
        WatermarkSettingsCard(
            config = config,
            onToggleWatermark = onToggleWatermark,
            onTypeSelected = onUpdateWatermarkType,
            onTextChanged = onUpdateWatermarkText,
            onOpacityChanged = onUpdateWatermarkOpacity,
            onSizeSelected = onUpdateWatermarkSize,
            onColorSelected = onUpdateWatermarkColor,
            onImageSelected = onUpdateWatermarkImageUri
        )

        // Overlays de Escena Personalizados (Marcos PNG y Alertas Estáticas)
        SceneOverlaySettingsCard(
            config = config,
            onToggleSceneOverlay = onToggleSceneOverlay,
            onTypeSelected = onUpdateSceneOverlayType,
            onTextChanged = onUpdateSceneOverlayText,
            onOpacityChanged = onUpdateSceneOverlayOpacity,
            onImageSelected = onUpdateSceneOverlayImageUri
        )

        // Facecam (Cámara Flotante, Filtros y Formas)
        FacecamSettingsCard(
            showFacecam = config.showFacecam,
            isFrontCamera = config.isFrontCamera,
            beautyFilterEnabled = config.beautyFilterEnabled,
            facecamRgbBorder = config.facecamRgbBorder,
            facecamShape = config.facecamShape,
            facecamSize = config.facecamSize,
            facecamFps = config.facecamFps,
            onToggleFacecam = onToggleFacecam,
            onToggleFacecamCamera = onToggleFacecamCamera,
            onToggleBeautyFilter = onToggleBeautyFilter,
            onToggleFacecamRgbBorder = onToggleFacecamRgbBorder,
            onUpdateFacecamShape = onUpdateFacecamShape,
            onUpdateFacecamSize = onUpdateFacecamSize,
            onUpdateFacecamFps = onUpdateFacecamFps
        )

        // Avatar 2D / PNGtuber Reactivo (Modo VTuber)
        VtuberSettingsCard(
            config = config,
            onToggleVtuber = onToggleVtuber,
            onPresetSelected = onUpdateVtuberPreset,
            onSizeSelected = onUpdateVtuberSize,
            onSensitivityChanged = onUpdateVtuberSensitivity,
            onToggleBounce = onToggleVtuberBounce,
            onIdleImageSelected = onUpdateVtuberIdleImage,
            onTalkImageSelected = onUpdateVtuberTalkImage,
            onBlinkImageSelected = onUpdateVtuberBlinkImage,
            onBlinkTalkImageSelected = onUpdateVtuberBlinkTalkImage
        )

        // Visualizador de Toques Táctiles
        TouchVisualizerSettingsCard(
            showTouchVisualizer = config.showTouchVisualizer,
            touchVisualizerColor = config.touchVisualizerColor,
            onToggleTouchVisualizer = onToggleTouchVisualizer,
            onUpdateTouchVisualizerColor = onUpdateTouchVisualizerColor
        )

        // Calidad de Video (Resolución, FPS, Bitrate Personalizado de 1 a 12 Mbps)
        VideoQualitySettingsCard(
            resolution = config.resolution,
            fps = config.fps,
            bitrate = config.bitrate,
            bitrateMbps = config.bitrateMbps,
            onUpdateResolution = onUpdateResolution,
            onUpdateFps = onUpdateFps,
            onUpdateBitrate = onUpdateBitrate,
            onUpdateBitrateMbps = onUpdateBitrateMbps
        )

        // Formato y Compresión de Imagen / Screenshots (PNG, JPG %, WebP)
        ImageFormatSettingsCard(
            imageFormat = config.imageFormat,
            imageQuality = config.imageQuality,
            imageWebpLossless = config.imageWebpLossless,
            onUpdateImageFormat = onUpdateImageFormat,
            onUpdateImageQuality = onUpdateImageQuality,
            onToggleWebpLossless = onToggleImageWebpLossless
        )

        // Fuente de Audio y Frecuencia de Muestreo (Sample Rate)
        AudioSettingsCard(
            audioSource = config.audioSource,
            audioSampleRate = config.audioSampleRate,
            showFloatingVuMeter = config.showFloatingVuMeter,
            gameAudioGain = config.gameAudioGain,
            micAudioGain = config.micAudioGain,
            noiseGateEnabled = config.noiseGateEnabled,
            audioDuckingEnabled = config.audioDuckingEnabled,
            onUpdateAudioSource = onUpdateAudioSource,
            onUpdateAudioSampleRate = onUpdateAudioSampleRate,
            onToggleFloatingVuMeter = onToggleFloatingVuMeter,
            onUpdateGameGain = onUpdateGameGain,
            onUpdateMicGain = onUpdateMicGain,
            onToggleNoiseGate = onToggleNoiseGate,
            onToggleAudioDucking = onToggleAudioDucking
        )

        // Cuenta Atrás
        CountdownSettingsCard(
            countdownSeconds = config.countdownSeconds,
            onUpdateCountdown = onUpdateCountdown
        )

        // Monitor de Estado de Módulos Nativos (C++ & Rust)
        NativeModulesStatusCard()

        Spacer(modifier = Modifier.height(32.dp))
    }
}
