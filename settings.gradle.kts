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
        // REQUIRED: Adding JitPack repository for MapLibre dependencies
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ai37"
include(":app")