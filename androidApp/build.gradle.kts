import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "com.nafis.calories"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nafis.calories"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val backendUrl = localProperties.getProperty("BACKEND_BASE_URL", "http://10.0.2.2:8000/").trim()
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = true
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["usesCleartextTraffic"] = false
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}