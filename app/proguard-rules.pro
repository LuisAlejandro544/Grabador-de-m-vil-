# =============================================================================
# Vortex Studio - ProGuard & R8 Optimization Rules
# =============================================================================

# 1. Conservar Atributos Esenciales y Metadatos para Debugging
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# 2. Puentes Nativos JNI (C++ NDK & Rust) - CRÍTICO
# Evita la ofuscación o eliminación de métodos nativos en tiempo de compilación
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclasseswithmembers class com.example.nativecore.** {
    native <methods>;
}

-keep class com.example.nativecore.** { *; }
-keepclassmembers class com.example.nativecore.** { *; }

# 3. Modelos de Datos, Serialización (Moshi) y Repositorios
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# 4. Servicios en Primer Plano, Receptores de Sistema y Overlays Flotantes
-keep class com.example.service.** { *; }
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    public <fields>;
}

# 5. Jetpack Compose & Kotlin Coroutines
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.android.** { *; }
-dontwarn kotlinx.coroutines.**

# 6. CameraX & AndroidX Lifecycle
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class androidx.lifecycle.** { *; }

# 7. Redes (OkHttp & Retrofit)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 8. Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# 9. Eliminación de Logs en Release para Máximo Rendimiento (60 FPS fluidos)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 10. Seguridad contra Ingeniería Inversa y Aplanado de Paquetes (Repackaging)
-repackageclasses 'com.vortexstudio.internal'
-allowaccessmodification


