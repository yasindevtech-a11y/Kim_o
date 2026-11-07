plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tr.zil.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "tr.zil.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.2"

        // Ziyaretçi sayfasının adresi. GitHub Actions derlerken otomatik verir
        // (-PpageUrl=...), kullanıcı ayrıca bir şey girmek zorunda kalmaz.
        val pageUrl = (project.findProperty("pageUrl") as String?) ?: ""
        buildConfigField("String", "DEFAULT_PAGE_URL", "\"$pageUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Sabit imza anahtarı: her derlemede aynı imza olur, böylece APK'yı
    // eskisinin üzerine güncelleyebilirsiniz.
    signingConfigs {
        create("zil") {
            storeFile = file("zil.keystore")
            storePassword = "zil12345"
            keyAlias = "zil"
            keyPassword = "zil12345"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("zil")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("zil")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.zxing:core:3.5.3")
}
