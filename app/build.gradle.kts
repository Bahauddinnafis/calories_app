import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nafis.calories"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nafis.calories"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // Load Gemini API key dari local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val apiKey = if (localPropertiesFile.exists()) {
            val properties = Properties()
            localPropertiesFile.inputStream().use { stream ->
                properties.load(stream)
            }
            properties.getProperty("GEMINI_API_KEY", "")
        } else {
            ""
        }

        // Load backend base URL from local.properties (default for emulator)
        val backendUrl = if (localPropertiesFile.exists()) {
            val properties = Properties()
            localPropertiesFile.inputStream().use { stream ->
                properties.load(stream)
            }
            properties.getProperty("BACKEND_BASE_URL", "http://10.0.2.2:8000/")
        } else {
            "http://10.0.2.2:8000/"
        }

        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
        buildConfig = true  // Enable BuildConfig untuk akses API key
    }
}

configurations.all {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Material Icons Extended untuk Icons.Filled.Home, Icons.Filled.List
    implementation("androidx.compose.material:material-icons-extended")

    // JSON parsing untuk load data makanan dari assets
    implementation(libs.kotlinx.serialization.json)

    // CameraX untuk capture foto makanan
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // TensorFlow Lite untuk custom model inference
    implementation(libs.tflite)
    implementation(libs.tflite.support)

    // Coil untuk display image hasil capture
    implementation(libs.coil.compose)

    // Accompanist untuk handle runtime permission
    implementation(libs.accompanist.permissions)

    // Room Database untuk persistent history storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Gemini AI untuk hybrid food recognition
    implementation(libs.gemini.ai)

    // Retrofit untuk backend API communication
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}