package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.model.AppUpdateInfo
import com.example.model.ReleaseChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repositorio de Comprobación y Descarga de Actualizaciones desde GitHub Releases.
 *
 * Consulta periódicamente https://github.com/LuisAlejandro544/Vortex/releases
 * filtrando estrictamente según el canal activo de la aplicación (ej: Beta solo busca Betas).
 */
class UpdateCheckerRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdates(
        currentChannel: ReleaseChannel = ReleaseChannel.getCurrentChannel(),
        forceCheck: Boolean = false
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        // Si no es forzado y se revisó hace menos de 4 horas, devolver estado previo o sin cambio
        if (!forceCheck && (currentTime - lastCheckTime < MIN_CHECK_INTERVAL_MS)) {
            val cachedTag = prefs.getString(KEY_CACHED_TAG, null)
            val cachedHasUpdate = prefs.getBoolean(KEY_CACHED_HAS_UPDATE, false)
            if (cachedTag != null && cachedHasUpdate) {
                return@withContext AppUpdateInfo(
                    tagName = cachedTag,
                    releaseName = prefs.getString(KEY_CACHED_NAME, "") ?: "",
                    releaseNotes = prefs.getString(KEY_CACHED_BODY, "") ?: "",
                    apkDownloadUrl = prefs.getString(KEY_CACHED_APK_URL, null),
                    releaseHtmlUrl = prefs.getString(KEY_CACHED_HTML_URL, AppUpdateInfo.GITHUB_RELEASES_URL) ?: AppUpdateInfo.GITHUB_RELEASES_URL,
                    channel = currentChannel,
                    isUpdateAvailable = true
                )
            }
        }

        try {
            val url = URL(AppUpdateInfo.GITHUB_API_RELEASES_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "VortexStudio-Android/${currentChannel.getFullVersionName()}")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val releasesJson = JSONArray(responseStr)
                val targetRelease = findLatestReleaseForChannel(releasesJson, currentChannel)

                // Guardar timestamp de comprobación exitosa
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, currentTime).apply()

                if (targetRelease != null) {
                    val tagName = targetRelease.optString("tag_name", "")
                    val releaseName = targetRelease.optString("name", tagName)
                    val body = targetRelease.optString("body", "")
                    val htmlUrl = targetRelease.optString("html_url", AppUpdateInfo.GITHUB_RELEASES_URL)
                    val publishedAt = targetRelease.optString("published_at", "")

                    // Buscar asset .apk descargable
                    val assets = targetRelease.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url", null)
                                break
                            }
                        }
                    }

                    val currentInstalledTag = "v${currentChannel.getFullVersionName()}"
                    val isNewer = isVersionNewer(
                        remoteTag = tagName,
                        currentTag = currentInstalledTag,
                        currentVersionCode = currentChannel.getVersionCode()
                    )

                    // Cachear resultado
                    prefs.edit()
                        .putString(KEY_CACHED_TAG, tagName)
                        .putString(KEY_CACHED_NAME, releaseName)
                        .putString(KEY_CACHED_BODY, body)
                        .putString(KEY_CACHED_APK_URL, apkUrl)
                        .putString(KEY_CACHED_HTML_URL, htmlUrl)
                        .putBoolean(KEY_CACHED_HAS_UPDATE, isNewer)
                        .apply()

                    return@withContext AppUpdateInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        releaseNotes = body,
                        apkDownloadUrl = apkUrl,
                        releaseHtmlUrl = htmlUrl,
                        channel = currentChannel,
                        isUpdateAvailable = isNewer,
                        publishedAt = publishedAt
                    )
                } else {
                    return@withContext AppUpdateInfo(
                        channel = currentChannel,
                        isUpdateAvailable = false
                    )
                }
            } else {
                Log.w(TAG, "GitHub API HTTP Response: $responseCode")
                return@withContext AppUpdateInfo(
                    channel = currentChannel,
                    isUpdateAvailable = false,
                    errorMessage = "Código de respuesta del servidor: $responseCode"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comprobando actualizaciones desde GitHub", e)
            return@withContext AppUpdateInfo(
                channel = currentChannel,
                isUpdateAvailable = false,
                errorMessage = e.localizedMessage ?: "No se pudo conectar a GitHub"
            )
        }
    }

    /**
     * Filtra la lista de releases de GitHub buscando la más reciente que coincida
     * estrictamente con el canal activo (Beta -> solo beta, Canary -> solo canary, etc.).
     */
    private fun findLatestReleaseForChannel(
        releases: JSONArray,
        channel: ReleaseChannel
    ): JSONObject? {
        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val tagName = release.optString("tag_name", "").lowercase()
            val releaseName = release.optString("name", "").lowercase()
            val isPrerelease = release.optBoolean("prerelease", false)
            val isDraft = release.optBoolean("draft", false)

            if (isDraft) continue

            if (channel.matchesRelease(tagName, releaseName, isPrerelease)) {
                return release
            }
        }
        return null
    }

    /**
     * Compara versiones semánticas (ej: v0.1.0-beta.2 vs v0.1.0-beta.1).
     */
    private fun isVersionNewer(
        remoteTag: String,
        currentTag: String,
        currentVersionCode: Int
    ): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentTag.removePrefix("v").trim()

        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) {
            return false
        }

        // Extraer números de versión principal y subversiones de canal
        try {
            val remoteParts = cleanRemote.split("-", ".").filter { it.isNotBlank() }
            val currentParts = cleanCurrent.split("-", ".").filter { it.isNotBlank() }

            // Comparar componentes numéricos principales
            val minSize = minOf(remoteParts.size, currentParts.size)
            for (i in 0 until minSize) {
                val rNum = remoteParts[i].filter { it.isDigit() }.toIntOrNull()
                val cNum = currentParts[i].filter { it.isDigit() }.toIntOrNull()

                if (rNum != null && cNum != null) {
                    if (rNum > cNum) return true
                    if (rNum < cNum) return false
                }
            }

            // Si tienen los mismos números base pero el remoto tiene número de build más alto (ej. beta.2 > beta.1)
            val remoteDigits = cleanRemote.filter { it.isDigit() }
            val currentDigits = cleanCurrent.filter { it.isDigit() }
            if (remoteDigits.isNotEmpty() && currentDigits.isNotEmpty()) {
                val rVal = remoteDigits.toLongOrNull() ?: 0L
                val cVal = currentDigits.toLongOrNull() ?: 0L
                return rVal > cVal
            }
        } catch (_: Exception) {
        }

        return cleanRemote != cleanCurrent
    }

    /**
     * Inicia la descarga del APK directamente mediante el DownloadManager del sistema
     * o abre el enlace en el navegador si no hay gestor de descargas disponible.
     */
    fun startApkDownload(apkUrl: String, fileName: String = "Vortex-Beta-Update.apk") {
        try {
            val uri = Uri.parse(apkUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Descargando actualización de Vortex Studio")
                setDescription(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            manager?.enqueue(request)
        } catch (_: Exception) {
            // Fallback: Abrir en navegador
            openReleasesInBrowser(apkUrl)
        }
    }

    /**
     * Abre la página de GitHub Releases oficial en el navegador del teléfono.
     */
    fun openReleasesInBrowser(url: String = AppUpdateInfo.GITHUB_RELEASES_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "UpdateCheckerRepository"
        private const val PREFS_NAME = "vortex_update_checker_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_CACHED_TAG = "cached_tag"
        private const val KEY_CACHED_NAME = "cached_name"
        private const val KEY_CACHED_BODY = "cached_body"
        private const val KEY_CACHED_APK_URL = "cached_apk_url"
        private const val KEY_CACHED_HTML_URL = "cached_html_url"
        private const val KEY_CACHED_HAS_UPDATE = "cached_has_update"

        // Intervalo de comprobación automática periódica: 4 horas
        private const val MIN_CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000L
    }
}
