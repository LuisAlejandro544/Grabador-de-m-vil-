package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lienzo transparente en tiempo real y barra de herramientas flotante de dibujo sobre la pantalla.
 * Permite realizar anotaciones, flechas o marcas con el dedo durante una grabación de video o partida.
 */
class ScreenDrawingOverlay(
    private val context: Context,
    private val onClose: () -> Unit
) {

    companion object {
        private const val TAG = "ScreenDrawingOverlay"

        private val COLORS = listOf(
            0xFFEF4444.toInt(), // Rojo vibrante
            0xFFFACC15.toInt(), // Amarillo neón
            0xFF22C55E.toInt(), // Verde esmeralda
            0xFF3B82F6.toInt(), // Azul eléctrico
            0xFFFFFFFF.toInt()  // Blanco puro
        )
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var isShowingInternal = false
    val isShowing: Boolean get() = isShowingInternal

    private var rootLayout: FrameLayout? = null
    private var drawCanvasView: DrawCanvasView? = null
    private var currentColorIndex = 0
    private var currentStrokeWidthPx = dpToPx(5)

    data class PathItem(val path: Path, val paint: Paint)

    /**
     * Vista interna de lienzo acelerada por hardware para dibujar trazos suaves.
     */
    private inner class DrawCanvasView(ctx: Context) : View(ctx) {
        private val paths = mutableListOf<PathItem>()
        private var activePath = Path()
        private var lastX = 0f
        private var lastY = 0f

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        private fun createPaintForCurrentColor(): Paint {
            return Paint().apply {
                color = COLORS[currentColorIndex]
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = currentStrokeWidthPx.toFloat()
                isAntiAlias = true
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activePath = Path()
                    activePath.moveTo(x, y)
                    lastX = x
                    lastY = y
                    paths.add(PathItem(activePath, createPaintForCurrentColor()))
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = Math.abs(x - lastX)
                    val dy = Math.abs(y - lastY)
                    if (dx >= 4 || dy >= 4) {
                        activePath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                        lastX = x
                        lastY = y
                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    activePath.lineTo(x, y)
                    invalidate()
                    return true
                }

                else -> return false
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            for (item in paths) {
                canvas.drawPath(item.path, item.paint)
            }
        }

        fun undo() {
            if (paths.isNotEmpty()) {
                paths.removeAt(paths.size - 1)
                invalidate()
            }
        }

        fun clear() {
            paths.clear()
            invalidate()
        }

        fun hasDrawings(): Boolean = paths.isNotEmpty()
    }

    /**
     * Muestra el lienzo de dibujo a pantalla completa y la barra de herramientas.
     */
    fun show() {
        if (isShowingInternal || windowManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

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
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            val root = FrameLayout(context).apply {
                setBackgroundColor(0x00000000)
            }
            this.rootLayout = root

            val canvas = DrawCanvasView(context)
            this.drawCanvasView = canvas
            root.addView(canvas, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            // Barra de control de dibujo flotante superior
            val toolbar = buildToolbarLayout()
            val toolbarParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(48)
            }
            root.addView(toolbar, toolbarParams)

            windowManager.addView(root, params)
            isShowingInternal = true
            vibrateQuick()

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay de dibujo: ${e.message}", e)
            dismiss()
        }
    }

    private fun buildToolbarLayout(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xEE181E29.toInt())
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), 0x44FFFFFF.toInt())
            }
            elevation = dpToPx(12).toFloat()
        }

        // 1. Indicador / Botón de cambio de color
        val colorBtn = View(context).apply {
            val size = dpToPx(26)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(12)
            }
            background = createColorPreviewDrawable(COLORS[currentColorIndex])
            setOnClickListener {
                vibrateQuick()
                currentColorIndex = (currentColorIndex + 1) % COLORS.size
                background = createColorPreviewDrawable(COLORS[currentColorIndex])
            }
        }
        container.addView(colorBtn)

        // 2. Botón de Grosor (Fino / Grueso)
        val strokeBtn = TextView(context).apply {
            text = "✏️ 5px"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0x33FFFFFF.toInt())
                cornerRadius = dpToPx(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                vibrateQuick()
                currentStrokeWidthPx = when (currentStrokeWidthPx) {
                    dpToPx(3) -> { text = "✏️ 6px"; dpToPx(6) }
                    dpToPx(6) -> { text = "✏️ 10px"; dpToPx(10) }
                    else -> { text = "✏️ 3px"; dpToPx(3) }
                }
            }
        }
        container.addView(strokeBtn)

        // 3. Botón Deshacer (Undo)
        val undoBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(Color.WHITE)
            val size = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                vibrateQuick()
                drawCanvasView?.undo()
            }
        }
        container.addView(undoBtn)

        // 4. Botón Limpiar todo (Clear)
        val clearBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(0xFFFF8888.toInt())
            val size = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(12)
            }
            setOnClickListener {
                vibrateQuick()
                drawCanvasView?.clear()
            }
        }
        container.addView(clearBtn)

        // 5. Botón Cerrar modo dibujo
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            val size = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                vibrateQuick()
                dismiss()
                onClose()
            }
        }
        container.addView(closeBtn)

        return container
    }

    private fun createColorPreviewDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(2), Color.WHITE)
        }
    }

    /**
     * Oculta y remueve el lienzo de dibujo.
     */
    fun dismiss() {
        if (!isShowingInternal) return
        try {
            rootLayout?.let { root ->
                windowManager?.removeView(root)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removiendo vista de dibujo: ${e.message}")
        } finally {
            rootLayout = null
            drawCanvasView = null
            isShowingInternal = false
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
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }
}
