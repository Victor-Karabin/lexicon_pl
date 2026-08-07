plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Settings are observed as a Flow by the presentation layer.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.unit.test)
}
