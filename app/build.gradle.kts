import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.soogoino.hugadroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soogoino.hugadroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Read from local.properties (local dev) or environment variables (CI / GitHub Actions).
            // Add these 4 entries to local.properties (never commit that file):
            //   KEYSTORE_PATH=/absolute/path/to/release.jks
            //   KEYSTORE_PASSWORD=yourKeystorePass
            //   KEY_ALIAS=yourKeyAlias
            //   KEY_PASSWORD=yourKeyPass
            val props = Properties()
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
            val path  = props.getProperty("KEYSTORE_PATH")     ?: System.getenv("KEYSTORE_PATH")
            val pass  = props.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            val alias = props.getProperty("KEY_ALIAS")         ?: System.getenv("KEY_ALIAS")
            val kPass = props.getProperty("KEY_PASSWORD")      ?: System.getenv("KEY_PASSWORD")
            if (path != null) {
                storeFile     = file(path)
                storePassword = pass
                keyAlias      = alias
                keyPassword   = kPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // JGit brings in duplicate slf4j providers
            excludes += "META-INF/services/org.eclipse.jgit.*"
            pickFirsts += "META-INF/DEPENDENCIES"
            // JSch + BouncyCastle both ship OSGI manifests under META-INF/versions
            excludes += "META-INF/versions/**"
            excludes += "OSGI-INF/**"
            excludes += "about.html"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // JGit (Git operations)
    implementation(libs.jgit.core)

    // JSch SSH transport (Android-compatible, replaces MINA SSHD)
    implementation(libs.jsch)

    // YAML / TOML parsing
    implementation(libs.snakeyaml.engine)
    implementation(libs.ktoml.core)

    // Markdown / Rich Editor
    implementation(libs.richeditor.compose)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)

    // Bouncy Castle (SSH keygen on device)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)

    // Secure storage for PAT tokens
    implementation(libs.security.crypto)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
