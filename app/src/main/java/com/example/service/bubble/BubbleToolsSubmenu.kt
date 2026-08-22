package com.example.service.bubble

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Submenú desplegable de herramientas para el widget flotante:
 * Captura de pantalla, Pincel/Anotación, Facecam, Filtro de Belleza, Borde RGB y Toques Táctiles.
 */
class BubbleToolsSubmenu(
    private val context: Context,
    private val onScreenshotClicked: () -> Unit,
    private val onDrawToolClicked: () -> Unit,
    private val onFacecamToggleClicked: () -> Unit,
    private val onBeautyToggleClicked: () -> Unit,
    private val onRgbBorderToggleClicked: () -> Unit,
    private val onTouchToggleClicked: () -> Unit,
    private val onWatermarkToggleClicked: () -> Unit = {},
    private val onSceneOverlayToggleClicked: () -> Unit = {},
    private val onVtuberToggleClicked: () -> Unit = {},
    private val onVuMeterToggleClicked: () -> Unit = {},
    private val onVibrateRequested: () -> Unit
) {

    val layout: LinearLayout
    val btnFacecamToggle: LinearLayout
    val iconFacecamToggle: ImageView
    val tvFacecamToggleLabel: TextView

    val btnVtuberToggle: LinearLayout
    val iconVtuberToggle: ImageView
    val tvVtuberToggleLabel: TextView

    val btnVuMeterToggle: LinearLayout
    val iconVuMeterToggle: ImageView
    val tvVuMeterToggleLabel: TextView

    val btnBeautyToggle: LinearLayout
    val iconBeautyToggle: ImageView
    val tvBeautyToggleLabel: TextView

    val btnRgbToggle: LinearLayout
    val iconRgbToggle: ImageView
    val tvRgbToggleLabel: TextView

    val btnTouchToggle: LinearLayout
    val iconTouchToggle: ImageView
    val tvTouchToggleLabel: TextView

    val btnWatermarkToggle: LinearLayout
    val iconWatermarkToggle: ImageView
    val tvWatermarkToggleLabel: TextView

    val btnOverlayToggle: LinearLayout
    val iconOverlayToggle: ImageView
    val tvOverlayToggleLabel: TextView

    init {
        layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = BubbleDrawables.createCardDrawable(context, 0xEE1E232A.toInt(), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
        }

        // 1. Captura de pantalla
        val btnScreenshot = createSubmenuItem(
            iconRes = android.R.drawable.ic_menu_camera,
            label = "Captura",
            onClick = {
                onVibrateRequested()
                onScreenshotClicked()
            }
        )
        layout.addView(btnScreenshot)

        // 2. Pincel / Anotación en pantalla
        val btnDraw = createSubmenuItem(
            iconRes = android.R.drawable.ic_menu_edit,
            label = "Pincel",
            onClick = {
                onVibrateRequested()
                onDrawToolClicked()
            }
        )
        layout.addView(btnDraw)

        // 3. Vúmetro / Mezclador de Audio Flotante OBS
        btnVuMeterToggle = createBaseContainer()
        iconVuMeterToggle = createIcon(android.R.drawable.ic_lock_silent_mode_off, Color.WHITE)
        tvVuMeterToggleLabel = createLabel("Vúmetro", Color.WHITE)
        btnVuMeterToggle.addView(iconVuMeterToggle)
        btnVuMeterToggle.addView(tvVuMeterToggleLabel)
        btnVuMeterToggle.setOnClickListener {
            onVibrateRequested()
            onVuMeterToggleClicked()
        }
        layout.addView(btnVuMeterToggle)

        // 4. Facecam
        btnFacecamToggle = createBaseContainer()
        iconFacecamToggle = createIcon(android.R.drawable.ic_menu_camera, Color.WHITE)
        tvFacecamToggleLabel = createLabel("Facecam", Color.WHITE)
        btnFacecamToggle.addView(iconFacecamToggle)
        btnFacecamToggle.addView(tvFacecamToggleLabel)
        btnFacecamToggle.setOnClickListener {
            onVibrateRequested()
            onFacecamToggleClicked()
        }
        layout.addView(btnFacecamToggle)

        // 5. PNGtuber / Avatar 2D
        btnVtuberToggle = createBaseContainer()
        iconVtuberToggle = createIcon(android.R.drawable.ic_menu_myplaces, Color.WHITE)
        tvVtuberToggleLabel = createLabel("Avatar 2D", Color.WHITE)
        btnVtuberToggle.addView(iconVtuberToggle)
        btnVtuberToggle.addView(tvVtuberToggleLabel)
        btnVtuberToggle.setOnClickListener {
            onVibrateRequested()
            onVtuberToggleClicked()
        }
        layout.addView(btnVtuberToggle)

        // 5. Belleza
        btnBeautyToggle = createBaseContainer()
        iconBeautyToggle = createIcon(android.R.drawable.btn_star_big_on, Color.WHITE)
        tvBeautyToggleLabel = createLabel("Belleza", Color.WHITE)
        btnBeautyToggle.addView(iconBeautyToggle)
        btnBeautyToggle.addView(tvBeautyToggleLabel)
        btnBeautyToggle.setOnClickListener {
            onVibrateRequested()
            onBeautyToggleClicked()
        }
        layout.addView(btnBeautyToggle)

        // 5. RGB
        btnRgbToggle = createBaseContainer()
        iconRgbToggle = createIcon(android.R.drawable.ic_menu_compass, Color.WHITE)
        tvRgbToggleLabel = createLabel("RGB", Color.WHITE)
        btnRgbToggle.addView(iconRgbToggle)
        btnRgbToggle.addView(tvRgbToggleLabel)
        btnRgbToggle.setOnClickListener {
            onVibrateRequested()
            onRgbBorderToggleClicked()
        }
        layout.addView(btnRgbToggle)

        // 6. Toques Táctiles
        btnTouchToggle = createBaseContainer()
        iconTouchToggle = createIcon(android.R.drawable.ic_menu_directions, Color.WHITE)
        tvTouchToggleLabel = createLabel("Toques", Color.WHITE)
        btnTouchToggle.addView(iconTouchToggle)
        btnTouchToggle.addView(tvTouchToggleLabel)
        btnTouchToggle.setOnClickListener {
            onVibrateRequested()
            onTouchToggleClicked()
        }
        layout.addView(btnTouchToggle)

        // 7. Marca de Agua / Logo
        btnWatermarkToggle = createBaseContainer()
        iconWatermarkToggle = createIcon(android.R.drawable.ic_menu_gallery, Color.WHITE)
        tvWatermarkToggleLabel = createLabel("Logo", Color.WHITE)
        btnWatermarkToggle.addView(iconWatermarkToggle)
        btnWatermarkToggle.addView(tvWatermarkToggleLabel)
        btnWatermarkToggle.setOnClickListener {
            onVibrateRequested()
            onWatermarkToggleClicked()
        }
        layout.addView(btnWatermarkToggle)

        // 8. Overlays / Marcos de Escena
        btnOverlayToggle = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        iconOverlayToggle = createIcon(android.R.drawable.ic_menu_crop, Color.WHITE)
        tvOverlayToggleLabel = createLabel("Marco", Color.WHITE)
        btnOverlayToggle.addView(iconOverlayToggle)
        btnOverlayToggle.addView(tvOverlayToggleLabel)
        btnOverlayToggle.setOnClickListener {
            onVibrateRequested()
            onSceneOverlayToggleClicked()
        }
        layout.addView(btnOverlayToggle)
    }

    fun updateFacecamStatus(active: Boolean) {
        if (active) {
            btnFacecamToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_FACECAM_ON_BG, dp(12))
            iconFacecamToggle.setColorFilter(BubbleColors.COLOR_FACECAM_ON_TEXT)
            tvFacecamToggleLabel.text = "Facecam ON"
            tvFacecamToggleLabel.setTextColor(BubbleColors.COLOR_FACECAM_ON_TEXT)
        } else {
            btnFacecamToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconFacecamToggle.setColorFilter(Color.WHITE)
            tvFacecamToggleLabel.text = "Facecam"
            tvFacecamToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateVtuberStatus(active: Boolean) {
        if (active) {
            btnVtuberToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_TOUCH_ON_BG, dp(12))
            iconVtuberToggle.setColorFilter(BubbleColors.COLOR_TOUCH_ON_TEXT)
            tvVtuberToggleLabel.text = "Avatar ON"
            tvVtuberToggleLabel.setTextColor(BubbleColors.COLOR_TOUCH_ON_TEXT)
        } else {
            btnVtuberToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconVtuberToggle.setColorFilter(Color.WHITE)
            tvVtuberToggleLabel.text = "Avatar 2D"
            tvVtuberToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateVuMeterStatus(active: Boolean) {
        if (active) {
            btnVuMeterToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_FACECAM_ON_BG, dp(12))
            iconVuMeterToggle.setColorFilter(BubbleColors.COLOR_FACECAM_ON_TEXT)
            tvVuMeterToggleLabel.text = "Vúmetro ON"
            tvVuMeterToggleLabel.setTextColor(BubbleColors.COLOR_FACECAM_ON_TEXT)
        } else {
            btnVuMeterToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconVuMeterToggle.setColorFilter(Color.WHITE)
            tvVuMeterToggleLabel.text = "Vúmetro"
            tvVuMeterToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateBeautyStatus(active: Boolean) {
        if (active) {
            btnBeautyToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BEAUTY_ON_BG, dp(12))
            iconBeautyToggle.setColorFilter(BubbleColors.COLOR_BEAUTY_ON_TEXT)
            tvBeautyToggleLabel.text = "Belleza ON"
            tvBeautyToggleLabel.setTextColor(BubbleColors.COLOR_BEAUTY_ON_TEXT)
        } else {
            btnBeautyToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconBeautyToggle.setColorFilter(Color.WHITE)
            tvBeautyToggleLabel.text = "Belleza"
            tvBeautyToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateRgbStatus(active: Boolean) {
        if (active) {
            btnRgbToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_RGB_ON_BG, dp(12))
            iconRgbToggle.setColorFilter(BubbleColors.COLOR_RGB_ON_TEXT)
            tvRgbToggleLabel.text = "RGB ON"
            tvRgbToggleLabel.setTextColor(BubbleColors.COLOR_RGB_ON_TEXT)
        } else {
            btnRgbToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconRgbToggle.setColorFilter(Color.WHITE)
            tvRgbToggleLabel.text = "RGB"
            tvRgbToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateTouchStatus(active: Boolean) {
        if (active) {
            btnTouchToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_TOUCH_ON_BG, dp(12))
            iconTouchToggle.setColorFilter(BubbleColors.COLOR_TOUCH_ON_TEXT)
            tvTouchToggleLabel.text = "Toques ON"
            tvTouchToggleLabel.setTextColor(BubbleColors.COLOR_TOUCH_ON_TEXT)
        } else {
            btnTouchToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconTouchToggle.setColorFilter(Color.WHITE)
            tvTouchToggleLabel.text = "Toques"
            tvTouchToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateWatermarkStatus(active: Boolean) {
        if (active) {
            btnWatermarkToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_WATERMARK_ON_BG, dp(12))
            iconWatermarkToggle.setColorFilter(BubbleColors.COLOR_WATERMARK_ON_TEXT)
            tvWatermarkToggleLabel.text = "Logo ON"
            tvWatermarkToggleLabel.setTextColor(BubbleColors.COLOR_WATERMARK_ON_TEXT)
        } else {
            btnWatermarkToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconWatermarkToggle.setColorFilter(Color.WHITE)
            tvWatermarkToggleLabel.text = "Logo"
            tvWatermarkToggleLabel.setTextColor(Color.WHITE)
        }
    }

    fun updateSceneOverlayStatus(active: Boolean) {
        if (active) {
            btnOverlayToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_OVERLAY_ON_BG, dp(12))
            iconOverlayToggle.setColorFilter(BubbleColors.COLOR_OVERLAY_ON_TEXT)
            tvOverlayToggleLabel.text = "Marco ON"
            tvOverlayToggleLabel.setTextColor(BubbleColors.COLOR_OVERLAY_ON_TEXT)
        } else {
            btnOverlayToggle.background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            iconOverlayToggle.setColorFilter(Color.WHITE)
            tvOverlayToggleLabel.text = "Marco"
            tvOverlayToggleLabel.setTextColor(Color.WHITE)
        }
    }

    private fun createBaseContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = BubbleDrawables.createCardDrawable(context, BubbleColors.COLOR_BTN_BG, dp(12))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
            }
        }
    }

    private fun createIcon(resId: Int, color: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(resId)
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                marginEnd = dp(4)
            }
            setColorFilter(color)
        }
    }

    private fun createLabel(text: String, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun createSubmenuItem(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout {
        val container = createBaseContainer()
        val icon = createIcon(iconRes, Color.WHITE)
        val tv = createLabel(label, Color.WHITE)
        container.addView(icon)
        container.addView(tv)
        container.setOnClickListener { onClick() }
        return container
    }

    private fun dp(dpValue: Int): Int = BubbleDrawables.dpToPx(context, dpValue)
}
