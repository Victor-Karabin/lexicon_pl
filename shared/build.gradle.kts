import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * What an iOS app links against: the shared logic modules bundled into one
 * framework, plus the Koin entry point that wires them together.
 *
 * There is no Android target here — the Android app depends on :domain, :data
 * and friends directly. This module exists purely to produce the XCFramework.
 */
kotlin {
    val xcf = XCFramework("Shared")

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.boundary)
            api(projects.common)
            api(projects.interactors)
            api(projects.domain)
            api(projects.data)
        }
    }
}
