package com.example.service.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.TypedValue

/**
 * Generador de drawables vectoriales y marcos de escena gamer acelerados por hardware.
 */
object SceneOverlayDrawables {

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    /**
     * Marco Gamer Neon con biseles en las 4 esquinas y acentos superiores/inferiores.
     */
    class GamerNeonFrameDrawable(context: Context, val primaryColor: Int = 0xFF0284C7.toInt()) : Drawable() {
        private val strokeWidth = dpToPx(context, 3f)
        private val cornerSize = dpToPx(context, 28f)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            this.strokeWidth = this@GamerNeonFrameDrawable.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            alpha = 60
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth * 2.5f
            strokeCap = Paint.Cap.ROUND
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val offset = strokeWidth

            val path = Path().apply {
                // Esquina Superior Izquierda
                moveTo(offset, offset + cornerSize)
                lineTo(offset, offset + 8)
                lineTo(offset + 8, offset)
                lineTo(offset + cornerSize, offset)

                // Esquina Superior Derecha
                moveTo(w - offset - cornerSize, offset)
                lineTo(w - offset - 8, offset)
                lineTo(w - offset, offset + 8)
                lineTo(w - offset, offset + cornerSize)

                // Esquina Inferior Derecha
                moveTo(w - offset, h - offset - cornerSize)
                lineTo(w - offset, h - offset - 8)
                lineTo(w - offset - 8, h - offset)
                lineTo(w - offset - cornerSize, h - offset)

                // Esquina Inferior Izquierda
                moveTo(offset + cornerSize, h - offset)
                lineTo(offset + 8, h - offset)
                lineTo(offset, h - offset - 8)
                lineTo(offset, h - offset - cornerSize)
            }

            canvas.drawPath(path, glowPaint)
            canvas.drawPath(path, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
