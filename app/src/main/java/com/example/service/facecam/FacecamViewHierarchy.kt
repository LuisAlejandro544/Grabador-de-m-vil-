package com.example.service.facecam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import com.example.model.FacecamShape
import com.example.model.FacecamSize

/**
 * Encargado de construir la jerarquía de vistas de la Facecam flotante:
 * contenedor raíz con padding de brillo, tarjeta con recorte de forma,
 * vista previa de CameraX, overlay de belleza, borde animado RGB y barra de controles.
 */
class FacecamViewHierarchy(
    private val context: Context,
    private val shapeProvider: () -> FacecamShape,
    private val rgbEnabledProvider: () -> Boolean,
    private val isShowingProvider: () -> Boolean,
    private val onFlipClicked: () -> Unit,
    private val onShapeClicked: () -> Unit,
    private val onBeautyClicked: () -> Unit,
    private val onRgbClicked: () -> Unit,
    private val onCloseClicked: () -> Unit
) {
    var rootContainer: FrameLayout? = null
        private set
    var cardContainer: FrameLayout? = null
        private set
    var previewView: PreviewView? = null
        private set
    var beautyOverlay: View? = null
        private set
    var rgbBorderView: FacecamRgbBorderView? = null
        private set
    var controlsBar: FacecamControlsBar? = null
        private set

    @SuppressLint("ClickableViewAccessibility")
    fun buildHierarchy(
        shape: FacecamShape,
        size: FacecamSize,
        beautyFilterEnabled: Boolean,
        rgbBorderEnabled: Boolean,
        windowParams: WindowManager.LayoutParams,
        windowManager: WindowManager
    ): FrameLayout {
        val (widthPx, heightPx) = FacecamShapeHelper.calculateDimensions(context, shape, size)
        val paddingGlow = FacecamShapeHelper.dpToPx(context, 12f).toInt()
        val totalW = widthPx + (paddingGlow * 2)
        val totalH = heightPx + (paddingGlow * 2)

        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(totalW, totalH)
            setPadding(paddingGlow, paddingGlow, paddingGlow, paddingGlow)
        }
        this.rootContainer = root

        val card = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }
        this.cardContainer = card
        FacecamShapeHelper.applyShapeOutline(context, card, shape)

        val preview = PreviewView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        this.previewView = preview
        card.addView(preview)

        val beauty = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x28FFE4E1.toInt())
            visibility = if (beautyFilterEnabled) View.VISIBLE else View.GONE
        }
        this.beautyOverlay = beauty
        card.addView(beauty)

        val rgb = FacecamRgbBorderView(
            context = context,
            shapeProvider = shapeProvider,
            rgbEnabledProvider = rgbEnabledProvider
        ).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        this.rgbBorderView = rgb
        card.addView(rgb)

        val controls = FacecamControlsBar(
            context = context,
            onFlipClicked = onFlipClicked,
            onShapeClicked = onShapeClicked,
            onBeautyClicked = onBeautyClicked,
            onRgbClicked = onRgbClicked,
            onCloseClicked = onCloseClicked
        )
        this.controlsBar = controls
        controls.updateBeautyIcon(beautyFilterEnabled)
        controls.updateRgbIcon(rgbBorderEnabled)
        card.addView(controls.layout)

        root.addView(card)

        FacecamTouchDragHelper.attach(
            container = root,
            params = windowParams,
            windowManager = windowManager,
            isShowingProvider = isShowingProvider,
            onSingleTap = { controls.toggleVisibility() }
        )

        return root
    }

    fun applyShape(shape: FacecamShape) {
        val card = cardContainer ?: return
        FacecamShapeHelper.applyShapeOutline(context, card, shape)
        rgbBorderView?.invalidate()
    }

    fun updateBeautyFilter(enabled: Boolean) {
        beautyOverlay?.visibility = if (enabled) View.VISIBLE else View.GONE
        controlsBar?.updateBeautyIcon(enabled)
    }

    fun updateRgbBorder(enabled: Boolean) {
        rgbBorderView?.updateRgbMode(enabled)
        controlsBar?.updateRgbIcon(enabled)
    }

    fun release() {
        rgbBorderView?.release()
        rootContainer = null
        cardContainer = null
        previewView = null
        beautyOverlay = null
        rgbBorderView = null
        controlsBar = null
    }
}
