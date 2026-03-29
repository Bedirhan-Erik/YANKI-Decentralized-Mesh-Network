plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf") // Protobuf eklentisi
    id("com.google.dagger.hilt.android") // Hilt
    kotlin("kapt") // Room ve Hilt için gerekli
}

android {
    namespace = "com.bedir.yanki"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bedir.yanki"
        minSdk = 26 // Wi-Fi Aware için en az API 26 (Android 8.0) gerekli
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // .proto dosyalarının yerini Gradle'a gösteriyoruz
    sourceSets {
        getByName("main") {
            proto {
                srcDir("../protocols")
            }
        }
    }
}

// Protobuf Derleme Ayarları
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") } // Mobil için hafif sürüm
            }
        }
    }
}

dependencies {
    // --- GÜVENLİK (Google Tink) ---
    implementation("com.google.crypto.tink:tink-android:1.12.0")

    // --- VERİ PAKETLEME (Protobuf) ---
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")

    // --- VERİTABANI (Room) ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- BAĞIMLILIK ENJEKSİYONU (Hilt) ---
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // --- ARKA PLAN İŞLERİ (WorkManager) ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}