package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InstalledAppItem
import com.example.ui.components.gamelauncher.GameLauncherEmptyState
import com.example.ui.components.gamelauncher.GameLauncherFilterBar
import com.example.ui.components.gamelauncher.GameLauncherHeaderBanner
import com.example.ui.components.gamelauncher.GameLauncherItemCard

/**
 * Pantalla orquestadora del Lanzador de Juegos de Vortex Studio.
 * Desacoplada modularmente:
 * - [GameLauncherHeaderBanner]: Banner informativo y botón de refresco.
 * - [GameLauncherFilterBar]: Filtros rápidos y barra de búsqueda en tiempo real.
 * - [GameLauncherItemCard]: Renderizado individual optimizado de apps/juegos.
 * - [GameLauncherEmptyState]: Indicador contextual de búsquedas sin resultados.
 */
@Composable
fun GameLauncherScreen(
    games: List<InstalledAppItem>,
    isLoading: Boolean,
    isRecording: Boolean,
    onStartRecordingWithGame: (String) -> Unit,
    onLaunchGameDirectly: (String) -> Unit,
    onRefreshGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    // 0: Solo Juegos, 1: Todas las Apps
    var selectedFilterIndex by remember { mutableStateOf(0) }

    val actualGamesCount = remember(games) { games.count { it.isGame } }

    val currentList = remember(games, selectedFilterIndex) {
        if (selectedFilterIndex == 0) {
            games.filter { it.isGame }
        } else {
            games
        }
    }

    val filteredGames = remember(currentList, searchQuery) {
        if (searchQuery.isBlank()) {
            currentList
        } else {
            currentList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GameLauncherHeaderBanner(onRefreshGames = onRefreshGames)

        Spacer(modifier = Modifier.height(10.dp))

        GameLauncherFilterBar(
            selectedFilterIndex = selectedFilterIndex,
            onFilterChange = { selectedFilterIndex = it },
            actualGamesCount = actualGamesCount,
            totalAppsCount = games.size,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("loading_games_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Escaneando juegos instalados...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Buscando en hilo secundario para no ralentizar la pantalla",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredGames.isEmpty()) {
            GameLauncherEmptyState(
                selectedFilterIndex = selectedFilterIndex,
                searchQuery = searchQuery,
                onSwitchToAllApps = { selectedFilterIndex = 1 },
                onRefreshGames = onRefreshGames,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredGames, key = { it.packageName }) { appItem ->
                    GameLauncherItemCard(
                        appItem = appItem,
                        isRecording = isRecording,
                        onRecordAndPlay = { onStartRecordingWithGame(appItem.packageName) },
                        onPlayOnly = { onLaunchGameDirectly(appItem.packageName) }
                    )
                }
            }
        }
    }
}
