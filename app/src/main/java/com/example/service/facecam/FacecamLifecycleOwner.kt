package com.example.service.facecam

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Ciclo de vida desacoplado para vincular CameraX de forma segura en un Service.
 */
class FacecamLifecycleOwner : LifecycleOwner {

    companion object {
        private const val TAG = "FacecamLifecycleOwner"
    }

    private var lifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    fun start() {
        try {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
                lifecycleRegistry = LifecycleRegistry(this)
                lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
            }
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo actualizar estado de ciclo de vida Facecam: ${e.message}")
        }
    }

    fun stop() {
        try {
            if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo ciclo de vida Facecam: ${e.message}")
        }
    }
}
