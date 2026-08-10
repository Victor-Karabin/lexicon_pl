plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lexicon.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation(projects.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Lesson recordings are fetched over plain HTTP; the shared client is provided by the app module.
    implementation(libs.okhttp.core)

    // Provides the @ApplicationContext qualifier and javax.inject.Inject only.
    // No Hilt @Module lives here — those are restricted to the app module per the architecture spec.
    implementation(libs.hilt.android)

    testImplementation(libs.bundles.unit.test)
}
