package com.example.service.facecam

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.model.FacecamShape
import com.example.model.FacecamSize

/**
 * Host modular encargado de la gestión de la ventana flotante de Facecam en WindowManager.
 * Maneja permisos de superposición, creación de LayoutParams, agregado, actualización y remoción de vistas.
 */
class FacecamWindowHost(
    private val context: Context
) {
    companion object {
        private const val TAG = "FacecamWindowHost"
    }

    val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    var windowParams: WindowManager.LayoutParams? = null
        private set

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    fun createLayoutParams(shape: FacecamShape, size: FacecamSize): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (widthPx, heightPx) = FacecamShapeHelper.calculateDimensions(context, shape, size)
        val paddingGlow = FacecamShapeHelper.dpToPx(context, 12f).toInt()
        val totalW = widthPx + (paddingGlow * 2)
        val totalH = heightPx + (paddingGlow * 2)

        val params = WindowManager.LayoutParams(
            totalW,
            totalH,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = FacecamShapeHelper.dpToPx(context, 16f).toInt()
            y = FacecamShapeHelper.dpToPx(context, 180f).toInt()
        }
        this.windowParams = params
        return params
    }

    fun addView(view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            windowManager?.addView(view, params)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir vista Facecam a WindowManager: ${e.message}", e)
            false
        }
    }

    fun updateDimensions(rootView: View, shape: FacecamShape, size: FacecamSize) {
        val params = windowParams ?: return
        val (widthPx, heightPx) = FacecamShapeHelper.calculateDimensions(context, shape, size)
        val paddingGlow = FacecamShapeHelper.dpToPx(context, 12f).toInt()
        val totalW = widthPx + (paddingGlow * 2)
        val totalH = heightPx + (paddingGlow * 2)

        params.width = totalW
        params.height = totalH

        try {
            windowManager?.updateViewLayout(rootView, params)
        } catch (e: Exception) {
            Log.w(TAG, "Error al actualizar dimensiones en WindowManager: ${e.message}")
        }
    }

    fun removeView(view: View?) {
        if (view == null) return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Error al remover vista Facecam de WindowManager: ${e.message}")
        } finally {
            windowParams = null
        }
    }
}
