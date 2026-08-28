plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  // =========================================================================
  // MATRIZ DE CANALES DE DISTRIBUCIÓN (v0.1.0)
  // Configuración de canales de lanzamiento: "dev", "canary", "beta", "stable"
  // Permite alternar la variante fácilmente modificando ACTIVE_CHANNEL o con -PvortexChannel=canary
  // =========================================================================
  val activeChannelId = (project.findProperty("vortexChannel") as? String) ?: "dev"
  val (targetAppId, targetVerCode, targetVerName, allowExp) = when (activeChannelId.lowercase()) {
    "canary" -> listOf("com.vortexstudio.recorder.canary", 1001, "0.1.0-canary.1", true)
    "beta"   -> listOf("com.vortexstudio.recorder.beta", 1002, "0.1.0-beta.1", false)
    "stable" -> listOf("com.vortexstudio.recorder", 1003, "0.1.0", false)
    else     -> listOf("com.vortexstudio.recorder.dev", 1000, "0.1.0-dev", true) // Por defecto: dev
  }

  defaultConfig {
    applicationId = targetAppId as String
    minSdk = 24
    targetSdk = 36
    versionCode = targetVerCode as Int
    versionName = targetVerName as String

    buildConfigField("String", "RELEASE_CHANNEL", "\"$activeChannelId\"")
    buildConfigField("boolean", "ENABLE_EXPERIMENTAL_FEATURES", "$allowExp")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17", "-O3")
        arguments("-DANDROID_STL=c++_shared")
      }
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  sourceSets {
    getByName("main") {
      jniLibs.srcDirs("src/main/jniLibs")
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/Vortex-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: "Vortex"
      keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      ndk {
        abiFilters += listOf("arm64-v8a", "armeabi-v7a")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      ndk {
        abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  packaging {
    jniLibs {
      pickFirsts += listOf(
        "**/libvortex_rust_network.so",
        "**/libavcodec.so",
        "**/libavformat.so",
        "**/libavfilter.so",
        "**/libavutil.so",
        "**/libswscale.so",
        "**/libswresample.so",
        "**/libc++_shared.so"
      )
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
