plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.interactors)
    implementation(projects.boundary)
    implementation(projects.common)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.unit.test)
}
