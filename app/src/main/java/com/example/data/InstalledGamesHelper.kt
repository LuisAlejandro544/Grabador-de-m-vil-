package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isGame: Boolean
)

/**
 * Asistente para escanear y detectar de forma precisa y asíncrona (en hilo secundario IO)
 * los juegos y aplicaciones instaladas en el dispositivo.
 */
class InstalledGamesHelper(private val context: Context) {

    companion object {
        private const val TAG = "InstalledGamesHelper"

        // Lista de paquetes o prefijos conocidos de editoriales de juegos y motores
        private val KNOWN_GAME_PACKAGES_PREFIXES = listOf(
            "com.garena", "com.dts.freefire", "com.tencent", "com.activision", "com.epicgames",
            "com.mojang", "com.supercell", "com.ea.gp", "com.gameloft", "com.roblox",
            "com.riotgames", "com.mihoyo", "com.hoyoverse", "com.krafton", "com.innersloth",
            "com.kiloo", "com.king.", "com.rovio", "com.miniclip", "com.zynga",
            "com.ubisoft", "com.square_enix", "com.bandainamco", "com.sega", "com.konami",
            "com.nintendo", "com.netmarble", "com.ketchapp", "com.voodoo", "com.habby",
            "com.playrix", "com.outfit7", "com.gametion", "com.chillingo", "com.nekki",
            "com.kabam", "com.plarium", "com.scopely", "com.igg", "com.lilithgames",
            "com.firsttouchgames", "com.nordcurrent", "com.fingersoft", "com.madfingergames",
            "com.halfbrick", "com.miniclip", "com.gameloft", "com.vector", "com.carx",
            "com.axlebolt.standoff2", "com.levelinfinite", "com.netease", "com.blizzard"
        )

        // Palabras clave en el nombre del paquete o etiqueta que identifican juegos de terceros / APKs
        private val GAME_KEYWORDS = listOf(
            "game", "games", "racing", "simulator", "shooter", "battle", "clash", "runner",
            "arcade", "puzzle", "tycoon", "zombie", "warrior", "sniper", "soccer", "football",
            "cricket", "brawl", "drift", "dungeon", "quest", "rpg", "fps", "arena", "legends",
            "league", "craft", "minecraft", "roblox", "stumble", "subway", "asphalt", "freefire",
            "pubg", "codm", "genshin", "fortnite", "fifa", "pes", "efootball", "pokemon",
            "shadowfight", "amongus", "angrybirds", "candycrush", "templerun", "hillclimb",
            "shadow gun", "dead trigger", "fruit ninja", "plant vs zombie", "mortal kombat"
        )

        // Prefijos de apps del sistema y utilidades que NUNCA deben clasificarse como juegos
        private val EXCLUDED_SYSTEM_PREFIXES = listOf(
            "com.android.settings", "com.android.chrome", "com.android.camera", "com.android.deskclock",
            "com.android.calculator", "com.android.contacts", "com.android.dialer", "com.android.documentsui",
            "com.google.android.apps", "com.google.android.gm", "com.google.android.youtube",
            "com.google.android.googlequicksearchbox", "com.google.android.contacts",
            "com.google.android.dialer", "com.google.android.deskclock", "com.google.android.calculator2",
            "com.google.android.keep", "com.google.android.calendar", "com.google.android.videos",
            "com.google.android.music", "com.google.android.play.games", "com.whatsapp", "com.facebook",
            "com.instagram", "com.twitter", "org.telegram", "com.spotify", "com.netflix", "tv.twitch",
            "com.discord", "com.microsoft", "com.amazon", "com.adobe"
        )
    }

    /**
     * Escanea las aplicaciones instaladas en un hilo secundario (Dispatchers.IO)
     * clasificando de forma precisa si son juegos o aplicaciones generales.
     */
    suspend fun getInstalledGamesAndApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val currentPkg = context.packageName

            // Procesamiento en paralelo de resolución de metadatos e iconos para máxima fluidez
            val appList = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == currentPkg) return@mapNotNull null

                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    val isGame = isAppClassifiedAsGame(appInfo, pkg, label)

                    InstalledAppItem(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isGame = isGame
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Ordenar: primero los juegos detectados, luego orden alfabético
            appList.sortedWith(compareByDescending<InstalledAppItem> { it.isGame }.thenBy { it.appName.lowercase() })
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando juegos instalados: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Algoritmo de clasificación de juegos multi-criterio.
     */
    private fun isAppClassifiedAsGame(appInfo: ApplicationInfo, packageName: String, appLabel: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = appLabel.lowercase()

        // 1. Descartar si coincide con utilidades del sistema conocidas
        if (EXCLUDED_SYSTEM_PREFIXES.any { lowerPkg.startsWith(it) }) {
            return false
        }

        // 2. Comprobar Categoría oficial de Android (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true
            }
            // Si tiene asignada otra categoría explícita no-juego, descartar
            if (appInfo.category in listOf(
                    ApplicationInfo.CATEGORY_AUDIO,
                    ApplicationInfo.CATEGORY_IMAGE,
                    ApplicationInfo.CATEGORY_VIDEO,
                    ApplicationInfo.CATEGORY_NEWS,
                    ApplicationInfo.CATEGORY_SOCIAL,
                    ApplicationInfo.CATEGORY_MAPS,
                    ApplicationInfo.CATEGORY_PRODUCTIVITY,
                    ApplicationInfo.CATEGORY_ACCESSIBILITY
                )
            ) {
                return false
            }
        }

        // 3. Comprobar flag legacy de juego en ApplicationInfo
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true
        }

        // 4. Comprobar prefijos de paquetes de juegos populares y editoriales
        if (KNOWN_GAME_PACKAGES_PREFIXES.any { lowerPkg.startsWith(it) || lowerPkg.contains(it) }) {
            return true
        }

        // 5. Comprobar palabras clave de juegos en el nombre de paquete o título
        val hasGameKeyword = GAME_KEYWORDS.any { keyword ->
            lowerPkg.contains(keyword) || lowerLabel.contains(keyword)
        }

        // Si contiene palabras clave de juego y no es una app del sistema básica
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (hasGameKeyword && !isSystemApp) {
            return true
        }

        return false
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error lanzando aplicación $packageName: ${e.message}")
            false
        }
    }
}
