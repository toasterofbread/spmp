rootProject.name = "spmp"

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
        val kotlin_version: String = extra["kotlin.version"] as String
        kotlin("jvm").version(kotlin_version)
        kotlin("multiplatform").version(kotlin_version)
        kotlin("plugin.serialization").version(kotlin_version)
        kotlin("android").version(kotlin_version)

        val agp_version: String = extra["agp.version"] as String
        id("com.android.application").version(agp_version)
        id("com.android.library").version(agp_version)

        val compose_version: String = extra["compose.version"] as String
        id("org.jetbrains.compose").version(compose_version)

        val sqldelight_version: String = extra["sqldelight.version"] as String
        id("app.cash.sqldelight").version(sqldelight_version)
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")

        // https://github.com/KevinnZou/compose-webview-multiplatform
        maven("https://jogamp.org/deployment/maven")
    }
}
