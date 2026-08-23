package com.example.service.vumeter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * Interfaz de usuario del Vúmetro Flotante y Mezclador de Audio de Vortex Studio.
 * Desacoplada, compacta, con soporte para arrastre y controles en tiempo real.
 */
@SuppressLint("ViewConstructor")
class FloatingVuMeterOverlayView(
    context: Context,
    private val initialGameGain: Float,
    private val initialMicGain: Float,
    private val isGameMutedInitial: Boolean,
    private val isMicMutedInitial: Boolean,
    private val isDuckingActiveInitial: Boolean,
    private val isNoiseGateActiveInitial: Boolean,
    private val onGameGainChanged: (Float) -> Unit,
    private val onMicGainChanged: (Float) -> Unit,
    private val onGameMuteToggled: (Boolean) -> Unit,
    private val onMicMuteToggled: (Boolean) -> Unit,
    private val onDuckingToggled: (Boolean) -> Unit,
    private val onNoiseGateToggled: (Boolean) -> Unit,
    private val onCloseClicked: () -> Unit
) : FrameLayout(context) {

    private val gameLevelBar: AudioLevelBarView
    private val micLevelBar: AudioLevelBarView
    private val gameGainValueTv: TextView
    private val micGainValueTv: TextView
    private val gameMuteBtn: TextView
    private val micMuteBtn: TextView
    private val duckingChip: TextView
    private val noiseGateChip: TextView
    private val bodyContainer: LinearLayout
    private val headerTitleTv: TextView
    private val collapseBtn: TextView

    private var isCollapsed: Boolean = false
    private var isGameMuted: Boolean = isGameMutedInitial
    private var isMicMuted: Boolean = isMicMutedInitial
    private var isDucking: Boolean = isDuckingActiveInitial
    private var isNoiseGate: Boolean = isNoiseGateActiveInitial

    init {
        layoutParams = LayoutParams(dp(260), LayoutParams.WRAP_CONTENT)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(0xF00F172A.toInt()) // Slate 900 translúcido
                setStroke(dp(1), 0xFF0284C7.toInt()) // Borde Azul Neón Gamer
            }
            background = bg
            setPadding(dp(10), dp(8), dp(10), dp(10))
            elevation = dp(8).toFloat()
        }

        // 1. Header (Título, botón colapsar y botón cerrar)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, dp(6))
        }

        headerTitleTv = TextView(context).apply {
            text = "🎚️ Mezclador de Audio"
            setTextColor(0xFF38BDF8.toInt()) // Cyan 400
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        collapseBtn = TextView(context).apply {
            text = "➖"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            setOnClickListener {
                toggleCollapse()
            }
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(0xFFEF4444.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(4), dp(2))
            setOnClickListener {
                onCloseClicked()
            }
        }

        header.addView(headerTitleTv)
        header.addView(collapseBtn)
        header.addView(closeBtn)
        root.addView(header)

        // 2. Body Container (Canales y Controles)
        bodyContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- Canal 1: Audio del Juego (Interno) ---
        val gameRow = createChannelRow(
            title = "🎮 Juego",
            iconColor = 0xFF10B981.toInt(),
            initialGain = initialGameGain
        )
        gameLevelBar = gameRow.first
        gameGainValueTv = gameRow.second
        val gameSeekBar = gameRow.third
        gameMuteBtn = gameRow.fourth

        gameSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val gain = progress / 100f
                    gameGainValueTv.text = "${(gain * 100).toInt()}%"
                    onGameGainChanged(gain)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        gameMuteBtn.setOnClickListener {
            isGameMuted = !isGameMuted
            updateMuteButtonUi(gameMuteBtn, isGameMuted)
            onGameMuteToggled(isGameMuted)
        }
        updateMuteButtonUi(gameMuteBtn, isGameMuted)

        bodyContainer.addView(gameRow.fifth)

        // Separador
        bodyContainer.addView(createDivider())

        // --- Canal 2: Audio de Micrófono / Voz ---
        val micRow = createChannelRow(
            title = "🎙️ Mic / Voz",
            iconColor = 0xFF38BDF8.toInt(),
            initialGain = initialMicGain
        )
        micLevelBar = micRow.first
        micGainValueTv = micRow.second
        val micSeekBar = micRow.third
        micMuteBtn = micRow.fourth

        micSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val gain = progress / 100f
                    micGainValueTv.text = "${(gain * 100).toInt()}%"
                    onMicGainChanged(gain)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        micMuteBtn.setOnClickListener {
            isMicMuted = !isMicMuted
            updateMuteButtonUi(micMuteBtn, isMicMuted)
            onMicMuteToggled(isMicMuted)
        }
        updateMuteButtonUi(micMuteBtn, isMicMuted)

        bodyContainer.addView(micRow.fifth)

        // Separador
        bodyContainer.addView(createDivider())

        // --- Chips de Filtros Rápidos (Ducking & Noise Gate) ---
        val filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(4), 0, 0)
        }

        duckingChip = createFilterChip(
            label = "🦆 Ducking",
            isActive = isDucking,
            onClick = {
                isDucking = !isDucking
                updateFilterChipUi(duckingChip, isDucking, "🦆 Ducking")
                onDuckingToggled(isDucking)
            }
        )

        noiseGateChip = createFilterChip(
            label = "🛡️ Noise Gate",
            isActive = isNoiseGate,
            onClick = {
                isNoiseGate = !isNoiseGate
                updateFilterChipUi(noiseGateChip, isNoiseGate, "🛡️ Noise Gate")
                onNoiseGateToggled(isNoiseGate)
            }
        )

        filterRow.addView(duckingChip)
        filterRow.addView(createSpace(dp(8)))
        filterRow.addView(noiseGateChip)

        bodyContainer.addView(filterRow)
        root.addView(bodyContainer)
        addView(root)
    }

    private fun toggleCollapse() {
        isCollapsed = !isCollapsed
        if (isCollapsed) {
            bodyContainer.visibility = View.GONE
            collapseBtn.text = "➕"
            headerTitleTv.text = "🎚️ Audio Mix"
        } else {
            bodyContainer.visibility = View.VISIBLE
            collapseBtn.text = "➖"
            headerTitleTv.text = "🎚️ Mezclador de Audio"
        }
    }

    fun updateLevels(gameLevel: Float, micLevel: Float) {
        post {
            val effGame = if (isGameMuted) 0f else gameLevel
            val effMic = if (isMicMuted) 0f else micLevel
            gameLevelBar.setLevel(effGame)
            micLevelBar.setLevel(effMic)
        }
    }

    fun updateMicMuteStatus(muted: Boolean) {
        this.isMicMuted = muted
        post {
            updateMuteButtonUi(micMuteBtn, muted)
        }
    }

    private fun createChannelRow(
        title: String,
        iconColor: Int,
        initialGain: Float
    ): ChannelRowResult {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(2), 0, dp(4))
        }

        // Top Row: Nombre de canal, ganancia % y botón Mute
        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameTv = TextView(context).apply {
            text = title
            setTextColor(iconColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val gainTv = TextView(context).apply {
            text = "${(initialGain * 100).toInt()}%"
            setTextColor(0xFFE2E8F0.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, dp(8), 0)
        }

        val muteBtn = TextView(context).apply {
            text = "🔊"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dp(6), dp(1), dp(6), dp(1))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0xFF1E293B.toInt())
            }
            background = bg
        }

        topRow.addView(nameTv)
        topRow.addView(gainTv)
        topRow.addView(muteBtn)
        container.addView(topRow)

        // Middle: Vúmetro LED
        val vuBar = AudioLevelBarView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(2)
            }
        }
        container.addView(vuBar)

        // Bottom: SeekBar de Ganancia (0% a 200%)
        val seekBar = SeekBar(context).apply {
            max = 200
            progress = (initialGain * 100).toInt().coerceIn(0, 200)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        container.addView(seekBar)

        return ChannelRowResult(vuBar, gainTv, seekBar, muteBtn, container)
    }

    private fun updateMuteButtonUi(btn: TextView, isMuted: Boolean) {
        if (isMuted) {
            btn.text = "🔇 MUTE"
            btn.setTextColor(0xFFEF4444.toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0x44EF4444.toInt())
                setStroke(dp(1), 0xFFEF4444.toInt())
            }
            btn.background = bg
        } else {
            btn.text = "🔊 ON"
            btn.setTextColor(0xFF10B981.toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0xFF1E293B.toInt())
                setStroke(dp(1), 0x4410B981.toInt())
            }
            btn.background = bg
        }
    }

    private fun createFilterChip(
        label: String,
        isActive: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            updateFilterChipUi(this, isActive, label)
        }
    }

    private fun updateFilterChipUi(chip: TextView, isActive: Boolean, label: String) {
        chip.text = label
        if (isActive) {
            chip.setTextColor(0xFF38BDF8.toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0x330284C7.toInt())
                setStroke(dp(1), 0xFF0284C7.toInt())
            }
            chip.background = bg
        } else {
            chip.setTextColor(0xFF64748B.toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0xFF1E293B.toInt())
                setStroke(dp(1), 0xFF334155.toInt())
            }
            chip.background = bg
        }
    }

    private fun createDivider(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            setBackgroundColor(0x33334155.toInt())
        }
    }

    private fun createSpace(sizePx: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, 1)
        }
    }

    private fun dp(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private data class ChannelRowResult(
        val first: AudioLevelBarView,
        val second: TextView,
        val third: SeekBar,
        val fourth: TextView,
        val fifth: View
    )
}
