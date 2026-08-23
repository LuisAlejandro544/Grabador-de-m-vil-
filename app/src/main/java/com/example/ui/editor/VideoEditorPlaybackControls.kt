package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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

/**
 * Barra de controles de transporte de reproducción y herramientas rápidas (Dividir ✂️ y Foto HD).
 */
@Composable
fun VideoEditorPlaybackControls(
    isPlaying: Boolean,
    onRewind1s: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onForward1s: () -> Unit,
    onSplitClick: () -> Unit,
    onExtractThumbnailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Retroceder 1s
        IconButton(
            onClick = onRewind1s,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E24))
        ) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "-1s",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Play/Pause
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF2979FF))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Avanzar 1s
        IconButton(
            onClick = onForward1s,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E24))
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "+1s",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Botón DIVIDIR AQUÍ (Split Tool)
        Button(
            onClick = onSplitClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF37474F),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("editor_split_button")
        ) {
            Icon(
                imageVector = Icons.Default.CallSplit,
                contentDescription = "Dividir",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Dividir ✂️",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Botón de Extraer Miniatura HD
        Button(
            onClick = onExtractThumbnailClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("editor_extract_thumb_button")
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = Color(0xFFFFD54F)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Foto HD",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
