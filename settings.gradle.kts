pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "Intra Example"
include(":app")
include(":intra")
