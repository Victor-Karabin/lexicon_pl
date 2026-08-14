import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
            // Settings are observed as a Flow, so the repository contract needs coroutines.
            implementation(libs.kotlinx.coroutines.core)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.unit.test)
        }
    }
}

android {
    namespace = "com.lexicon.boundary"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
