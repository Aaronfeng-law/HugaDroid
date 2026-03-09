// Top-level build file

// Override the R8 version bundled with AGP so it can parse Kotlin 2.2.x metadata
// produced by transitive dependencies (ktoml, kotlinx-serialization 1.9.x).
// Without this, release builds emit: "WARNING: R8: An error occurred when parsing kotlin metadata."
// See: https://developer.android.com/studio/build/kotlin-d8-r8-versions
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools:r8:8.13.19")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
