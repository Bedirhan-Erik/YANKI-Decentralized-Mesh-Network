import org.gradle.api.file.SourceDirectorySet

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.bedir.yanki"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bedir.yanki"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            // Proto dosyalarını kaynak olarak ekliyoruz. 
            // Kotlin DSL bazen doğrudan tanımayabilir, bu yüzden bu yapılandırmayı protobuf bloğunda yapacağız.
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation("com.google.crypto.tink:tink-android:1.12.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
