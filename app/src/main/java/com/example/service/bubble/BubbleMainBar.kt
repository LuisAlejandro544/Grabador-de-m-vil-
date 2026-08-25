package com.example.service.bubble

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/**
 * Barra principal horizontal del widget flotante.
 * Diseñada de forma compacta y adaptativa para verse impecable tanto en vertical como en horizontal.
 * Contiene el led pulsante, cronómetro monospace, botones rápidos (Pausar, Micrófono, HUD, Parar) y flecha de colapso.
 */
class BubbleMainBar(
    private val context: Context,
    private val onPauseClicked: () -> Unit,
    private val onResumeClicked: () -> Unit,
    private val onMicToggleClicked: () -> Unit,
    private val onToolsToggleClicked: () -> Unit,
    private val onStopClicked: () -> Unit,
    private val onExpandToggleClicked: () -> Unit,
    private val onVibrateRequested: () -> Unit
) {

    val layout: LinearLayout
    private val dotIndicator: View
    private val tvTimer: TextView
    val actionControlsLayout: LinearLayout

    private val iconPauseResume: ImageView
    private val tvPauseResumeLabel: TextView

    private val btnMicToggle: LinearLayout
    private val iconMicToggle: ImageView
    private val tvMicToggleLabel: TextView

    private val btnTools: LinearLayout
    private val iconTools: ImageView
    private val tvToolsLabel: TextView

    val btnToggleExpand: ImageView
    private var dotPulseAnimator: ObjectAnimator? = null

    init {
        layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BG_DARK, dp(20))
        }

        // 1. Indicador en vivo (Punto pulsante)
        dotIndicator = View(context).apply {
            val dotSize = dp(8)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dp(6)
            }
            background = BubbleDrawables.createCircleDrawable(BubbleColors.COLOR_RECORDING_RED)
        }
        layout.addView(dotIndicator)

        // 2. Cronómetro Monospace
        tvTimer = TextView(context).apply {
            text = "00:00"
            setTextColor(BubbleColors.COLOR_TEXT_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
            }
        }
        layout.addView(tvTimer)

        // 3. Contenedor de acciones rápidas
        actionControlsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Botón Pausa / Reanudar
        val btnPauseResume = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(4)
            }
        }

        iconPauseResume = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                marginEnd = dp(3)
            }
            setColorFilter(Color.WHITE)
        }
        btnPauseResume.addView(iconPauseResume)

        tvPauseResumeLabel = TextView(context).apply {
            text = "Pausa"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        btnPauseResume.addView(tvPauseResumeLabel)

        btnPauseResume.setOnClickListener {
            onVibrateRequested()
            if (isPausedState) onResumeClicked() else onPauseClicked()
        }
        actionControlsLayout.addView(btnPauseResume)

        // Botón Micrófono Dinámico
        btnMicToggle = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_MIC_ON_BG, dp(12))
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(4)
            }
        }

        iconMicToggle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                marginEnd = dp(3)
            }
            setColorFilter(BubbleColors.COLOR_MIC_ON_TEXT)
        }
        btnMicToggle.addView(iconMicToggle)

        tvMicToggleLabel = TextView(context).apply {
            text = "Mic"
            setTextColor(BubbleColors.COLOR_MIC_ON_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        btnMicToggle.addView(tvMicToggleLabel)

        btnMicToggle.setOnClickListener {
            onVibrateRequested()
            onMicToggleClicked()
        }
        actionControlsLayout.addView(btnMicToggle)

        // Botón Herramientas / HUD
        btnTools = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_TOOLS_BG, dp(12))
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(4)
            }

            iconTools = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_manage)
                layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                    marginEnd = dp(3)
                }
                setColorFilter(0xFF818CF8.toInt())
            }
            addView(iconTools)

            tvToolsLabel = TextView(context).apply {
                text = "HUD"
                setTextColor(0xFF818CF8.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(tvToolsLabel)

            setOnClickListener {
                onVibrateRequested()
                onToolsToggleClicked()
            }
        }
        actionControlsLayout.addView(btnTools)

        // Botón Parar
        val btnStop = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_STOP_BG, dp(12))
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(4)
            }

            val iconStop = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                    marginEnd = dp(3)
                }
                setColorFilter(BubbleColors.COLOR_RECORDING_RED)
            }
            addView(iconStop)

            val tvStopLabel = TextView(context).apply {
                text = "Fin"
                setTextColor(BubbleColors.COLOR_RECORDING_RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(tvStopLabel)

            setOnClickListener {
                onVibrateRequested()
                onStopClicked()
            }
        }
        actionControlsLayout.addView(btnStop)

        layout.addView(actionControlsLayout)

        // 4. Botón colapsar / expandir
        btnToggleExpand = ImageView(context).apply {
            setImageResource(android.R.drawable.arrow_up_float)
            rotation = 90f
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            setColorFilter(0xAAFFFFFF.toInt())
            setPadding(dp(1), dp(1), dp(1), dp(1))

            setOnClickListener {
                onVibrateRequested()
                onExpandToggleClicked()
            }
        }
        layout.addView(btnToggleExpand)
    }

    private var isPausedState = false

    fun updateTimer(elapsedSeconds: Int) {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        tvTimer.post {
            tvTimer.text = timeString
        }
    }

    fun updateStatus(paused: Boolean) {
        this.isPausedState = paused
        layout.post {
            if (paused) {
                dotIndicator.background = BubbleDrawables.createCircleDrawable(BubbleColors.COLOR_PAUSED_AMBER)
                iconPauseResume.setImageResource(android.R.drawable.ic_media_play)
                tvPauseResumeLabel.text = "Seguir"
                dotPulseAnimator?.pause()
                dotIndicator.alpha = 1.0f
            } else {
                dotIndicator.background = BubbleDrawables.createCircleDrawable(BubbleColors.COLOR_RECORDING_RED)
                iconPauseResume.setImageResource(android.R.drawable.ic_media_pause)
                tvPauseResumeLabel.text = "Pausa"
                if (dotPulseAnimator?.isPaused == true) {
                    dotPulseAnimator?.resume()
                } else {
                    startPulse()
                }
            }
        }
    }

    fun updateMicStatus(muted: Boolean) {
        layout.post {
            if (muted) {
                btnMicToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_MIC_OFF_BG, dp(12))
                iconMicToggle.setColorFilter(BubbleColors.COLOR_MIC_OFF_TEXT)
                tvMicToggleLabel.text = "Mute"
                tvMicToggleLabel.setTextColor(BubbleColors.COLOR_MIC_OFF_TEXT)
            } else {
                btnMicToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_MIC_ON_BG, dp(12))
                iconMicToggle.setColorFilter(BubbleColors.COLOR_MIC_ON_TEXT)
                tvMicToggleLabel.text = "Mic"
                tvMicToggleLabel.setTextColor(BubbleColors.COLOR_MIC_ON_TEXT)
            }
        }
    }

    fun updateToolsStatus(isOpen: Boolean) {
        layout.post {
            if (isOpen) {
                btnTools.background = BubbleDrawables.createCardDrawable(
                    context = context,
                    color = BubbleColors.COLOR_TOOLS_ACTIVE_BG,
                    cornerRadiusPx = dp(12),
                    strokeColor = 0xFFA5B4FC.toInt(),
                    strokeWidthDp = 1
                )
                iconTools.setColorFilter(Color.WHITE)
                tvToolsLabel.setTextColor(Color.WHITE)
            } else {
                btnTools.background = BubbleDrawables.createCardDrawable(
                    context = context,
                    color = BubbleColors.COLOR_TOOLS_BG,
                    cornerRadiusPx = dp(12)
                )
                iconTools.setColorFilter(0xFF818CF8.toInt())
                tvToolsLabel.setTextColor(0xFF818CF8.toInt())
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

    private fun dp(dpValue: Int): Int = BubbleDrawables.dpToPx(context, dpValue)
}
