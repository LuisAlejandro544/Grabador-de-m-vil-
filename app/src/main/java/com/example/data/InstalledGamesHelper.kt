package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isGame: Boolean
)

class InstalledGamesHelper(private val context: Context) {

    suspend fun getInstalledGamesAndApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<InstalledAppItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)

                appList.add(
                    InstalledAppItem(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isGame = isGame
                    )
                )
            } catch (e: Exception) {
                // Ignore inaccessible packages
            }
        }

        // Sort games first, then alphabetically
        appList.sortedWith(compareByDescending<InstalledAppItem> { it.isGame }.thenBy { it.appName.lowercase() })
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
            false
        }
    }
}
