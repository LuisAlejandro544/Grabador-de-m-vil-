package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.AspectRatioFitMode
import com.example.editor.AspectRatioOption

/**
 * Selector interactivo de Relación de Aspecto (1-Tap Aspect Ratio) con modos de ajuste (Blur, Crop, Letterbox).
 */
@Composable
fun AspectRatioSelectorRow(
    selectedAspectRatio: AspectRatioOption,
    selectedFitMode: AspectRatioFitMode,
    onSelectAspectRatio: (AspectRatioOption) -> Unit,
    onSelectFitMode: (AspectRatioFitMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Selector de Chips de Aspect Ratio (1-Tap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aspect Ratio:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                modifier = Modifier.padding(end = 2.dp)
            )
            AspectRatioOption.values().forEach { option ->
                val isSelected = selectedAspectRatio == option
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectAspectRatio(option) },
                    label = {
                        Text(
                            text = option.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1B1B22),
                        selectedContainerColor = Color(0xFF00E676),
                        selectedLabelColor = Color.Black,
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("aspect_ratio_chip_${option.name.lowercase()}")
                )
            }
        }

        // 2. Modos de ajuste cuando no es ORIGINAL (Blur / Crop / Letterbox)
        AnimatedVisibility(visible = selectedAspectRatio != AspectRatioOption.ORIGINAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AspectRatioFitMode.values().forEach { mode ->
                    val isSelected = selectedFitMode == mode
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectFitMode(mode) }
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color(0xFF2979FF) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        color = if (isSelected) Color(0xFF1E2A4A) else Color(0xFF17171E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    AspectRatioFitMode.BLUR_BACKGROUND -> Icons.Default.BlurOn
                                    AspectRatioFitMode.CROP_FILL -> Icons.Default.AspectRatio
                                    AspectRatioFitMode.LETTERBOX_BLACK -> Icons.Default.Layers
                                },
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isSelected) Color(0xFF64B5F6) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.label,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
