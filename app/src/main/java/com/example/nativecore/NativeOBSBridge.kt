package com.example.nativecore

import android.util.Log

/**
 * Native C++ OBS Engine Bridge.
 * Interfaces with `libobs_core.so` (OpenGL ES 3.0 Scene Compositor and Hardware Pipeline).
 */
object NativeOBSBridge {
    private const val TAG = "NativeOBSBridge"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("obs_core")
            isLibraryLoaded = true
            Log.i(TAG, "Successfully loaded native C++ library 'obs_core'")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            Log.w(TAG, "Native C++ library 'obs_core' not yet linked or compiled in host environment: ${e.message}")
        }
    }

    fun isNativeReady(): Boolean = isLibraryLoaded

    fun getEngineVersion(): String {
        return if (isLibraryLoaded) {
            try {
                nativeGetEngineVersion()
            } catch (e: Throwable) {
                "C++ Engine Standby (JNI Ready)"
            }
        } else {
            "C++ Engine Preparado (Estructura NDK GLES3 / CMake activa)"
        }
    }

    fun initCompositor(width: Int, height: Int, fps: Int): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeInitCompositor(width, height, fps)
            } catch (e: Throwable) {
                false
            }
        } else {
            true // Virtual compositor stub ready
        }
    }

    fun addSource(name: String, sourceType: Int, x: Float, y: Float, width: Float, height: Float): Int {
        return if (isLibraryLoaded) {
            try {
                nativeAddSource(name, sourceType, x, y, width, height)
            } catch (e: Throwable) {
                -1
            }
        } else {
            1 // Mock source ID
        }
    }

    fun removeSource(sourceId: Int): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeRemoveSource(sourceId)
            } catch (e: Throwable) {
                false
            }
        } else {
            true
        }
    }

    fun getSourceCount(): Int {
        return if (isLibraryLoaded) {
            try {
                nativeGetSourceCount()
            } catch (e: Throwable) {
                0
            }
        } else {
            0
        }
    }

    fun releaseCompositor() {
        if (isLibraryLoaded) {
            try {
                nativeReleaseCompositor()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    // JNI Native method declarations
    private external fun nativeGetEngineVersion(): String
    private external fun nativeInitCompositor(width: Int, height: Int, fps: Int): Boolean
    private external fun nativeReleaseCompositor()
    private external fun nativeAddSource(name: String, sourceType: Int, x: Float, y: Float, width: Float, height: Float): Int
    private external fun nativeRemoveSource(sourceId: Int): Boolean
    private external fun nativeGetSourceCount(): Int
}
