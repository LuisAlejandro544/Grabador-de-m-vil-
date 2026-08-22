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
import com.example.model.SceneOverlayType
import com.example.service.overlay.SceneOverlayDrawables

/**
 * Gestor modular de Overlays de Escena Personalizados (Marcos PNG y Alertas Estáticas).
 * Superpone marcos gamer, banners de transmisión, badges en vivo o imágenes PNG transparentes.
 * Se ejecuta con FLAG_NOT_TOUCHABLE para garantizar cero latencia e interacción intacta en juegos.
 */
class SceneOverlayManager(
    private val context: Context,
    private var config: RecordingConfig
) {

    companion object {
        private const val TAG = "SceneOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootContainer: FrameLayout? = null
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

        if (config.sceneOverlayType == SceneOverlayType.NONE) {
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
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            this.windowParams = params

            val root = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            this.rootContainer = root

            buildSceneContent(root)

            windowManager.addView(root, params)
            isShowingInternal = true
            applyOpacity()

            Log.i(TAG, "Scene Overlay mostrado con éxito (${config.sceneOverlayType.name})")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando Scene Overlay: ${e.message}", e)
            isShowingInternal = false
        }
    }

    private fun buildSceneContent(root: FrameLayout) {
        root.removeAllViews()

        when (config.sceneOverlayType) {
            SceneOverlayType.NONE -> {
                // Vacío
            }
            SceneOverlayType.GAMER_NEON_FRAME -> {
                // Marco Gamer con biseles en bordes
                val frameView = View(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    background = SceneOverlayDrawables.GamerNeonFrameDrawable(
                        context,
                        config.touchVisualizerColor.colorInt
                    )
                }
                root.addView(frameView)
            }
            SceneOverlayType.STREAMER_BANNER -> {
                // Banner inferior con redes
                val banner = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    val h = dpToPx(38)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        h,
                        Gravity.BOTTOM
                    ).apply {
                        bottomMargin = dpToPx(8)
                        leftMargin = dpToPx(16)
                        rightMargin = dpToPx(16)
                    }
                    background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(0xEE0A0F1D.toInt(), 0xEE1E293B.toInt(), 0xEE0A0F1D.toInt())
                    ).apply {
                        cornerRadius = dpToPx(12).toFloat()
                        setStroke(dpToPx(1), config.touchVisualizerColor.colorInt)
                    }
                    setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
                }

                val bannerText = TextView(context).apply {
                    text = config.sceneOverlayText
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.DEFAULT_BOLD
                    setShadowLayer(4f, 0f, 2f, Color.BLACK)
                }
                banner.addView(bannerText)
                root.addView(banner)
            }
            SceneOverlayType.LIVE_BADGE -> {
                // Badge "🔴 EN VIVO" en esquina superior derecha
                val badge = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.END
                    ).apply {
                        topMargin = dpToPx(24)
                        rightMargin = dpToPx(16)
                    }
                    background = GradientDrawable().apply {
                        setColor(0xCCEF4444.toInt())
                        cornerRadius = dpToPx(16).toFloat()
                    }
                    setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                }

                val badgeText = TextView(context).apply {
                    text = "🔴 LIVE"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                badge.addView(badgeText)
                root.addView(badge)
            }
            SceneOverlayType.STANDBY_PAUSE -> {
                // Cartel central de Standby / Pausa
                val centerCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                    background = GradientDrawable().apply {
                        setColor(0xDD0F172A.toInt())
                        cornerRadius = dpToPx(18).toFloat()
                        setStroke(dpToPx(2), config.touchVisualizerColor.colorInt)
                    }
                    setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
                }

                val title = TextView(context).apply {
                    text = "⏸️ TRANSMISIÓN EN PAUSA"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                }
                val sub = TextView(context).apply {
                    text = config.sceneOverlayText
                    setTextColor(0xFF94A3B8.toInt())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    gravity = Gravity.CENTER
                }
                centerCard.addView(title)
                centerCard.addView(sub)
                root.addView(centerCard)
            }
            SceneOverlayType.CUSTOM_IMAGE -> {
                val customImg = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_XY
                }
                loadCustomImageInto(customImg)
                root.addView(customImg)
            }
        }
    }

    private fun loadCustomImageInto(imageView: ImageView) {
        val uriStr = config.sceneOverlayImageUri
        if (!uriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriStr)
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cargar la imagen de overlay desde URI: ${e.message}")
            }
        }
        // Fallback: marco sutil gamer
        imageView.background = SceneOverlayDrawables.GamerNeonFrameDrawable(
            context,
            config.touchVisualizerColor.colorInt
        )
    }

    private fun applyOpacity() {
        val root = rootContainer ?: return
        root.alpha = config.sceneOverlayOpacity.coerceIn(0.1f, 1.0f)
    }

    fun updateConfig(newConfig: RecordingConfig) {
        this.config = newConfig
        val root = rootContainer ?: return
        mainHandler.post {
            try {
                buildSceneContent(root)
                applyOpacity()
            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando Scene Overlay: ${e.message}")
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
            Log.w(TAG, "Error removiendo Scene Overlay de WindowManager: ${e.message}")
        } finally {
            rootContainer = null
            windowParams = null
            isShowingInternal = false
            Log.d(TAG, "Scene Overlay cerrado")
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
