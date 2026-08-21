package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.model.FacecamShape
import com.example.model.FacecamSize

/**
 * Gestor de la cámara flotante Facecam sobre la pantalla.
 * Permite mostrar la vista previa en vivo de la cámara frontal o trasera durante la grabación
 * con múltiples formas geométricas (Circular, Cuadrado, Cuadrado Redondeado y Rectangular),
 * tamaños dinámicos, borde estilizado y controles táctiles de arrastre y cambio de cámara.
 */
class FacecamOverlayManager(
    private val context: Context,
    private var shape: FacecamShape = FacecamShape.CIRCLE,
    private var size: FacecamSize = FacecamSize.MEDIUM,
    private var isFrontCamera: Boolean = true,
    private val onCloseClicked: (() -> Unit)? = null,
    private val onShapeChanged: ((FacecamShape) -> Unit)? = null,
    private val onCameraFlipped: ((Boolean) -> Unit)? = null
) {

    companion object {
        private const val TAG = "FacecamOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var rootContainer: FrameLayout? = null
    private var cameraCard: FrameLayout? = null
    private var previewView: PreviewView? = null
    private var overlayControls: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private var isShowingInternal = false
    val isShowing: Boolean get() = isShowingInternal

    private val lifecycleOwner = FacecamLifecycleOwner()
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Ciclo de vida desacoplado para vincular CameraX de forma segura en un Service.
     */
    private class FacecamLifecycleOwner : LifecycleOwner {
        private var lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry

        fun start() {
            try {
                if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
                    lifecycleRegistry = LifecycleRegistry(this)
                    lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
                }
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar estado de ciclo de vida Facecam: ${e.message}")
            }
        }

        fun stop() {
            try {
                if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
                    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo ciclo de vida Facecam: ${e.message}")
            }
        }
    }

    fun isOverlayAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowingInternal || !isOverlayAvailable() || windowManager == null) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val (widthPx, heightPx) = calculateDimensions(shape, size)

            val p = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dpToPx(16)
                y = dpToPx(100)
            }
            this.params = p

            // Contenedor principal con sombra y gestos
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            }
            this.rootContainer = container

            // Contenedor con borde y recorte de forma
            val card = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            this.cameraCard = card

            // Vista previa de CameraX
            val pv = PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            this.previewView = pv
            card.addView(pv)

            // Borde decorativo translúcido
            val borderView = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            applyShapeOutline(card, borderView, shape)
            card.addView(borderView)

            container.addView(card)

            // Barra de controles rápidos al tocar
            val controls = createOverlayControls(borderView)
            this.overlayControls = controls
            container.addView(controls)

            // Configurar movimiento táctil de la ventana
            setupTouchDragging(container, p)

            windowManager.addView(container, p)
            isShowingInternal = true

            // Iniciar ciclo de vida y CameraX
            lifecycleOwner.start()
            startCameraPreview()

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando Facecam en WindowManager: ${e.message}", e)
            dismiss()
        }
    }

    private fun setupTouchDragging(container: View, p: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (initialTouchX - event.rawX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isClick = false
                    }

                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    if (isShowingInternal) {
                        try {
                            windowManager?.updateViewLayout(container, p)
                        } catch (e: Exception) {
                            // Ignore layout updates if detached
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleControlsVisibility()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun createOverlayControls(borderView: View): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xCC111827.toInt())
                cornerRadius = dpToPx(14).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(6)
            }

            // 1. Alternar Cámara Frontal / Trasera
            val btnFlip = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_rotate)
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                    marginEnd = dpToPx(6)
                }
                setColorFilter(Color.WHITE)
                setOnClickListener {
                    flipCamera()
                }
            }
            addView(btnFlip)

            // 2. Cambiar Forma de Facecam
            val btnShape = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_crop)
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                    marginEnd = dpToPx(6)
                }
                setColorFilter(0xFF38BDF8.toInt())
                setOnClickListener {
                    cycleShape(borderView)
                }
            }
            addView(btnShape)

            // 3. Cerrar Facecam
            val btnClose = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
                setColorFilter(0xFFEF4444.toInt())
                setOnClickListener {
                    dismiss()
                    onCloseClicked?.invoke()
                }
            }
            addView(btnClose)
        }
    }

    private fun toggleControlsVisibility() {
        overlayControls?.let { ctrls ->
            ctrls.visibility = if (ctrls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun startCameraPreview() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al inicializar CameraProvider: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val pv = previewView ?: return

        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }

            val primarySelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()

            // Intentar enlazar con la cámara seleccionada, o hacer fallback pacífico si no existe
            val hasSelectedCamera = try {
                provider.hasCamera(primarySelector)
            } catch (e: Exception) {
                false
            }

            val finalSelector = if (hasSelectedCamera) {
                primarySelector
            } else {
                val fallbackSelector = if (isFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                val hasFallback = try { provider.hasCamera(fallbackSelector) } catch (e: Exception) { false }
                if (hasFallback) fallbackSelector else primarySelector
            }

            provider.bindToLifecycle(lifecycleOwner, finalSelector, preview)
        } catch (e: Exception) {
            Log.e(TAG, "Error vinculando cámara en Facecam: ${e.message}", e)
        }
    }

    fun flipCamera() {
        isFrontCamera = !isFrontCamera
        bindCameraUseCases()
        onCameraFlipped?.invoke(isFrontCamera)
    }

    fun setShape(newShape: FacecamShape) {
        if (this.shape == newShape) return
        this.shape = newShape
        rootContainer?.post {
            updateDimensionsAndOutline()
        }
        onShapeChanged?.invoke(newShape)
    }

    fun setSize(newSize: FacecamSize) {
        if (this.size == newSize) return
        this.size = newSize
        rootContainer?.post {
            updateDimensionsAndOutline()
        }
    }

    private fun cycleShape(borderView: View) {
        val shapes = FacecamShape.values()
        val nextIndex = (shape.ordinal + 1) % shapes.size
        val nextShape = shapes[nextIndex]
        this.shape = nextShape
        rootContainer?.post {
            updateDimensionsAndOutline()
        }
        onShapeChanged?.invoke(nextShape)
    }

    private fun updateDimensionsAndOutline() {
        val container = rootContainer ?: return
        val card = cameraCard ?: return
        val p = params ?: return

        try {
            val (widthPx, heightPx) = calculateDimensions(shape, size)
            p.width = widthPx
            p.height = heightPx

            container.layoutParams = container.layoutParams?.apply {
                width = widthPx
                height = heightPx
            } ?: ViewGroup.LayoutParams(widthPx, heightPx)

            card.layoutParams = card.layoutParams?.apply {
                width = widthPx
                height = heightPx
            } ?: FrameLayout.LayoutParams(widthPx, heightPx)

            if (card.childCount > 1) {
                val borderView = card.getChildAt(1)
                applyShapeOutline(card, borderView, shape)
            }
            card.invalidateOutline()

            if (isShowingInternal) {
                windowManager?.updateViewLayout(container, p)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error actualizando layout tras cambio de forma: ${e.message}", e)
        }
    }

    private fun applyShapeOutline(card: FrameLayout, borderView: View, currentShape: FacecamShape) {
        try {
            when (currentShape) {
                FacecamShape.CIRCLE -> {
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            val side = minOf(view.width, view.height)
                            if (side <= 0) return
                            val left = (view.width - side) / 2
                            val top = (view.height - side) / 2
                            try {
                                outline.setOval(left, top, left + side, top + side)
                            } catch (e: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                    borderView.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dpToPx(3), 0xFF0284C7.toInt())
                    }
                }
                FacecamShape.ROUNDED_SQUARE -> {
                    val radius = dpToPx(24).toFloat()
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            val safeRadius = minOf(radius, (minOf(view.width, view.height) / 2).toFloat())
                            try {
                                outline.setRoundRect(0, 0, view.width, view.height, safeRadius)
                            } catch (e: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                    borderView.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setStroke(dpToPx(3), 0xFF0284C7.toInt())
                    }
                }
                FacecamShape.SQUARE -> {
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            try {
                                outline.setRect(0, 0, view.width, view.height)
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    }
                    card.clipToOutline = true
                    borderView.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setStroke(dpToPx(3), 0xFF0284C7.toInt())
                    }
                }
                FacecamShape.RECTANGLE -> {
                    val radius = dpToPx(16).toFloat()
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            val safeRadius = minOf(radius, (minOf(view.width, view.height) / 2).toFloat())
                            try {
                                outline.setRoundRect(0, 0, view.width, view.height, safeRadius)
                            } catch (e: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                    borderView.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setStroke(dpToPx(3), 0xFF0284C7.toInt())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error aplicando outline en Facecam: ${e.message}", e)
        }
    }

    private fun calculateDimensions(currentShape: FacecamShape, currentSize: FacecamSize): Pair<Int, Int> {
        val basePx = maxOf(dpToPx(currentSize.dpSize), 60)
        return when (currentShape) {
            FacecamShape.CIRCLE,
            FacecamShape.SQUARE,
            FacecamShape.ROUNDED_SQUARE -> Pair(basePx, basePx)
            FacecamShape.RECTANGLE -> Pair((basePx * 1.5f).toInt(), basePx)
        }
    }

    fun dismiss() {
        if (!isShowingInternal) return
        try {
            lifecycleOwner.stop()
            cameraProvider?.unbindAll()
            cameraProvider = null

            rootContainer?.let { container ->
                windowManager?.removeView(container)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al remover vista Facecam: ${e.message}")
        } finally {
            rootContainer = null
            cameraCard = null
            previewView = null
            overlayControls = null
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
}
