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
            // api: domainModule binds the use-case interfaces, so consumers see them.
            api(projects.interactors)
            api(projects.boundary)
            api(projects.common)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)

            // The domain layer owns the wiring of its own use cases.
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
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
