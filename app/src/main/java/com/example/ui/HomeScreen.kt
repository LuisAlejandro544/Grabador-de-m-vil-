package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RecordingStatus
import com.example.ui.components.GameLauncherScreen
import com.example.ui.components.RecordControlCard
import com.example.ui.components.SettingsView
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.RecordActiveAmber
import com.example.ui.theme.RecordActiveRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RecordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = UiState())

    var pendingGameLaunchPackage by remember { mutableStateOf<String?>(null) }

    // Media Projection Permission Launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingFlow(
                resultCode = result.resultCode,
                resultData = result.data!!,
                targetGamePackage = pendingGameLaunchPackage
            )
            pendingGameLaunchPackage = null
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de captura de pantalla cancelado")
            }
            pendingGameLaunchPackage = null
        }
    }

    // Audio and Notification Permissions Launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        } else true

        // Launch screen capture prompt
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }

    fun requestStartRecording(gamePackage: String? = null) {
        pendingGameLaunchPackage = gamePackage
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Grabador de Pantalla",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val statusText = when (uiState.status) {
                                RecordingStatus.RECORDING -> "Grabando (${formatSeconds(uiState.elapsedSeconds)})"
                                RecordingStatus.PAUSED -> "En pausa"
                                RecordingStatus.COUNTDOWN -> "Iniciando..."
                                RecordingStatus.SAVING -> "Guardando video..."
                                else -> "Listo para capturar"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.status == RecordingStatus.RECORDING) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(RecordActiveRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    color = if (uiState.status == RecordingStatus.RECORDING) RecordActiveRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadVideos() },
                        modifier = Modifier.testTag("refresh_videos_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = {
                        Icon(
                            if (uiState.activeTab == 0) Icons.Filled.Videocam else Icons.Outlined.Videocam,
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
                    selected = uiState.activeTab == 1,
                    onClick = {
                        viewModel.setActiveTab(1)
                        viewModel.loadVideos()
                    },
                    icon = {
                        Icon(
                            if (uiState.activeTab == 1) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Videos"
                        )
                    },
                    label = {
                        Text(if (uiState.videos.isNotEmpty()) "Videos (${uiState.videos.size})" else "Videos")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_videos")
                )
                NavigationBarItem(
                    selected = uiState.activeTab == 2,
                    onClick = {
                        viewModel.setActiveTab(2)
                        viewModel.loadInstalledGames()
                    },
                    icon = {
                        Icon(
                            if (uiState.activeTab == 2) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
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
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.setActiveTab(3) },
                    icon = {
                        Icon(
                            if (uiState.activeTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.activeTab) {
                0 -> {
                    // Main Recording Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            RecordControlCard(
                                config = uiState.config,
                                status = uiState.status,
                                elapsedSeconds = uiState.elapsedSeconds,
                                countdownNumber = uiState.countdownNumber,
                                onStartClick = { requestStartRecording() },
                                onStopClick = { viewModel.stopRecording() },
                                onPauseClick = { viewModel.pauseRecording() },
                                onResumeClick = { viewModel.resumeRecording() },
                                onCancelCountdown = { viewModel.cancelCountdown() },
                                onToggleGameMode = { viewModel.toggleGameMode(it) }
                            )
                        }

                        // Quick Guide Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Consejos para grabar",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Puedes pausar y detener la grabación directamente desde la barra de notificaciones mientras juegas.\n• En la pestaña \"Juegos\" puedes iniciar la grabación y abrir tu juego con un solo toque.\n• Las grabaciones se guardan en alta calidad MP4 compatibles con cualquier reproductor.",
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Recent Recordings Header & Short List
                        if (uiState.videos.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Grabaciones Recientes",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(onClick = { viewModel.setActiveTab(1) }) {
                                        Text("Ver todas (${uiState.videos.size})", fontSize = 13.sp)
                                    }
                                }
                            }

                            items(uiState.videos.take(3), key = { it.id }) { video ->
                                VideoItemCard(
                                    video = video,
                                    onPlay = { viewModel.playVideo(it) },
                                    onShare = { viewModel.shareVideo(context, it) },
                                    onDelete = { viewModel.deleteVideo(it) },
                                    onRename = { v, name -> viewModel.renameVideo(v, name) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Videos Gallery Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Galería de Grabaciones",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.videos.size} videos guardados",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isLoadingVideos) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.videos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Aún no tienes grabaciones",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ve a la pestaña \"Grabar\" para realizar tu primera captura de pantalla o partida de juego.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.videos, key = { it.id }) { video ->
                                    VideoItemCard(
                                        video = video,
                                        onPlay = { viewModel.playVideo(it) },
                                        onShare = { viewModel.shareVideo(context, it) },
                                        onDelete = { viewModel.deleteVideo(it) },
                                        onRename = { v, name -> viewModel.renameVideo(v, name) }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Games Launcher Tab
                    GameLauncherScreen(
                        games = uiState.installedGames,
                        isRecording = uiState.status == RecordingStatus.RECORDING,
                        onStartRecordingWithGame = { pkg ->
                            requestStartRecording(pkg)
                        },
                        onLaunchGameDirectly = { pkg ->
                            viewModel.launchGame(pkg)
                        }
                    )
                }

                3 -> {
                    // Settings Tab
                    SettingsView(
                        config = uiState.config,
                        onUpdateResolution = { viewModel.updateResolution(it) },
                        onUpdateFps = { viewModel.updateFps(it) },
                        onUpdateBitrate = { viewModel.updateBitrate(it) },
                        onUpdateAudioSource = { viewModel.updateAudioSource(it) },
                        onUpdateCountdown = { viewModel.updateCountdown(it) },
                        onToggleGameMode = { viewModel.toggleGameMode(it) },
                        onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) }
                    )
                }
            }
        }
    }

    // Video Player Dialog
    uiState.selectedVideoForPlay?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.closePlayer() },
            onShare = { viewModel.shareVideo(context, it) }
        )
    }
}

private fun formatSeconds(totalSec: Int): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", min, sec)
}
