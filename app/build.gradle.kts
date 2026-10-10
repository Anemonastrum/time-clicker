plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "id.anemona.clicker"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.anemona.clicker"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ponytail: signed with the debug keystore so the release APK is installable out of
    // the box; add a real upload key + secrets (storeFile/storePassword/keyAlias) when
    // publishing to Play or when release-key provenance starts to matter.
    signingConfigs {
        create("release") {
            // Overridden by CI secrets when present, else falls back to the debug keystore.
            System.getenv("RELEASE_STORE_FILE")?.let { storeFile = file(it) }
            System.getenv("RELEASE_STORE_PASSWORD")?.let { storePassword = it }
            System.getenv("RELEASE_KEY_ALIAS")?.let { keyAlias = it }
            System.getenv("RELEASE_KEY_PASSWORD")?.let { keyPassword = it }
            if (storeFile == null) {
                // Debug keystore lives in ~/.android on the build host.
                storeFile = file(
                    System.getProperty("user.home") + "/.android/debug.keystore"
                )
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity)

    // MIUI X (HyperOS style) UI library.
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)

    // JetBrains Compose Multiplatform artifacts; the compose compiler plugin wires the
    // Android variants into an AGP module. material3/ui-tooling are not published to
    // Maven Central past 1.9.0 — miuix supplies the components instead.
    implementation("org.jetbrains.compose.ui:ui:1.12.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
    implementation("org.jetbrains.compose.runtime:runtime:1.12.0")
}
