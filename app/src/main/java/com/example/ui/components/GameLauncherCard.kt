package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.InstalledAppItem

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
        // Banner card explaining Game Recorder helper
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lanzador de Juegos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inicia la grabación y abre tu juego favorito al instante",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefreshGames,
                    modifier = Modifier.testTag("refresh_games_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recargar juegos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips (Solo Juegos vs Todas las Apps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilterIndex == 0,
                onClick = { selectedFilterIndex = 0 },
                label = {
                    Text(
                        text = "🎮 Juegos (${actualGamesCount})",
                        fontSize = 13.sp,
                        fontWeight = if (selectedFilterIndex == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_games_only_chip")
            )

            FilterChip(
                selected = selectedFilterIndex == 1,
                onClick = { selectedFilterIndex = 1 },
                label = {
                    Text(
                        text = "📱 Todas las Apps (${games.size})",
                        fontSize = 13.sp,
                        fontWeight = if (selectedFilterIndex == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("filter_all_apps_chip")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (selectedFilterIndex == 0) "Buscar en tus juegos..." else "Buscar aplicación...",
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_games_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area: Loading vs Empty vs List
        if (isLoading) {
            // Pantalla de Carga
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedFilterIndex == 0) Icons.Default.SportsEsports else Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Sin resultados para \"$searchQuery\""
                            } else if (selectedFilterIndex == 0) {
                                "No se detectaron juegos instalados"
                            } else {
                                "No se encontraron aplicaciones"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedFilterIndex == 0 && searchQuery.isBlank()) {
                                "Si instalaste un juego desde un APK de terceros o tienda externa, puedes seleccionarlo en \"Todas las Apps\"."
                            } else {
                                "Verifica el texto de búsqueda o pulsa recargar."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (selectedFilterIndex == 0) {
                            Button(
                                onClick = { selectedFilterIndex = 1 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Ver todas las aplicaciones", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onRefreshGames,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Escanear de nuevo", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredGames, key = { it.packageName }) { appItem ->
                    GameListItem(
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

@Composable
private fun GameListItem(
    appItem: InstalledAppItem,
    isRecording: Boolean,
    onRecordAndPlay: () -> Unit,
    onPlayOnly: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_item_${appItem.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (appItem.icon != null) {
                val bitmap = remember(appItem.icon) {
                    try {
                        appItem.icon.toBitmap(96, 96)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = appItem.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appItem.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (appItem.isGame) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "JUEGO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = appItem.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón Lanzar directo (Jugar sin grabar si lo desea)
            IconButton(
                onClick = onPlayOnly,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("play_game_direct_btn_${appItem.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Abrir juego",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action Button: Grabar y Abrir
            Button(
                onClick = onRecordAndPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("record_game_btn_${appItem.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Grabar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
