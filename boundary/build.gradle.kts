plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Settings are observed as a Flow, so the repository contract needs coroutines.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.unit.test)
}
