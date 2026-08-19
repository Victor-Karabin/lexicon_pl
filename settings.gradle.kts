enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lexicon"

include(":app")
include(":common")
include(":android")
include(":model")
include(":boundary")
include(":interactors")
include(":application")
include(":data")
include(":presentation")
include(":shared")
