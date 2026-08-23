package com.example.ui.launcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig
import com.example.ui.RecordViewModel

/**
 * Estado y control de lanzadores de permisos para la captura de pantalla [MediaProjection],
 * cámara (Facecam) y notificaciones en Android 13+.
 */
class ScreenRecordLaunchersState(
    val requestStartRecording: (gamePackage: String?) -> Unit,
    val requestCameraPermission: () -> Unit
)

/**
 * Composable que registra y gestiona de manera modular los Activity Result Launchers
 * necesarios para la grabación de pantalla y permisos del sistema.
 */
@Composable
fun rememberScreenRecordLaunchers(
    viewModel: RecordViewModel,
    config: RecordingConfig,
    onShowSnackbar: (String) -> Unit
): ScreenRecordLaunchersState {
    val context = LocalContext.current
    var pendingGameLaunchPackage by remember { mutableStateOf<String?>(null) }

    // Launcher dedicado para permiso de cámara (usado al activar Facecam en Ajustes)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onShowSnackbar("Permiso de cámara denegado")
        }
    }

    // Launcher de permisos de captura de pantalla MediaProjection
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
            onShowSnackbar("Permiso de captura de pantalla cancelado")
            pendingGameLaunchPackage = null
        }
    }

    // Launcher de permisos múltiples requeridos
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }

    val requestStartRecording: (String?) -> Unit = { gamePackage ->
        pendingGameLaunchPackage = gamePackage
        val missingPermissions = mutableListOf<String>()

        // 1. Permiso de micrófono: Solo si la configuración requiere capturar audio y aún no está concedido
        if (config.audioSource != AudioSourceType.NONE) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 2. Permiso de cámara: Solo si Facecam está encendido y aún no está concedido
        if (config.showFacecam) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.CAMERA)
            }
        }

        // 3. Notificaciones en Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isEmpty()) {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = projectionManager.createScreenCaptureIntent()
            mediaProjectionLauncher.launch(captureIntent)
        } else {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    val requestCameraPermission: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return remember(viewModel, config) {
        ScreenRecordLaunchersState(
            requestStartRecording = requestStartRecording,
            requestCameraPermission = requestCameraPermission
        )
    }
}
