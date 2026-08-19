import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {

            api(projects.interactors)
            api(projects.boundary)
            api(projects.common)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)

            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.unit.test)
        }
    }
}

android {
    namespace = "com.lexicon.application"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
