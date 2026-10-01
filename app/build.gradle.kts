plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dtouch.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.dtouch.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    signingConfigs {
        create("ciRelease") {
            storeFile = file(System.getenv("DTOUCH_KEYSTORE") ?: "ci-release.jks")
            storePassword = System.getenv("DTOUCH_STORE_PASSWORD") ?: "dtouch-test-store"
            keyAlias = System.getenv("DTOUCH_KEY_ALIAS") ?: "dtouch"
            keyPassword = System.getenv("DTOUCH_KEY_PASSWORD") ?: "dtouch-test-key"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ciRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += listOf("task", "tflite") }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}
