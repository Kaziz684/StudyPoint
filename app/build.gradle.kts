// signing test
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val youtubeApiKey: String = localProperties.getProperty("YOUTUBE_API_KEY") ?: ""
val youtubeChannelId: String = localProperties.getProperty("YOUTUBE_CHANNEL_ID") ?: ""

android {
    namespace = "com.example.studypoint"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studypoint"
        minSdk = 23 // Firebase + PDF Viewer ke liye best
        targetSdk = 35
        versionCode = 87
        versionName = "2.0"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
        buildConfigField("String", "YOUTUBE_CHANNEL_ID", "\"$youtubeChannelId\"")
    }

    // Release signing: uses env vars when present (CI). Local builds fall back to debug signing.
    val releaseKeystorePath = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (!releaseKeystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!releaseKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Lint ko release build rokne se mana karein (CI par signed build ke liye)
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10" // Compose 1.6.x ke liye stable
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 🔹 Android basics
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // 🔹 Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // 🔹 Firebase (BOM auto-manages versions)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    implementation("androidx.viewpager2:viewpager2:1.1.0")

    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // 🔹 Jetpack Compose (stable versions)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material:material:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")

    // ✅ PDF Viewer (latest maintained fork, no jitpack error)
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    //networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //Lifecycle ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    //JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil for Compose image loading (YouTube thumbnails)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // AndroidX Browser for Custom Tabs and social links
    implementation("androidx.browser:browser:1.8.0")
}
