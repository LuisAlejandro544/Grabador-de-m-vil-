package com.example.service.facecam

import android.content.Context
import android.graphics.Outline
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.example.model.FacecamShape
import com.example.model.FacecamSize

/**
 * Utilidades para el cálculo de dimensiones en píxeles y recorte geométrico de Facecam.
 */
object FacecamShapeHelper {

    private const val TAG = "FacecamShapeHelper"

    fun calculateDimensions(context: Context, shape: FacecamShape, size: FacecamSize): Pair<Int, Int> {
        val basePx = maxOf(dpToPx(context, size.dpSize.toFloat()).toInt(), 60)
        return when (shape) {
            FacecamShape.CIRCLE,
            FacecamShape.SQUARE,
            FacecamShape.ROUNDED_SQUARE -> Pair(basePx, basePx)
            FacecamShape.RECTANGLE -> Pair((basePx * 1.5f).toInt(), basePx)
        }
    }

    fun applyShapeOutline(context: Context, card: FrameLayout, currentShape: FacecamShape) {
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
                            } catch (_: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                }
                FacecamShape.ROUNDED_SQUARE -> {
                    val radius = dpToPx(context, 24f)
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            val safeRadius = minOf(radius, (minOf(view.width, view.height) / 2).toFloat())
                            try {
                                outline.setRoundRect(0, 0, view.width, view.height, safeRadius)
                            } catch (_: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                }
                FacecamShape.SQUARE -> {
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            try {
                                outline.setRect(0, 0, view.width, view.height)
                            } catch (_: Exception) {}
                        }
                    }
                    card.clipToOutline = true
                }
                FacecamShape.RECTANGLE -> {
                    val radius = dpToPx(context, 16f)
                    card.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            if (view.width <= 0 || view.height <= 0) return
                            val safeRadius = minOf(radius, (minOf(view.width, view.height) / 2).toFloat())
                            try {
                                outline.setRoundRect(0, 0, view.width, view.height, safeRadius)
                            } catch (_: Exception) {
                                outline.setRect(0, 0, view.width, view.height)
                            }
                        }
                    }
                    card.clipToOutline = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error aplicando outline en Facecam: ${e.message}", e)
        }
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
}
