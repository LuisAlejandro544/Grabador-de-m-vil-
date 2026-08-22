package com.example.service.facecam

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.model.FacecamShape

/**
 * Vista de borde dinámico con soporte de borde sólido clásico o borde RGB Arcoíris giratorio animado.
 */
class FacecamRgbBorderView(
    context: Context,
    private var shapeProvider: () -> FacecamShape,
    private var rgbEnabledProvider: () -> Boolean
) : View(context) {

    companion object {
        private val RAINBOW_COLORS = intArrayOf(
            0xFFFF0055.toInt(),
            0xFFFF7700.toInt(),
            0xFFFFEE00.toInt(),
            0xFF00FF66.toInt(),
            0xFF00D4FF.toInt(),
            0xFF8A2BE2.toInt(),
            0xFFFF00AA.toInt(),
            0xFFFF0055.toInt()
        )
    }

    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = FacecamShapeHelper.dpToPx(context, 3.5f)
    }
    private val matrix = Matrix()
    private val rectF = RectF()

    init {
        startOrStopAnimation()
    }

    fun updateRgbMode(enabled: Boolean) {
        startOrStopAnimation()
        invalidate()
    }

    private fun startOrStopAnimation() {
        animator?.cancel()
        animator = null

        if (rgbEnabledProvider()) {
            animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 2400
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    rotationAngle = anim.animatedValue as Float
                    postInvalidateOnAnimation()
                }
                start()
            }
        }
    }

    fun release() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strokeW = FacecamShapeHelper.dpToPx(context, 3.5f)
        val halfStroke = strokeW / 2f
        rectF.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)

        val isRgb = rgbEnabledProvider()
        if (isRgb) {
            val cx = width / 2f
            val cy = height / 2f
            val gradient = SweepGradient(cx, cy, RAINBOW_COLORS, null)
            matrix.setRotate(rotationAngle, cx, cy)
            gradient.setLocalMatrix(matrix)
            borderPaint.shader = gradient
        } else {
            borderPaint.shader = null
            borderPaint.color = 0xFF0284C7.toInt()
        }

        when (shapeProvider()) {
            FacecamShape.CIRCLE -> {
                val radius = (minOf(width, height) / 2f) - halfStroke
                canvas.drawCircle(width / 2f, height / 2f, maxOf(radius, 0f), borderPaint)
            }
            FacecamShape.ROUNDED_SQUARE -> {
                val cornerRadius = FacecamShapeHelper.dpToPx(context, 24f)
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
            }
            FacecamShape.SQUARE -> {
                canvas.drawRect(rectF, borderPaint)
            }
            FacecamShape.RECTANGLE -> {
                val cornerRadius = FacecamShapeHelper.dpToPx(context, 16f)
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
            }
        }
    }
}
