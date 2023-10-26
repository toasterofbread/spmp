rootProject.name = "SpMp"

include(":ToasterComposeTools:lib")
include(":shared")
include(":androidApp")
include(":desktopApp")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    plugins {
        val kotlin_version = extra["kotlin.version"] as String
        val agp_version = extra["agp.version"] as String
        val compose_version = extra["compose.version"] as String

        kotlin("jvm").version(kotlin_version)
        kotlin("multiplatform").version(kotlin_version)
        kotlin("android").version(kotlin_version)

        id("com.android.application").version(agp_version)
        id("com.android.library").version(agp_version)

        id("org.jetbrains.compose").version(compose_version)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
