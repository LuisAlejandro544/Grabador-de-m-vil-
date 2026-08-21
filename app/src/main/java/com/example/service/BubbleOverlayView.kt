package com.example.service

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/**
 * Componente visual modular del widget flotante de grabación.
 * Incluye controles de grabación, cronómetro monospace, pulso en vivo
 * y botón de "Herramientas" (Captura de pantalla rápida y Pincel de dibujo sobre la pantalla).
 */
class BubbleOverlayView(
    private val context: Context,
    private val onPauseClicked: () -> Unit,
    private val onResumeClicked: () -> Unit,
    private val onStopClicked: () -> Unit,
    private val onMicToggleClicked: () -> Unit,
    private val onScreenshotClicked: () -> Unit,
    private val onDrawToolClicked: () -> Unit
) {

    companion object {
        private const val COLOR_BG_DARK = 0xEE181E29.toInt()
        private const val COLOR_RECORDING_RED = 0xFFEF4444.toInt()
        private const val COLOR_PAUSED_AMBER = 0xFFF59E0B.toInt()
        private const val COLOR_TEXT_WHITE = 0xFFF8FAFC.toInt()
        private const val COLOR_BTN_BG = 0x33FFFFFF.toInt()
        private const val COLOR_STOP_BG = 0x55EF4444.toInt()
        private const val COLOR_TOOLS_BG = 0x336366F1.toInt()
        private const val COLOR_MIC_ON_BG = 0x4410B981.toInt()
        private const val COLOR_MIC_ON_TEXT = 0xFF34D399.toInt()
        private const val COLOR_MIC_OFF_BG = 0x3364748B.toInt()
        private const val COLOR_MIC_OFF_TEXT = 0xFF94A3B8.toInt()
    }

    val rootView: LinearLayout
    private val dotIndicator: View
    private val tvTimer: TextView
    private val actionControlsLayout: LinearLayout
    private val toolsSubmenuLayout: LinearLayout
    private val iconPauseResume: ImageView
    private val tvPauseResumeLabel: TextView
    private val btnMicToggle: LinearLayout
    private val iconMicToggle: ImageView
    private val tvMicToggleLabel: TextView
    private val btnToggleExpand: ImageView
    private var dotPulseAnimator: ObjectAnimator? = null

    var isExpanded: Boolean = true
        private set
    var isPaused: Boolean = false
        private set
    var isMicMuted: Boolean = false
        private set
    var isToolsMenuOpen: Boolean = false
        private set

    init {
        rootView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            elevation = dpToPx(8).toFloat()
        }

        // Barra principal horizontal
        val mainBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = createCardDrawable(COLOR_BG_DARK, dpToPx(24))
        }

        // 1. Indicador en vivo (Punto pulsante)
        dotIndicator = View(context).apply {
            val dotSize = dpToPx(10)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dpToPx(8)
            }
            background = createCircleDrawable(COLOR_RECORDING_RED)
        }
        mainBar.addView(dotIndicator)

        // 2. Etiqueta de cronómetro (Monospace)
        tvTimer = TextView(context).apply {
            text = "00:00"
            setTextColor(COLOR_TEXT_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
        }
        mainBar.addView(tvTimer)

        // 3. Contenedor de acciones rápidas
        actionControlsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Botón Pausa / Reanudar
        val btnPauseResume = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_BTN_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
        }

        iconPauseResume = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            layoutParams = LinearLayout.LayoutParams(dpToPx(15), dpToPx(15)).apply {
                marginEnd = dpToPx(4)
            }
            setColorFilter(Color.WHITE)
        }
        btnPauseResume.addView(iconPauseResume)

        tvPauseResumeLabel = TextView(context).apply {
            text = "Pausar"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        btnPauseResume.addView(tvPauseResumeLabel)

        btnPauseResume.setOnClickListener {
            vibrateQuick()
            if (isPaused) onResumeClicked() else onPauseClicked()
        }
        actionControlsLayout.addView(btnPauseResume)

        // Botón Selector Dinámico de Voz / Audio del Juego (Micrófono en vivo)
        btnMicToggle = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_MIC_ON_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
        }

        iconMicToggle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            layoutParams = LinearLayout.LayoutParams(dpToPx(15), dpToPx(15)).apply {
                marginEnd = dpToPx(4)
            }
            setColorFilter(COLOR_MIC_ON_TEXT)
        }
        btnMicToggle.addView(iconMicToggle)

        tvMicToggleLabel = TextView(context).apply {
            text = "Voz ON"
            setTextColor(COLOR_MIC_ON_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        btnMicToggle.addView(tvMicToggleLabel)

        btnMicToggle.setOnClickListener {
            vibrateQuick()
            onMicToggleClicked()
        }
        actionControlsLayout.addView(btnMicToggle)

        // Botón Herramientas (Captura & Lapicero)
        val btnTools = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_TOOLS_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }

            val iconTools = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_manage)
                layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    marginEnd = dpToPx(4)
                }
                setColorFilter(0xFF818CF8.toInt())
            }
            addView(iconTools)

            val tvToolsLabel = TextView(context).apply {
                text = "Herramientas"
                setTextColor(0xFF818CF8.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvToolsLabel)

            setOnClickListener {
                vibrateQuick()
                toggleToolsMenu()
            }
        }
        actionControlsLayout.addView(btnTools)

        // Botón Parar
        val btnStop = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_STOP_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }

            val iconStop = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    marginEnd = dpToPx(4)
                }
                setColorFilter(COLOR_RECORDING_RED)
            }
            addView(iconStop)

            val tvStopLabel = TextView(context).apply {
                text = "Parar"
                setTextColor(COLOR_RECORDING_RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvStopLabel)

            setOnClickListener {
                vibrateQuick()
                onStopClicked()
            }
        }
        actionControlsLayout.addView(btnStop)

        mainBar.addView(actionControlsLayout)

        // 4. Botón colapsar / expandir
        btnToggleExpand = ImageView(context).apply {
            setImageResource(android.R.drawable.arrow_up_float)
            rotation = 90f
            layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
            setColorFilter(0xAAFFFFFF.toInt())
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))

            setOnClickListener {
                vibrateQuick()
                toggleExpand()
            }
        }
        mainBar.addView(btnToggleExpand)

        rootView.addView(mainBar)

        // Submenú desplegable de Herramientas
        toolsSubmenuLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = createCardDrawable(0xEE1E232A.toInt(), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
            }
        }

        // Opción 1: Captura de pantalla
        val btnScreenshot = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(0x33FFFFFF.toInt(), dpToPx(12))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }

            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_camera)
                layoutParams = LinearLayout.LayoutParams(dpToPx(15), dpToPx(15)).apply {
                    marginEnd = dpToPx(4)
                }
                setColorFilter(Color.WHITE)
            }
            addView(icon)

            val tv = TextView(context).apply {
                text = "Captura"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tv)

            setOnClickListener {
                vibrateQuick()
                toggleToolsMenu()
                onScreenshotClicked()
            }
        }
        toolsSubmenuLayout.addView(btnScreenshot)

        // Opción 2: Lapicero / Pincel en pantalla
        val btnDraw = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(0x33FFFFFF.toInt(), dpToPx(12))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                layoutParams = LinearLayout.LayoutParams(dpToPx(15), dpToPx(15)).apply {
                    marginEnd = dpToPx(4)
                }
                setColorFilter(Color.WHITE)
            }
            addView(icon)

            val tv = TextView(context).apply {
                text = "Pincel"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tv)

            setOnClickListener {
                vibrateQuick()
                toggleToolsMenu()
                onDrawToolClicked()
            }
        }
        toolsSubmenuLayout.addView(btnDraw)

        rootView.addView(toolsSubmenuLayout)
    }

    fun toggleExpand() {
        isExpanded = !isExpanded
        actionControlsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        if (!isExpanded) {
            toolsSubmenuLayout.visibility = View.GONE
            isToolsMenuOpen = false
        }
        btnToggleExpand.rotation = if (isExpanded) 90f else 270f
    }

    fun toggleToolsMenu() {
        isToolsMenuOpen = !isToolsMenuOpen
        toolsSubmenuLayout.visibility = if (isToolsMenuOpen) View.VISIBLE else View.GONE
    }

    fun updateTimer(elapsedSeconds: Int) {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        tvTimer.post {
            tvTimer.text = timeString
        }
    }

    fun updateStatus(paused: Boolean) {
        this.isPaused = paused
        rootView.post {
            if (paused) {
                dotIndicator.background = createCircleDrawable(COLOR_PAUSED_AMBER)
                iconPauseResume.setImageResource(android.R.drawable.ic_media_play)
                tvPauseResumeLabel.text = "Reanudar"
                dotPulseAnimator?.pause()
                dotIndicator.alpha = 1.0f
            } else {
                dotIndicator.background = createCircleDrawable(COLOR_RECORDING_RED)
                iconPauseResume.setImageResource(android.R.drawable.ic_media_pause)
                tvPauseResumeLabel.text = "Pausar"
                if (dotPulseAnimator?.isPaused == true) {
                    dotPulseAnimator?.resume()
                } else {
                    startPulse()
                }
            }
        }
    }

    fun updateMicStatus(muted: Boolean) {
        this.isMicMuted = muted
        rootView.post {
            if (muted) {
                // Solo audio del juego grabado (micrófono silenciado)
                btnMicToggle.background = createCardDrawable(COLOR_MIC_OFF_BG, dpToPx(14))
                iconMicToggle.setColorFilter(COLOR_MIC_OFF_TEXT)
                tvMicToggleLabel.text = "Solo Juego"
                tvMicToggleLabel.setTextColor(COLOR_MIC_OFF_TEXT)
            } else {
                // Juego + Voz del usuario (micrófono activo)
                btnMicToggle.background = createCardDrawable(COLOR_MIC_ON_BG, dpToPx(14))
                iconMicToggle.setColorFilter(COLOR_MIC_ON_TEXT)
                tvMicToggleLabel.text = "Voz ON"
                tvMicToggleLabel.setTextColor(COLOR_MIC_ON_TEXT)
            }
        }
    }

    fun startPulse() {
        dotPulseAnimator?.cancel()
        dotPulseAnimator = ObjectAnimator.ofFloat(dotIndicator, "alpha", 1.0f, 0.3f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }

    fun stopPulse() {
        dotPulseAnimator?.cancel()
        dotPulseAnimator = null
    }

    private fun createCardDrawable(color: Int, cornerRadiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx.toFloat()
            setStroke(dpToPx(1), 0x33FFFFFF.toInt())
        }
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun vibrateQuick() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }
}
