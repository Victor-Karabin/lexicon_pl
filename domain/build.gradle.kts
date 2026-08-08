plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.interactors)
    implementation(projects.boundary)
    implementation(projects.common)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
    implementation(libs.javax.inject)

    testImplementation(libs.bundles.unit.test)
}
