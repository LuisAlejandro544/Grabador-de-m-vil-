package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordedVideo(
    val id: String,
    val filePath: String,
    val title: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val dateModified: Long,
    val width: Int = 1080,
    val height: Int = 1920
) {
    fun formattedDuration(): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    fun formattedSize(): String {
        val mb = fileSizeBytes.toDouble() / (1024 * 1024)
        return if (mb >= 1000) {
            String.format(Locale.getDefault(), "%.2f GB", mb / 1024)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(dateModified))
    }
}
