import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    val xcf = XCFramework("Shared")

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)

            export(projects.boundary)
            export(projects.common)
            export(projects.interactors)
            export(projects.application)
            export(projects.data)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.boundary)
            api(projects.common)
            api(projects.interactors)
            api(projects.application)
            api(projects.data)
        }
    }
}
