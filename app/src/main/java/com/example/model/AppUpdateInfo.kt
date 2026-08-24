package com.example.model

/**
 * Información sobre una actualización de la aplicación desde GitHub Releases.
 */
data class AppUpdateInfo(
    val tagName: String = "",
    val releaseName: String = "",
    val releaseNotes: String = "",
    val apkDownloadUrl: String? = null,
    val releaseHtmlUrl: String = GITHUB_RELEASES_URL,
    val channel: ReleaseChannel = ReleaseChannel.getCurrentChannel(),
    val isUpdateAvailable: Boolean = false,
    val publishedAt: String? = null,
    val isChecking: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        const val GITHUB_REPO_OWNER = "LuisAlejandro544"
        const val GITHUB_REPO_NAME = "Vortex"
        const val GITHUB_RELEASES_URL = "https://github.com/LuisAlejandro544/Vortex/releases"
        const val GITHUB_API_RELEASES_URL = "https://api.github.com/repos/LuisAlejandro544/Vortex/releases"
    }
}
