package com.example.editor

import com.example.nativecore.NativeRustNetwork

/**
 * Opciones de Relación de Aspecto (1-Tap Aspect Ratio Converter).
 */
enum class AspectRatioOption(
    val label: String,
    val ratioW: Int,
    val ratioH: Int,
    val description: String,
    val rustCode: Int
) {
    ORIGINAL("Original", 0, 0, "Aspecto Nativo", -1),
    TIKTOK_9_16("9:16", 9, 16, "TikTok / Shorts / Reels", 0),
    YOUTUBE_16_9("16:9", 16, 9, "YouTube / Pantalla Completa", 1),
    SQUARE_1_1("1:1", 1, 1, "Instagram / Post Cuadrado", 2),
    PORTRAIT_4_5("4:5", 4, 5, "Feed / Retrato Vertical", 3),
    CLASSIC_4_3("4:3", 4, 3, "Clásico / iPad / Tablet", 4);

    fun getTargetDimensions(origWidth: Int, origHeight: Int): Pair<Int, Int> {
        if (this == ORIGINAL || ratioW == 0 || ratioH == 0) return Pair(origWidth, origHeight)
        return NativeRustNetwork.calculateTargetDimensions(origWidth, origHeight, rustCode)
    }
}

/**
 * Modo de ajuste visual para la conversión de aspecto.
 */
enum class AspectRatioFitMode(val label: String, val description: String, val nativeMode: Int) {
    BLUR_BACKGROUND("Desenfoque Blur", "Fondo cinematográfico desenfocado con video centrado", 0),
    CROP_FILL("Llenar (Crop)", "Recorte central llenando todo el marco sin bordes", 1),
    LETTERBOX_BLACK("Barras Negras", "Ajuste tradicional con bandas negras", 2)
}
