@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
                implementation(project(":ToasterComposeTools:lib"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "KotlinMultiplatformComposeDesktopApplication"
            packageVersion = "0.2.3"

            modules("java.sql")
        }

//        buildTypes.release.proguard {
//            version.set("7.4.0-beta02")
//        }
    }
}
