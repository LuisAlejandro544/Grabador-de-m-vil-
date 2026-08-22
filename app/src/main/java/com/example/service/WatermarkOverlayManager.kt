package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.RecordingConfig
import com.example.model.TouchColorOption
import com.example.model.WatermarkSize
import com.example.model.WatermarkType
import com.example.service.watermark.WatermarkTouchHelper

/**
 * Gestor modular de la Marca de Agua / Logo Personalizado Superpuesto.
 * Permite superponer un texto con estilo gamer o un logo/imagen PNG personalizado,
 * con opacidad configurable y movimiento libre (drag & drop) por toda la pantalla.
 */
class WatermarkOverlayManager(
    private val context: Context,
    private var config: RecordingConfig,
    private val onCloseClicked: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "WatermarkOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootContainer: FrameLayout? = null
    private var contentContainer: LinearLayout? = null
    private var textView: TextView? = null
    private var imageView: ImageView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isShowingInternal = false
    val isShowing: Boolean get() = isShowingInternal

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowingInternal || !isOverlayPermissionGranted() || windowManager == null) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(20)
                y = dpToPx(60)
            }
            this.windowParams = params

            val root = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            }
            this.rootContainer = root

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            this.contentContainer = container

            // Construir vistas internas según el tipo de marca de agua
            buildWatermarkContent(container)

            root.addView(container)

            WatermarkTouchHelper.attach(
                targetView = root,
                params = params,
                windowManager = windowManager,
                isShowingProvider = { isShowingInternal },
                onSingleTap = {
                    // Feedback sutil
                }
            )

            windowManager.addView(root, params)
            isShowingInternal = true
            applyOpacity()

            Log.i(TAG, "Marca de agua mostrada con éxito (${config.watermarkType.name})")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando marca de agua flotante: ${e.message}", e)
            isShowingInternal = false
        }
    }

    private fun buildWatermarkContent(container: LinearLayout) {
        container.removeAllViews()

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(8).toFloat()
            setColor(0x77111827)
            setStroke(dpToPx(1), (config.watermarkColor.primaryArgb and 0x66FFFFFF).toInt())
        }
        container.background = bgDrawable
        container.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

        if (config.watermarkType == WatermarkType.IMAGE) {
            val img = ImageView(context).apply {
                val iconSize = dpToPx(config.watermarkSize.iconSizeDp)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            this.imageView = img
            loadImageIntoView(img)
            container.addView(img)
        } else {
            val tv = TextView(context).apply {
                text = config.watermarkText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, config.watermarkSize.textSizeSp)
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(config.watermarkColor.colorInt)
                setShadowLayer(6f, 0f, 2f, Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            this.textView = tv
            container.addView(tv)
        }
    }

    private fun loadImageIntoView(img: ImageView) {
        val uriStr = config.watermarkCustomImageUri
        if (!uriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriStr)
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    img.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cargar la imagen de marca de agua desde URI: ${e.message}")
            }
        }
        // Fallback a ícono gamer por defecto
        img.setImageResource(android.R.drawable.ic_menu_compass)
        img.setColorFilter(config.watermarkColor.colorInt)
    }

    private fun applyOpacity() {
        val root = rootContainer ?: return
        root.alpha = config.watermarkOpacity.coerceIn(0.15f, 1.0f)
    }

    fun updateConfig(newConfig: RecordingConfig) {
        this.config = newConfig
        val container = contentContainer ?: return
        mainHandler.post {
            try {
                buildWatermarkContent(container)
                applyOpacity()
                rootContainer?.let { root ->
                    windowParams?.let { p ->
                        windowManager?.updateViewLayout(root, p)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando configuración de marca de agua: ${e.message}")
            }
        }
    }

    fun dismiss() {
        if (!isShowingInternal) return
        try {
            if (rootContainer != null) {
                windowManager?.removeView(rootContainer)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removiendo vista de marca de agua: ${e.message}")
        } finally {
            rootContainer = null
            contentContainer = null
            textView = null
            imageView = null
            windowParams = null
            isShowingInternal = false
            Log.d(TAG, "Marca de agua flotante cerrada")
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
