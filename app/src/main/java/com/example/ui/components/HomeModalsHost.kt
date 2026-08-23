package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.model.RecordedVideo
import com.example.ui.UiState
import com.example.ui.RecordViewModel
import com.example.ui.editor.VideoEditorDialog

/**
 * Contenedor desacoplado para la gestión y visualización de diálogos modales en [HomeScreen].
 * Orquesta de forma modular el reproductor de video integrado y el mini-editor de video.
 */
@Composable
fun HomeModalsHost(
    uiState: UiState,
    viewModel: RecordViewModel,
    context: Context
) {
    // Modal de reproducción de video integrado
    uiState.selectedVideoForPlay?.let { video: RecordedVideo ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.closePlayer() },
            onShare = { viewModel.shareVideo(context, it) },
            onEdit = { viewModel.openEditor(it) }
        )
    }

    // Modal de Mini Editor de Video estilo CapCut
    uiState.selectedVideoForEdit?.let { video: RecordedVideo ->
        VideoEditorDialog(
            video = video,
            onDismiss = { viewModel.closeEditor() },
            onVideoEdited = { editedFile ->
                viewModel.onVideoEdited(editedFile)
            },
            onThumbnailExtracted = { thumbFile ->
                viewModel.onThumbnailExtracted(thumbFile)
            }
        )
    }
}
