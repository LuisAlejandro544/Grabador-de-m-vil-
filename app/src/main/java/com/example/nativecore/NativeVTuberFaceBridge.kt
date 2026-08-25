package com.example.nativecore

import android.util.Log

/**
 * Resultado estructurado del análisis de seguimiento facial en tiempo real para el avatar VTuber.
 */
data class VtuberFaceTrackingData(
    val faceDetected: Boolean = false,
    val leftEyeOpenness: Float = 1.0f,
    val rightEyeOpenness: Float = 1.0f,
    val mouthOpenness: Float = 0.0f,
    val smileRatio: Float = 0.0f,
    val headPitch: Float = 0.0f,
    val headYaw: Float = 0.0f,
    val headRoll: Float = 0.0f,
    val confidence: Float = 0.0f,
    val processingTimeUs: Long = 0L
)

/**
 * Puente JNI con el motor nativo de C++ para seguimiento facial de alta velocidad (IA local / Face Mesh / Blendshapes).
 * Procesa fotogramas de cámara y calcula rotación de cabeza (Roll, Pitch, Yaw) y apertura de ojos/boca en <5ms.
 */
object NativeVTuberFaceBridge {
    private const val TAG = "NativeVTuberFaceBridge"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("obs_core")
            isLibraryLoaded = true
            Log.i(TAG, "Librería C++ 'obs_core' vinculada para seguimiento facial VTuber")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            Log.w(TAG, "Librería C++ 'obs_core' en modo virtual/standby: ${e.message}")
        }
    }

    fun isNativeReady(): Boolean = isLibraryLoaded

    fun initTracker(width: Int, height: Int): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeInitTracker(width, height)
            } catch (e: Throwable) {
                false
            }
        } else {
            true
        }
    }

    fun processFrameYUV(
        yPlaneData: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): VtuberFaceTrackingData {
        if (isLibraryLoaded) {
            try {
                val array = nativeProcessFrameYUV(
                    yPlaneData,
                    rowStride,
                    width,
                    height,
                    rotationDegrees,
                    isFrontCamera
                )
                if (array != null && array.size >= 10) {
                    return VtuberFaceTrackingData(
                        faceDetected = array[0] > 0.5f,
                        leftEyeOpenness = array[1],
                        rightEyeOpenness = array[2],
                        mouthOpenness = array[3],
                        smileRatio = array[4],
                        headPitch = array[5],
                        headYaw = array[6],
                        headRoll = array[7],
                        confidence = array[8],
                        processingTimeUs = array[9].toLong()
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en processFrameYUV nativo: ${e.message}")
            }
        }
        return VtuberFaceTrackingData()
    }

    fun processDirectLandmarks(
        landmarks3D: FloatArray,
        count: Int,
        eyeThreshold: Float,
        mouthThreshold: Float
    ): VtuberFaceTrackingData {
        if (isLibraryLoaded) {
            try {
                val array = nativeProcessDirectLandmarks(
                    landmarks3D,
                    count,
                    eyeThreshold,
                    mouthThreshold
                )
                if (array != null && array.size >= 10) {
                    return VtuberFaceTrackingData(
                        faceDetected = array[0] > 0.5f,
                        leftEyeOpenness = array[1],
                        rightEyeOpenness = array[2],
                        mouthOpenness = array[3],
                        smileRatio = array[4],
                        headPitch = array[5],
                        headYaw = array[6],
                        headRoll = array[7],
                        confidence = array[8],
                        processingTimeUs = array[9].toLong()
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en processDirectLandmarks nativo: ${e.message}")
            }
        }
        return VtuberFaceTrackingData()
    }

    fun setSmoothingFactor(alpha: Float) {
        if (isLibraryLoaded) {
            try {
                nativeSetSmoothingFactor(alpha)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun resetTemporalFilter() {
        if (isLibraryLoaded) {
            try {
                nativeResetTemporalFilter()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun releaseTracker() {
        if (isLibraryLoaded) {
            try {
                nativeReleaseTracker()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    private external fun nativeInitTracker(width: Int, height: Int): Boolean
    private external fun nativeProcessFrameYUV(
        yPlaneData: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): FloatArray?

    private external fun nativeProcessDirectLandmarks(
        landmarks3D: FloatArray,
        count: Int,
        eyeThreshold: Float,
        mouthThreshold: Float
    ): FloatArray?

    private external fun nativeSetSmoothingFactor(alpha: Float)
    private external fun nativeResetTemporalFilter()
    private external fun nativeReleaseTracker()
}
