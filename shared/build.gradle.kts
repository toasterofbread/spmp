@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockStoreTask
import plugin.spmp.SpMpDeps
import plugin.spmp.getDeps

plugins {
    id("generate-build-config")
    id("generate-dependency-list")

    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

val DATABASE_VERSION: Int = 9 // since #368

kotlin {
    androidTarget()

    jvm("desktop")

//    wasmJs {
//        moduleName = project.parent!!.name
//        browser {
//            commonWebpackConfig {
//                outputFileName = "composeApp.js"
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(project.projectDir.path)
//                        add(project.projectDir.path + "/commonMain/")
//                        add(project.projectDir.path + "/wasmJsMain/")
//                    }
//                }
//            }
//        }
//        binaries.executable()
//    }

    applyDefaultHierarchyTemplate {
        common {
            withAndroidTarget()
            withJvm()
//            withWasmJs()

            group("jvm") {
                withAndroidTarget()
                withJvm()
            }
            group("notAndroid") {
                withJvm()
                withWasmJs()
            }
        }
    }

    sourceSets {
        val deps: SpMpDeps = getDeps()

        all {
            languageSettings.apply {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.material.ExperimentalMaterialApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")

                enableLanguageFeature("ExpectActualClasses")
            }
        }

        commonMain {
            kotlin {
                srcDir(project.layout.buildDirectory.dir(SpMpDeps.OUTPUT_DIR))
            }

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(deps.get("dev.toastbits:spms"))
                implementation(deps.get("dev.toastbits:ytm-kt"))
                implementation(deps.get("dev.toastbits.kana-kt:kanakt"))
                for (dependency in deps.getAllComposeKit()) {
                    implementation(dependency)
                }

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                implementation(deps.get("org.apache.commons:commons-text"))
                implementation(deps.get("com.atilika.kuromoji:kuromoji-ipadic"))
                implementation(deps.get("com.andree-surya:moji4j"))
                implementation(deps.get("com.mohamedrejeb.ksoup:ksoup-html"))
                implementation(deps.get("com.mohamedrejeb.ksoup:ksoup-entities"))
                implementation(deps.get("com.github.toasterofbread.ComposeReorderable:reorderable"))
                implementation(deps.get("com.github.SvenWoltmann:color-thief-java"))
                implementation(deps.get("com.github.paramsen:noise"))
                implementation(deps.get("io.github.pdvrieze.xmlutil:core", "io.github.pdvrieze.xmlutil"))
                implementation(deps.get("io.github.pdvrieze.xmlutil:serialization", "io.github.pdvrieze.xmlutil"))
                implementation(deps.get("org.zeromq:jeromq"))
                implementation(deps.get("io.coil-kt.coil3:coil-compose"))
                implementation(deps.get("io.coil-kt.coil3:coil-network-ktor3"))
                implementation(deps.get("io.ktor:ktor-client-core", "io.ktor"))
                implementation(deps.get("io.ktor:ktor-client-content-negotiation", "io.ktor"))
                implementation(deps.get("io.ktor:ktor-serialization-kotlinx-json", "io.ktor"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(deps.get("io.ktor:ktor-client-cio", "io.ktor"))
                implementation(deps.get("com.github.toasterofbread.compose-webview-multiplatform:compose-webview-multiplatform"))
                implementation(deps.get("org.bitbucket.ijabz:jaudiotagger"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.9.0")
                api("androidx.core:core-ktx:1.13.1")
                api("androidx.appcompat:appcompat:1.7.0")

                implementation("androidx.palette:palette:1.0.0")
                implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
                implementation(deps.get("androidx.media3:media3-exoplayer", "androidx.media3"))
                implementation(deps.get("androidx.media3:media3-ui", "androidx.media3"))
                implementation(deps.get("androidx.media3:media3-session", "androidx.media3"))
                implementation(deps.get("com.google.accompanist:accompanist-pager"))

                implementation(deps.get("com.google.accompanist:accompanist-pager-indicators"))
                implementation(deps.get("com.google.accompanist:accompanist-systemuicontroller"))
                // implementation(deps.get("com.github.andob:android-awt"))
                implementation(deps.get("com.github.toasterofbread:KizzyRPC"))
                implementation(deps.get("app.cash.sqldelight:android-driver"))
                implementation(deps.get("com.anggrayudi:storage"))
                implementation(deps.get("io.github.jan-tennert.supabase:functions-kt"))
                implementation(deps.get("io.ktor:ktor-client-cio"))

                // Widget
                implementation("androidx.glance:glance-appwidget:1.1.1")
                implementation("androidx.glance:glance-material3:1.1.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)

                implementation(deps.get("dev.toastbits.mediasession:library-jvm"))

                implementation(deps.get("app.cash.sqldelight:sqlite-driver"))
                implementation(deps.get("com.github.caoimhebyrne:KDiscordIPC"))
                implementation(deps.get("org.bytedeco:ffmpeg-platform"))
            }
        }

//        val wasmJsMain by getting {
//            dependencies {
//                implementation(deps.get("io.ktor:ktor-client-js", "io.ktor"))
//                implementation(deps.get("app.cash.sqldelight:web-worker-driver-wasm-js"))
//            }
//        }
    }
}

compose {
    resources {
        publicResClass = true
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.toasterofbread.spmp.shared"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }

    sourceSets.getByName("main") {
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }
    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.toasterofbread.${project.parent!!.name}.db")
            // Version specification kept for backwards-compatibility
            version = DATABASE_VERSION
        }
    }
}

val fixDatabaseVersion = tasks.register("fixDatabaseVersion") {
    doLast {
        val file: File = project.file("build/generated/sqldelight/code/Database/commonMain/com/toasterofbread/${project.parent!!.name}/db/shared/DatabaseImpl.kt")
        val lines: MutableList<String> = file.readLines().toMutableList()
        var found: Boolean = false

        for (i in 0 until lines.size) {
            if (lines[i].endsWith("override val version: Long")) {
                lines[i + 1] = "      get() = $DATABASE_VERSION"
                found = true
                break
            }
        }

        check(found) { "Version line not found in $file" }

        file.writer().use { writer ->
            for (line in lines) {
                writer.write(line + "\n")
            }
        }
    }
}

afterEvaluate {
    tasks.getByName("generateCommonMainDatabaseInterface") {
        finalizedBy(fixDatabaseVersion)
    }

//    rootProject.tasks.apply {
//        getByName("kotlinNodeJsSetup") {
//            enabled = false
//        }
//
//        getByName("kotlinNpmInstall") {
//            enabled = false
//        }
//
//        getByName<YarnLockStoreTask>("kotlinStoreYarnLock") {
//            inputFile.asFile.get().apply {
//                parentFile.mkdirs()
//                createNewFile()
//            }
//        }
//    }
}
