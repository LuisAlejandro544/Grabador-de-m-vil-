package com.example.service.bubble

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Tarjeta de herramientas estilo Gaming Glassmorphism (Floating Grid Dock).
 * Agrupa las herramientas en una cuadrícula compacta y táctil (Touch-friendly),
 * evitando ocupar todo el ancho de la pantalla y manteniendo la visibilidad del juego.
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

    // Items individuales
    private val btnFacecam: LinearLayout
    private val iconFacecam: ImageView
    private val tvFacecam: TextView
    private val dotFacecam: View

    private val btnVtuber: LinearLayout
    private val iconVtuber: ImageView
    private val tvVtuber: TextView
    private val dotVtuber: View

    private val btnVuMeter: LinearLayout
    private val iconVuMeter: ImageView
    private val tvVuMeter: TextView
    private val dotVuMeter: View

    private val btnBeauty: LinearLayout
    private val iconBeauty: ImageView
    private val tvBeauty: TextView
    private val dotBeauty: View

    private val btnRgb: LinearLayout
    private val iconRgb: ImageView
    private val tvRgb: TextView
    private val dotRgb: View

    private val btnTouch: LinearLayout
    private val iconTouch: ImageView
    private val tvTouch: TextView
    private val dotTouch: View

    private val btnWatermark: LinearLayout
    private val iconWatermark: ImageView
    private val tvWatermark: TextView
    private val dotWatermark: View

    private val btnOverlay: LinearLayout
    private val iconOverlay: ImageView
    private val tvOverlay: TextView
    private val dotOverlay: View

    init {
        layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = BubbleDrawables.createCardDrawable(
                context = context,
                color = BubbleColors.COLOR_GLASS_PANEL,
                cornerRadiusPx = dp(18),
                strokeColor = BubbleColors.COLOR_GLASS_BORDER,
                strokeWidthDp = 1
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        // Título de la tarjeta HUD
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val headerDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply {
                marginEnd = dp(6)
            }
            background = BubbleDrawables.createCircleDrawable(0xFF818CF8.toInt())
        }
        headerLayout.addView(headerDot)

        val headerTitle = TextView(context).apply {
            text = "HERRAMIENTAS EN VIVO"
            setTextColor(0xFF818CF8.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
        headerLayout.addView(headerTitle)
        layout.addView(headerLayout)

        // Cuadrícula 5 columnas x 2 filas (10 herramientas)
        val gridLayout = GridLayout(context).apply {
            columnCount = 5
            rowCount = 2
            orientation = GridLayout.HORIZONTAL
        }

        // 1. Captura (Acción inmediata 1-Tap)
        val (btnShot, _, _, _) = createGridTile(
            iconRes = android.R.drawable.ic_menu_camera,
            label = "Captura",
            onClick = {
                onVibrateRequested()
                onScreenshotClicked()
            }
        )
        gridLayout.addView(btnShot)

        // 2. Pincel / Anotación (Acción inmediata)
        val (btnDraw, _, _, _) = createGridTile(
            iconRes = android.R.drawable.ic_menu_edit,
            label = "Pincel",
            onClick = {
                onVibrateRequested()
                onDrawToolClicked()
            }
        )
        gridLayout.addView(btnDraw)

        // 3. Vúmetro / Audio Mixer Pro
        val vuTile = createGridTile(
            iconRes = android.R.drawable.ic_lock_silent_mode_off,
            label = "Vúmetro",
            onClick = {
                onVibrateRequested()
                onVuMeterToggleClicked()
            }
        )
        btnVuMeter = vuTile.container
        iconVuMeter = vuTile.icon
        tvVuMeter = vuTile.label
        dotVuMeter = vuTile.statusDot
        gridLayout.addView(btnVuMeter)

        // 4. Facecam / Cámara flotante
        val faceTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_camera,
            label = "Facecam",
            onClick = {
                onVibrateRequested()
                onFacecamToggleClicked()
            }
        )
        btnFacecam = faceTile.container
        iconFacecam = faceTile.icon
        tvFacecam = faceTile.label
        dotFacecam = faceTile.statusDot
        gridLayout.addView(btnFacecam)

        // 5. Avatar 2D / PNGtuber
        val vtuberTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_myplaces,
            label = "Avatar 2D",
            onClick = {
                onVibrateRequested()
                onVtuberToggleClicked()
            }
        )
        btnVtuber = vtuberTile.container
        iconVtuber = vtuberTile.icon
        tvVtuber = vtuberTile.label
        dotVtuber = vtuberTile.statusDot
        gridLayout.addView(btnVtuber)

        // 6. Belleza (Filtro Suavizado)
        val beautyTile = createGridTile(
            iconRes = android.R.drawable.btn_star_big_on,
            label = "Belleza",
            onClick = {
                onVibrateRequested()
                onBeautyToggleClicked()
            }
        )
        btnBeauty = beautyTile.container
        iconBeauty = beautyTile.icon
        tvBeauty = beautyTile.label
        dotBeauty = beautyTile.statusDot
        gridLayout.addView(btnBeauty)

        // 7. RGB Gamer Border
        val rgbTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_compass,
            label = "RGB",
            onClick = {
                onVibrateRequested()
                onRgbBorderToggleClicked()
            }
        )
        btnRgb = rgbTile.container
        iconRgb = rgbTile.icon
        tvRgb = rgbTile.label
        dotRgb = rgbTile.statusDot
        gridLayout.addView(btnRgb)

        // 8. Toques Táctiles en pantalla
        val touchTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_directions,
            label = "Toques",
            onClick = {
                onVibrateRequested()
                onTouchToggleClicked()
            }
        )
        btnTouch = touchTile.container
        iconTouch = touchTile.icon
        tvTouch = touchTile.label
        dotTouch = touchTile.statusDot
        gridLayout.addView(btnTouch)

        // 9. Logo / Marca de agua
        val watermarkTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_gallery,
            label = "Logo",
            onClick = {
                onVibrateRequested()
                onWatermarkToggleClicked()
            }
        )
        btnWatermark = watermarkTile.container
        iconWatermark = watermarkTile.icon
        tvWatermark = watermarkTile.label
        dotWatermark = watermarkTile.statusDot
        gridLayout.addView(btnWatermark)

        // 10. Marco / Overlay de Escena
        val overlayTile = createGridTile(
            iconRes = android.R.drawable.ic_menu_crop,
            label = "Marco",
            onClick = {
                onVibrateRequested()
                onSceneOverlayToggleClicked()
            }
        )
        btnOverlay = overlayTile.container
        iconOverlay = overlayTile.icon
        tvOverlay = overlayTile.label
        dotOverlay = overlayTile.statusDot
        gridLayout.addView(btnOverlay)

        layout.addView(gridLayout)
    }

    private data class GridTile(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView,
        val statusDot: View
    )

    private fun createGridTile(
        iconRes: Int,
        label: String,
        onClick: () -> Unit
    ): GridTile {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = BubbleDrawables.createCardDrawable(
                context = context,
                color = BubbleColors.COLOR_BTN_BG,
                cornerRadiusPx = dp(12),
                strokeColor = 0x22FFFFFF.toInt(),
                strokeWidthDp = 1
            )
            setPadding(dp(4), dp(8), dp(4), dp(6))
            val params = GridLayout.LayoutParams().apply {
                width = dp(56)
                height = dp(54)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }

        // Icono + Status Dot Container
        val iconWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val iv = ImageView(context).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
            setColorFilter(Color.WHITE)
        }
        iconWrapper.addView(iv)

        val statusDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(5), dp(5)).apply {
                marginStart = dp(2)
                topMargin = dp(-6)
            }
            background = BubbleDrawables.createCircleDrawable(0x00000000) // Invisible por defecto
        }
        iconWrapper.addView(statusDot)

        container.addView(iconWrapper)

        val tv = TextView(context).apply {
            text = label
            setTextColor(BubbleColors.COLOR_TEXT_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        }
        container.addView(tv)

        return GridTile(container, iv, tv, statusDot)
    }

    fun updateFacecamStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnFacecam,
            icon = iconFacecam,
            label = tvFacecam,
            dot = dotFacecam,
            activeBgColor = BubbleColors.COLOR_FACECAM_ON_BG,
            activeTextColor = BubbleColors.COLOR_FACECAM_ON_TEXT,
            activeLabel = "Facecam"
        )
    }

    fun updateVtuberStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnVtuber,
            icon = iconVtuber,
            label = tvVtuber,
            dot = dotVtuber,
            activeBgColor = BubbleColors.COLOR_TOUCH_ON_BG,
            activeTextColor = BubbleColors.COLOR_TOUCH_ON_TEXT,
            activeLabel = "Avatar 2D"
        )
    }

    fun updateVuMeterStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnVuMeter,
            icon = iconVuMeter,
            label = tvVuMeter,
            dot = dotVuMeter,
            activeBgColor = BubbleColors.COLOR_FACECAM_ON_BG,
            activeTextColor = BubbleColors.COLOR_FACECAM_ON_TEXT,
            activeLabel = "Vúmetro"
        )
    }

    fun updateBeautyStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnBeauty,
            icon = iconBeauty,
            label = tvBeauty,
            dot = dotBeauty,
            activeBgColor = BubbleColors.COLOR_BEAUTY_ON_BG,
            activeTextColor = BubbleColors.COLOR_BEAUTY_ON_TEXT,
            activeLabel = "Belleza"
        )
    }

    fun updateRgbStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnRgb,
            icon = iconRgb,
            label = tvRgb,
            dot = dotRgb,
            activeBgColor = BubbleColors.COLOR_RGB_ON_BG,
            activeTextColor = BubbleColors.COLOR_RGB_ON_TEXT,
            activeLabel = "RGB"
        )
    }

    fun updateTouchStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnTouch,
            icon = iconTouch,
            label = tvTouch,
            dot = dotTouch,
            activeBgColor = BubbleColors.COLOR_TOUCH_ON_BG,
            activeTextColor = BubbleColors.COLOR_TOUCH_ON_TEXT,
            activeLabel = "Toques"
        )
    }

    fun updateWatermarkStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnWatermark,
            icon = iconWatermark,
            label = tvWatermark,
            dot = dotWatermark,
            activeBgColor = BubbleColors.COLOR_WATERMARK_ON_BG,
            activeTextColor = BubbleColors.COLOR_WATERMARK_ON_TEXT,
            activeLabel = "Logo"
        )
    }

    fun updateSceneOverlayStatus(active: Boolean) {
        updateToggleAppearance(
            active = active,
            container = btnOverlay,
            icon = iconOverlay,
            label = tvOverlay,
            dot = dotOverlay,
            activeBgColor = BubbleColors.COLOR_OVERLAY_ON_BG,
            activeTextColor = BubbleColors.COLOR_OVERLAY_ON_TEXT,
            activeLabel = "Marco"
        )
    }

    private fun updateToggleAppearance(
        active: Boolean,
        container: LinearLayout,
        icon: ImageView,
        label: TextView,
        dot: View,
        activeBgColor: Int,
        activeTextColor: Int,
        activeLabel: String
    ) {
        if (active) {
            container.background = BubbleDrawables.createCardDrawable(
                context = context,
                color = activeBgColor,
                cornerRadiusPx = dp(12),
                strokeColor = activeTextColor,
                strokeWidthDp = 1
            )
            icon.setColorFilter(activeTextColor)
            label.setTextColor(activeTextColor)
            dot.background = BubbleDrawables.createCircleDrawable(activeTextColor)
        } else {
            container.background = BubbleDrawables.createCardDrawable(
                context = context,
                color = BubbleColors.COLOR_BTN_BG,
                cornerRadiusPx = dp(12),
                strokeColor = 0x22FFFFFF.toInt(),
                strokeWidthDp = 1
            )
            icon.setColorFilter(Color.WHITE)
            label.setTextColor(BubbleColors.COLOR_TEXT_WHITE)
            dot.background = BubbleDrawables.createCircleDrawable(0x00000000)
        }
    }

    private fun dp(dpValue: Int): Int = BubbleDrawables.dpToPx(context, dpValue)
}
