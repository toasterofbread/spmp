rootProject.name = "spmp"

include(":ComposeKit:lib")
include(":spmp-server")
include(":shared")
include(":androidApp")
include(":desktopApp")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
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
    repositories {
        google()
        mavenCentral()

        // https://github.com/orgs/community/discussions/26634
        val key: String = "M67GD0wv5HhykCWA5oPN2nD3021qBTGofbBW_phg".reversed()
        maven("https://toasterofbread:${key}@maven.pkg.github.com/toasterofbread/mediasession-kt")

        maven("https://jitpack.io")
        mavenLocal()
    }
}
