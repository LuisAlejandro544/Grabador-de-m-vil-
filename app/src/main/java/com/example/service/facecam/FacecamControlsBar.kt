package com.example.service.facecam

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * Barra inferior flotante de control rápido superpuesta en Facecam:
 * Voltear lente frontal/trasero, cambiar forma geométrica, filtro de belleza, borde RGB y cerrar.
 */
class FacecamControlsBar(
    private val context: Context,
    private val onFlipClicked: () -> Unit,
    private val onShapeClicked: () -> Unit,
    private val onBeautyClicked: () -> Unit,
    private val onRgbClicked: () -> Unit,
    private val onCloseClicked: () -> Unit
) {

    val layout: LinearLayout
    private val btnBeauty: ImageView
    private val btnRgb: ImageView

    init {
        layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            val pad = dp(4)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xEE0F172A.toInt())
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(6)
            }

            // 1. Alternar Cámara Frontal / Trasera
            val btnFlip = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_rotate)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(4)
                }
                setColorFilter(Color.WHITE)
                setOnClickListener { onFlipClicked() }
            }
            addView(btnFlip)

            // 2. Cambiar Forma
            val btnShape = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_crop)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(4)
                }
                setColorFilter(0xFF38BDF8.toInt())
                setOnClickListener { onShapeClicked() }
            }
            addView(btnShape)

            // 3. Belleza
            btnBeauty = ImageView(context).apply {
                setImageResource(android.R.drawable.btn_star_big_on)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(4)
                }
                setColorFilter(0xFF94A3B8.toInt())
                setOnClickListener { onBeautyClicked() }
            }
            addView(btnBeauty)

            // 4. RGB
            btnRgb = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_compass)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(4)
                }
                setColorFilter(0xFF94A3B8.toInt())
                setOnClickListener { onRgbClicked() }
            }
            addView(btnRgb)

            // 5. Cerrar Facecam
            val btnClose = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                setColorFilter(0xFFEF4444.toInt())
                setOnClickListener { onCloseClicked() }
            }
            addView(btnClose)
        }
    }

    fun updateBeautyIcon(enabled: Boolean) {
        btnBeauty.setColorFilter(if (enabled) 0xFFF472B6.toInt() else 0xFF94A3B8.toInt())
    }

    fun updateRgbIcon(enabled: Boolean) {
        btnRgb.setColorFilter(if (enabled) 0xFF10B981.toInt() else 0xFF94A3B8.toInt())
    }

    fun toggleVisibility() {
        layout.visibility = if (layout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun dp(dpValue: Int): Int = FacecamShapeHelper.dpToPx(context, dpValue.toFloat()).toInt()
}
