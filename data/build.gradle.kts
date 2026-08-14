plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // expect/actual classes are still flagged Beta; the three here (database builder,
    // datastore path, asset reader) are exactly the intended use.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // api: dataModule binds Clock/DispatcherProvider and the repository
            // interfaces, so consumers of this module see those types.
            api(projects.boundary)
            api(projects.common)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            // The data layer owns its own Koin wiring: only it knows which pieces are
            // platform-specific (see di/DataModule).
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)

            // The image-provider APIs are Retrofit-based and stay Android-only; iOS has
            // no RemoteImageSource yet (see FallbackImageProviderImpl, which takes
            // whatever the platform's DI module supplies).
            implementation(libs.retrofit.core)
            implementation(libs.retrofit.kotlinx.serialization)
            implementation(libs.okhttp.core)
            implementation(libs.okhttp.logging)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.unit.test)
        }
    }
}

// One declaration wires the Room compiler across every target.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.lexicon.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
