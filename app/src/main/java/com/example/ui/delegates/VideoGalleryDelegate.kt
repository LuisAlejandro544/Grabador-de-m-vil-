package com.example.ui.delegates

import android.content.Context
import android.content.Intent
import com.example.data.RecordingsRepository
import com.example.model.RecordedVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Delegado modular para la gestión de la galería de videos grabados,
 * reproducción, edición rápida y compartición.
 */
class VideoGalleryDelegate(
    private val context: Context,
    private val repository: RecordingsRepository,
    private val scope: CoroutineScope,
    private val onMessageEmitted: (String) -> Unit
) {
    private val _videos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos = _isLoadingVideos.asStateFlow()

    private val _selectedVideoForPlay = MutableStateFlow<RecordedVideo?>(null)
    val selectedVideoForPlay = _selectedVideoForPlay.asStateFlow()

    private val _selectedVideoForEdit = MutableStateFlow<RecordedVideo?>(null)
    val selectedVideoForEdit = _selectedVideoForEdit.asStateFlow()

    fun loadVideos() {
        scope.launch {
            _isLoadingVideos.value = true
            try {
                _videos.value = repository.loadSavedRecordings()
            } catch (_: Exception) {
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    fun deleteVideo(video: RecordedVideo) {
        scope.launch {
            val success = repository.deleteRecording(video.filePath)
            if (success) {
                onMessageEmitted("Video eliminado")
                loadVideos()
            }
        }
    }

    fun renameVideo(video: RecordedVideo, newTitle: String) {
        scope.launch {
            val success = repository.renameRecording(video.filePath, newTitle)
            if (success) {
                onMessageEmitted("Video renombrado")
                loadVideos()
            } else {
                onMessageEmitted("No se pudo renombrar el archivo")
            }
        }
    }

    fun playVideo(video: RecordedVideo) {
        _selectedVideoForPlay.value = video
    }

    fun closePlayer() {
        _selectedVideoForPlay.value = null
    }

    fun openEditor(video: RecordedVideo) {
        _selectedVideoForPlay.value = null
        _selectedVideoForEdit.value = video
    }

    fun closeEditor() {
        _selectedVideoForEdit.value = null
    }

    fun onVideoEdited(file: File) {
        _selectedVideoForEdit.value = null
        onMessageEmitted("¡Clip recortado guardado: ${file.name}!")
        loadVideos()
    }

    fun onThumbnailExtracted(file: File) {
        onMessageEmitted("¡Miniatura HD guardada en Pictures: ${file.name}!")
    }

    fun shareVideo(context: Context, video: RecordedVideo) {
        val shareIntent = repository.createShareIntent(video.filePath)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir grabación"))
        } else {
            onMessageEmitted("No se pudo preparar el video para compartir")
        }
    }
}
