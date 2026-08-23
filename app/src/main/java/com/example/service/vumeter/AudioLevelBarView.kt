package com.example.service.vumeter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Vista de barra de Vúmetro estilo consola de estudio con aceleración por hardware.
 * Dibuja un gradiente de 3 tramos: Verde (normal), Amarillo (precaución) y Rojo (saturación/clipping),
 * con marcador de pico (Peak Hold) y fondo oscuro tipo medidor de radiodifusión.
 */
class AudioLevelBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentLevel: Float = 0.0f
    private var peakLevel: Float = 0.0f
    private var peakHoldTimer: Int = 0

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1F2937.toInt() // Slate 800
        style = Paint.Style.FILL
    }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x669CA3AF.toInt()
        strokeWidth = 1.5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9CA3AF.toInt()
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    private val rect = RectF()

    fun setLevel(level: Float) {
        val clamped = level.coerceIn(0.0f, 1.0f)
        this.currentLevel = clamped

        if (clamped >= peakLevel) {
            peakLevel = clamped
            peakHoldTimer = 25 // Mantener pico durante ~25 frames
        } else {
            if (peakHoldTimer > 0) {
                peakHoldTimer--
            } else {
                peakLevel = (peakLevel * 0.92f).coerceAtLeast(clamped)
            }
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = 6f

        // Fondo del vúmetro
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        // Número de segmentos estilo barra LED
        val segmentCount = 28
        val gap = 2f
        val segmentWidth = (w - (segmentCount - 1) * gap) / segmentCount

        val activeCount = (currentLevel * segmentCount).toInt().coerceIn(0, segmentCount)

        for (i in 0 until segmentCount) {
            val left = i * (segmentWidth + gap)
            val right = left + segmentWidth
            val progress = i.toFloat() / segmentCount

            val color = when {
                progress > 0.85f -> 0xFFEF4444.toInt() // Rojo (Clip / -3 dB a 0 dB)
                progress > 0.65f -> 0xFFF59E0B.toInt() // Amarillo (-12 dB a -3 dB)
                else -> 0xFF10B981.toInt()             // Verde (-inf a -12 dB)
            }

            if (i < activeCount) {
                segmentPaint.color = color
                segmentPaint.alpha = 255
            } else {
                segmentPaint.color = color
                segmentPaint.alpha = 40 // LED apagado translúcido
            }

            rect.set(left, 0f, right, h)
            canvas.drawRoundRect(rect, 2f, 2f, segmentPaint)
        }

        // Dibujar barra de Peak Hold
        if (peakLevel > 0.02f) {
            val peakX = (peakLevel * w).coerceIn(2f, w - 2f)
            canvas.drawLine(peakX, 0f, peakX, h, peakPaint)
        }
    }
}
