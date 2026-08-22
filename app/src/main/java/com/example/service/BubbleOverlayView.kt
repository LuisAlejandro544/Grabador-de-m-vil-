package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.example.service.bubble.BubbleDrawables
import com.example.service.bubble.BubbleMainBar
import com.example.service.bubble.BubbleToolsSubmenu

/**
 * Componente visual modular del widget flotante de grabación.
 * Coordina la barra principal [BubbleMainBar], el submenú de herramientas [BubbleToolsSubmenu]
 * y el estado de los interruptores rápidos.
 */
class BubbleOverlayView(
    private val context: Context,
    private val onPauseClicked: () -> Unit,
    private val onResumeClicked: () -> Unit,
    private val onStopClicked: () -> Unit,
    private val onMicToggleClicked: () -> Unit,
    private val onScreenshotClicked: () -> Unit,
    private val onDrawToolClicked: () -> Unit,
    private val onFacecamToggleClicked: () -> Unit = {},
    private val onBeautyToggleClicked: () -> Unit = {},
    private val onRgbBorderToggleClicked: () -> Unit = {},
    private val onTouchToggleClicked: () -> Unit = {},
    private val onWatermarkToggleClicked: () -> Unit = {},
    private val onSceneOverlayToggleClicked: () -> Unit = {}
) {

    val rootView: LinearLayout
    private val mainBar: BubbleMainBar
    private val toolsSubmenu: BubbleToolsSubmenu

    var isExpanded: Boolean = true
        private set
    var isPaused: Boolean = false
        private set
    var isMicMuted: Boolean = false
        private set
    var isFacecamActive: Boolean = false
        private set
    var isBeautyActive: Boolean = false
        private set
    var isRgbActive: Boolean = false
        private set
    var isTouchActive: Boolean = false
        private set
    var isWatermarkActive: Boolean = false
        private set
    var isSceneOverlayActive: Boolean = false
        private set
    var isToolsMenuOpen: Boolean = false
        private set

    init {
        rootView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            elevation = BubbleDrawables.dpToPx(context, 8).toFloat()
        }

        mainBar = BubbleMainBar(
            context = context,
            onPauseClicked = onPauseClicked,
            onResumeClicked = onResumeClicked,
            onMicToggleClicked = onMicToggleClicked,
            onToolsToggleClicked = { toggleToolsMenu() },
            onStopClicked = onStopClicked,
            onExpandToggleClicked = { toggleExpand() },
            onVibrateRequested = { vibrateQuick() }
        )
        rootView.addView(mainBar.layout)

        toolsSubmenu = BubbleToolsSubmenu(
            context = context,
            onScreenshotClicked = {
                toggleToolsMenu()
                onScreenshotClicked()
            },
            onDrawToolClicked = {
                toggleToolsMenu()
                onDrawToolClicked()
            },
            onFacecamToggleClicked = onFacecamToggleClicked,
            onBeautyToggleClicked = onBeautyToggleClicked,
            onRgbBorderToggleClicked = onRgbBorderToggleClicked,
            onTouchToggleClicked = onTouchToggleClicked,
            onWatermarkToggleClicked = onWatermarkToggleClicked,
            onSceneOverlayToggleClicked = onSceneOverlayToggleClicked,
            onVibrateRequested = { vibrateQuick() }
        )
        rootView.addView(toolsSubmenu.layout)
    }

    fun toggleExpand() {
        isExpanded = !isExpanded
        mainBar.actionControlsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        if (!isExpanded) {
            toolsSubmenu.layout.visibility = View.GONE
            isToolsMenuOpen = false
        }
        mainBar.btnToggleExpand.rotation = if (isExpanded) 90f else 270f
    }

    fun toggleToolsMenu() {
        isToolsMenuOpen = !isToolsMenuOpen
        toolsSubmenu.layout.visibility = if (isToolsMenuOpen) View.VISIBLE else View.GONE
    }

    fun updateTimer(elapsedSeconds: Int) {
        mainBar.updateTimer(elapsedSeconds)
    }

    fun updateStatus(paused: Boolean) {
        this.isPaused = paused
        mainBar.updateStatus(paused)
    }

    fun updateMicStatus(muted: Boolean) {
        this.isMicMuted = muted
        mainBar.updateMicStatus(muted)
    }

    fun updateFacecamStatus(active: Boolean) {
        this.isFacecamActive = active
        rootView.post {
            toolsSubmenu.updateFacecamStatus(active)
        }
    }

    fun updateBeautyStatus(active: Boolean) {
        this.isBeautyActive = active
        rootView.post {
            toolsSubmenu.updateBeautyStatus(active)
        }
    }

    fun updateRgbStatus(active: Boolean) {
        this.isRgbActive = active
        rootView.post {
            toolsSubmenu.updateRgbStatus(active)
        }
    }

    fun updateTouchStatus(active: Boolean) {
        this.isTouchActive = active
        rootView.post {
            toolsSubmenu.updateTouchStatus(active)
        }
    }

    fun updateWatermarkStatus(active: Boolean) {
        this.isWatermarkActive = active
        rootView.post {
            toolsSubmenu.updateWatermarkStatus(active)
        }
    }

    fun updateSceneOverlayStatus(active: Boolean) {
        this.isSceneOverlayActive = active
        rootView.post {
            toolsSubmenu.updateSceneOverlayStatus(active)
        }
    }

    fun startPulse() {
        mainBar.startPulse()
    }

    fun stopPulse() {
        mainBar.stopPulse()
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
        } catch (_: Exception) {
        }
    }
}
