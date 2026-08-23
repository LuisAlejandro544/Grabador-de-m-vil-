package com.example.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Receptor de eventos de emergencia del sistema (Batería Crítica y Almacenamiento Lleno).
 * Permite ejecutar la salvaguarda Graceful Finalize de la grabación activa.
 */
class ServiceEmergencyReceiver(
    private val onEmergencyBatteryLow: () -> Unit,
    private val onEmergencyStorageLow: () -> Unit
) {
    companion object {
        private const val TAG = "ServiceEmergencyReceiver"
    }

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_LOW -> {
                    Log.w(TAG, "Alerta del sistema: Batería baja. Ejecutando Graceful Finalize...")
                    onEmergencyBatteryLow()
                }
                Intent.ACTION_DEVICE_STORAGE_LOW -> {
                    Log.w(TAG, "Alerta del sistema: Almacenamiento bajo. Ejecutando Graceful Finalize...")
                    onEmergencyStorageLow()
                }
            }
        }
    }

    fun register(context: Context) {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_DEVICE_STORAGE_LOW)
            }
            try {
                context.registerReceiver(receiver, filter)
                isRegistered = true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar ServiceEmergencyReceiver: ${e.message}")
            }
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }
}
