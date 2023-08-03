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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        maven("https://gitlab.com/api/v4/projects/26729549/packages/maven")
    }
}

includeBuild("androidApp/src/thirdparty/NewPipeExtractor") {
    dependencySubstitution {
        substitute(module("com.github.TeamNewPipe:NewPipeExtractor")).using(project(":extractor"))
    }
}
