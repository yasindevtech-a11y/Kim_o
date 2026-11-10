import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------- AdMob ayarları
// Ayarlar kök klasördeki admob.properties dosyasından okunur.
// USE_TEST=true iken Google'ın TEST reklamları kullanılır (kendi reklamınıza tıklama riski yok).
val admob = Properties().apply {
    val f = rootProject.file("admob.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val testAppId = "ca-app-pub-3940256099942544~3347511713"     // Google'ın resmi test uygulama kimliği
val testBannerId = "ca-app-pub-3940256099942544/6300978111"  // Google'ın resmi test banner kimliği
val admobLive = admob.getProperty("USE_TEST", "true").trim() == "false" &&
    admob.getProperty("APP_ID", "").trim().isNotEmpty() &&
    admob.getProperty("BANNER_ID", "").trim().isNotEmpty()
val admobAppId = if (admobLive) admob.getProperty("APP_ID").trim() else testAppId
val admobBannerId = if (admobLive) admob.getProperty("BANNER_ID").trim() else testBannerId

// Play Store için yükleme anahtarı (GitHub Secrets'tan gelir). Yoksa depodaki anahtar kullanılır.
val uploadPath: String? = System.getenv("UPLOAD_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "tr.zil.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "tr.zil.app"
        minSdk = 26
        targetSdk = 36          // Play Store, Ağustos 2026'dan beri hedef API 36 ister
        versionCode = 7
        versionName = "1.7"

        // Ziyaretçi sayfasının adresi. GitHub Actions derlerken otomatik verir
        // (-PpageUrl=...), kullanıcı ayrıca bir şey girmek zorunda kalmaz.
        val pageUrl = (project.findProperty("pageUrl") as String?) ?: ""
        buildConfigField("String", "DEFAULT_PAGE_URL", "\"$pageUrl\"")

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("boolean", "ADMOB_LIVE", "$admobLive")
    }

    buildFeatures {
        buildConfig = true
    }

    // Release derlemesinde "lintVital" denetimi bir uyarı yüzünden derlemeyi durdurmasın
    lint {
        checkReleaseBuilds = false
        abortOnError = false
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
        if (uploadPath != null && file(uploadPath).exists()) {
            create("upload") {
                storeFile = file(uploadPath)
                storePassword = System.getenv("UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("zil")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("zil")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Kotlin 2.2+ eski "kotlinOptions" yazımını kabul etmez
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
