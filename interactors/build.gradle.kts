import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    // A program is configuration, so its shape and its wire format are the same
    // thing. Keeping one set of models beats a parallel set of asset DTOs and the
    // mappers between them, which could only ever drift.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        // Java is pinned to 17 below; Kotlin otherwise follows the JDK running
        // Gradle, which is 21 in Android Studio and fails the target check.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            // Settings are observed as a Flow by the presentation layer.
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
            api(libs.kotlinx.serialization.json)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.unit.test)
        }
    }
}

android {
    namespace = "com.lexicon.interactors"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
