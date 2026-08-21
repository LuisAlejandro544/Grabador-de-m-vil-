package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Barra inferior modular de navegación de pestañas (Grabar, Videos, Juegos, Ajustes).
 */
@Composable
fun RecordBottomBar(
    activeTab: Int,
    videoCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    if (activeTab == 0) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                    contentDescription = "Grabar"
                )
            },
            label = { Text("Grabar") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_record")
        )
        NavigationBarItem(
            selected = activeTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    if (activeTab == 1) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                    contentDescription = "Videos"
                )
            },
            label = {
                Text(if (videoCount > 0) "Videos ($videoCount)" else "Videos")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_videos")
        )
        NavigationBarItem(
            selected = activeTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    if (activeTab == 2) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
                    contentDescription = "Juegos"
                )
            },
            label = { Text("Juegos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_games")
        )
        NavigationBarItem(
            selected = activeTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    if (activeTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Ajustes"
                )
            },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}
