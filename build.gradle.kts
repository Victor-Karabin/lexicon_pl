plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // detekt only discovers src/main|test/kotlin on its own, so a multiplatform module's
    // commonMain/androidMain/iosMain/androidUnitTest would silently go unanalysed —
    // NO-SOURCE rather than a failure. Point it at whatever source dirs actually exist.
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            source.setFrom(
                files(
                    "src/main/kotlin",
                    "src/test/kotlin",
                    "src/commonMain/kotlin",
                    "src/commonTest/kotlin",
                    "src/androidMain/kotlin",
                    "src/androidUnitTest/kotlin",
                    "src/iosMain/kotlin",
                ).filter { it.exists() },
            )
        }
    }
}
