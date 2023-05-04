rootProject.name = "SpMp"

include(":shared")
include(":androidApp")
include(":desktopApp")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    plugins {
        val kotlinVersion = extra["kotlin.version"] as String
        val agpVersion = extra["agp.version"] as String
        val composeVersion = extra["compose.version"] as String

        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)

        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)

        id("org.jetbrains.compose").version(composeVersion)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        maven("https://gitlab.com/api/v4/projects/26729549/packages/maven")
        maven("https://maven.andob.info/repository/open_source")
    }
}

apply(from = "androidApp/src/thirdparty/ExoPlayer/core_settings.gradle")

includeBuild("androidApp/src/thirdparty/NewPipeExtractor") {
    dependencySubstitution {
        substitute(module("com.github.TeamNewPipe:NewPipeExtractor")).using(project(":extractor"))
    }
}
