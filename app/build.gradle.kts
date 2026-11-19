plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.abisor.ai"
    compileSdk = 34
    signingConfigs {
        getByName("debug") {
            storeFile = file("abisor.keystore")
            storePassword = "abisor123"
            keyAlias = "abisor"
            keyPassword = "abisor123"
        }
    }
    defaultConfig {
        applicationId = "com.abisor.ai"
        minSdk = 21
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
