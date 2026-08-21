package com.example.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import java.util.Locale

/**
 * Gestor de la burbuja flotante / widget superpuesto durante la grabación.
 * Muestra el tiempo transcurrido en tiempo real y ofrece controles de pausa,
 * reanudación y parada rápida sin salir del juego o aplicación actual.
 */
class FloatingBubbleManager(
    private val context: Context,
    private val onPauseClicked: () -> Unit,
    private val onResumeClicked: () -> Unit,
    private val onStopClicked: () -> Unit
) {

    companion object {
        private const val TAG = "FloatingBubbleManager"
        private const val COLOR_BG_DARK = 0xEE1E232A.toInt()
        private const val COLOR_RECORDING_RED = 0xFFEF4444.toInt()
        private const val COLOR_PAUSED_AMBER = 0xFFF59E0B.toInt()
        private const val COLOR_TEXT_WHITE = 0xFFF8FAFC.toInt()
        private const val COLOR_BTN_BG = 0x33FFFFFF.toInt()
        private const val COLOR_STOP_BG = 0x55EF4444.toInt()
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var overlayView: View? = null
    private var isShowing = false
    private var isExpanded = true
    private var isPaused = false

    private var tvTimer: TextView? = null
    private var dotIndicator: View? = null
    private var actionControlsLayout: LinearLayout? = null
    private var btnPauseResume: LinearLayout? = null
    private var iconPauseResume: ImageView? = null
    private var tvPauseResumeLabel: TextView? = null
    private var btnStop: LinearLayout? = null
    private var btnToggleExpand: ImageView? = null

    private var dotPulseAnimator: ObjectAnimator? = null

    private var params: WindowManager.LayoutParams? = null

    fun isOverlayAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowing || !isOverlayAvailable() || windowManager == null) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(16)
                y = dpToPx(120)
            }

            val view = createBubbleLayout()
            overlayView = view
            setupTouchDrag(view)

            windowManager.addView(view, params)
            isShowing = true
            startDotPulse()

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando la burbuja flotante: ${e.message}", e)
            isShowing = false
            overlayView = null
        }
    }

    fun updateTime(elapsedSeconds: Int) {
        if (!isShowing) return
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        tvTimer?.post {
            tvTimer?.text = timeString
        }
    }

    fun updateStatus(paused: Boolean) {
        if (!isShowing) return
        this.isPaused = paused
        overlayView?.post {
            if (paused) {
                dotIndicator?.background = createCircleDrawable(COLOR_PAUSED_AMBER)
                iconPauseResume?.setImageResource(android.R.drawable.ic_media_play)
                tvPauseResumeLabel?.text = "Reanudar"
                dotPulseAnimator?.pause()
                dotIndicator?.alpha = 1.0f
            } else {
                dotIndicator?.background = createCircleDrawable(COLOR_RECORDING_RED)
                iconPauseResume?.setImageResource(android.R.drawable.ic_media_pause)
                tvPauseResumeLabel?.text = "Pausar"
                if (dotPulseAnimator?.isPaused == true) {
                    dotPulseAnimator?.resume()
                } else {
                    startDotPulse()
                }
            }
        }
    }

    fun dismiss() {
        if (!isShowing) return
        try {
            dotPulseAnimator?.cancel()
            dotPulseAnimator = null
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al remover burbuja flotante: ${e.message}")
        } finally {
            overlayView = null
            isShowing = false
        }
    }

    private fun createBubbleLayout(): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = createCardDrawable(COLOR_BG_DARK, dpToPx(24))
            elevation = dpToPx(8).toFloat()
        }

        // 1. Live Indicator (Red/Amber pulsing circle)
        dotIndicator = View(context).apply {
            val dotSize = dpToPx(10)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dpToPx(8)
            }
            background = createCircleDrawable(if (isPaused) COLOR_PAUSED_AMBER else COLOR_RECORDING_RED)
        }
        rootLayout.addView(dotIndicator)

        // 2. Timer Label (Monospace text)
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
                marginEnd = dpToPx(10)
            }
        }
        rootLayout.addView(tvTimer)

        // 3. Action Controls Container (Pause/Resume + Stop buttons)
        actionControlsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Pause/Resume Button
        btnPauseResume = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_BTN_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(10), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }

            iconPauseResume = ImageView(context).apply {
                setImageResource(if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
                layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                    marginEnd = dpToPx(4)
                }
                setColorFilter(Color.WHITE)
            }
            addView(iconPauseResume)

            tvPauseResumeLabel = TextView(context).apply {
                text = if (isPaused) "Reanudar" else "Pausar"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvPauseResumeLabel)

            setOnClickListener {
                vibrateQuick()
                if (isPaused) {
                    onResumeClicked()
                } else {
                    onPauseClicked()
                }
            }
        }
        actionControlsLayout?.addView(btnPauseResume)

        // Stop Button
        btnStop = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCardDrawable(COLOR_STOP_BG, dpToPx(14))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(10), dpToPx(4))
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
        actionControlsLayout?.addView(btnStop)

        rootLayout.addView(actionControlsLayout)

        // 4. Toggle Expand / Collapse arrow button
        btnToggleExpand = ImageView(context).apply {
            setImageResource(android.R.drawable.arrow_up_float)
            rotation = 90f
            layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
            setColorFilter(0xAAFFFFFF.toInt())
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))

            setOnClickListener {
                vibrateQuick()
                toggleExpandState()
            }
        }
        rootLayout.addView(btnToggleExpand)

        return rootLayout
    }

    private fun toggleExpandState() {
        isExpanded = !isExpanded
        actionControlsLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        btnToggleExpand?.rotation = if (isExpanded) 90f else 270f
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchDrag(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        p.x = initialX + dx
                        p.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(view, p)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tapped on the bubble outside child buttons -> toggle expand
                        toggleExpandState()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startDotPulse() {
        dotIndicator?.let { dot ->
            dotPulseAnimator?.cancel()
            dotPulseAnimator = ObjectAnimator.ofFloat(dot, "alpha", 1.0f, 0.3f).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        }
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
            // Ignore
        }
    }
}
