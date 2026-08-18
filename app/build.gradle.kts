import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val hasGoogleServicesConfig = file("google-services.json").exists()
if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

val localProperties =
    Properties().apply {
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
    }

fun localProperty(key: String) = localProperties.getProperty(key, "")

android {
    namespace = "com.lexicon"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lexicon"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "PEXELS_API_KEY", "\"${localProperty("pexels.apiKey")}\"")
        buildConfigField("String", "PIXABAY_API_KEY", "\"${localProperty("pixabay.apiKey")}\"")
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"${localProperty("unsplash.accessKey")}\"")
        buildConfigField("String", "OPENVERSE_CLIENT_ID", "\"${localProperty("openverse.clientId")}\"")
        buildConfigField("String", "OPENVERSE_CLIENT_SECRET", "\"${localProperty("openverse.clientSecret")}\"")
        buildConfigField("String", "DEEPL_API_KEY", "\"${localProperty("deepl.apiKey")}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${localProperty("openai.apiKey")}\"")
        buildConfigField("String", "GOOGLE_TTS_API_KEY", "\"${localProperty("google.ttsApiKey")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(projects.common)
    implementation(projects.android)
    implementation(projects.boundary)
    implementation(projects.interactors)
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.presentation)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)

    if (hasGoogleServicesConfig) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }

    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.koin.test)
}
