plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Settings are observed as a Flow by the presentation layer.
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)

    testImplementation(libs.bundles.unit.test)
}
