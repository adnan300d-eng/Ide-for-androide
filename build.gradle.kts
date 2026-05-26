plugins {
    id("com.android.application") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.8.10"
}

android {
    namespace = "com.example.codeeditor"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.codeeditor"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // مكتبات أندرويد الأساسية المستقرة
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}
