plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.interactors)
            implementation(projects.boundary)
            implementation(projects.common)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.unit.test)
        }
    }
}

android {
    namespace = "com.lexicon.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
