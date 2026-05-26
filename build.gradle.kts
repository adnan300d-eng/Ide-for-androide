plugins {
    id("com.android.application") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.8.10"
}

android {
    namespace = "com.example.codeeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.codeeditor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
}

dependencies {
    // أساسيات نظام أندرويد وواجهات Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // إعدادات الواجهة والأيقونات الخاصة بتطبيقك
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    
    // مكتبة إدارة وقراءة ملفات الهاتف
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // مكتبة الاتصال بالإنترنت والبناء السحابي
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
