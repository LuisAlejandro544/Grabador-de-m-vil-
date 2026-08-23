package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.RecordedVideo

/**
 * Barra superior del Editor de Video con botón de cierre, metadatos y botón de exportación/recorte.
 */
@Composable
fun VideoEditorHeader(
    video: RecordedVideo,
    selectedAspectRatio: AspectRatioOption,
    selectedFitMode: AspectRatioFitMode,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .testTag("editor_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Vortex Video Studio Pro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (selectedAspectRatio == AspectRatioOption.ORIGINAL)
                        "${video.width}x${video.height} • ${video.formattedDuration()}"
                    else
                        "Formato: ${selectedAspectRatio.label} • ${selectedFitMode.label}",
                    fontSize = 11.sp,
                    color = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color(0xFF00E676) else Color(0xFF9E9EA4)
                )
            }
        }

        // Botón de Guardar / Exportar Clip
        Button(
            onClick = onExportClick,
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color(0xFF2979FF) else Color(0xFF00E676),
                contentColor = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color.White else Color.Black
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("editor_export_trim_button")
        ) {
            Icon(
                imageVector = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Icons.Default.FileDownload else Icons.Default.ContentCut,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (selectedAspectRatio != AspectRatioOption.ORIGINAL) "Exportar ${selectedAspectRatio.label}" else "Guardar Clip",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
