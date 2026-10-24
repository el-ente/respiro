import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "io.github.elente.respiro"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.elente.respiro"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }

    signingConfigs {
        // Debug builds only: this key is public, so it must never sign what users install. Respiro holds an ADB key
        // the TV trusts, and anyone with the signing key could ship an "update" that inherits it.
        create("dev") {
            storeFile = file("dev.keystore")
            storePassword = "respiro-dev"
            keyAlias = "respiro"
            keyPassword = "respiro-dev"
        }
        // Releases: the private key, given by CI from repository secrets (see CLAUDE.md, Releases).
        System.getenv("RESPIRO_KEYSTORE")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("RESPIRO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RESPIRO_KEY_ALIAS")
                keyPassword = System.getenv("RESPIRO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
        }
        release {
            isMinifyEnabled = false
            // Without the release key (local builds, pull requests) a release build is signed with the dev key, for
            // testing only; published APKs always come from CI with the private key.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("dev")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("dev.mobile:dadb:1.2.10")

    testImplementation("junit:junit:4.13.2")
}
