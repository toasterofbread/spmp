import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

val buildConfigDir get() = project.layout.buildDirectory.dir("generated/buildconfig")

fun GenerateBuildConfig.buildConfig(debug: Boolean) {
    class_fq_name.set("com.toasterofbread.spmp.ProjectBuildConfig")
    generated_output_dir.set(buildConfigDir)

    val keys = Properties()

    keys.load(FileInputStream(rootProject.file("keys.properties")))
    for (item in keys) {
        fields_to_generate.add(Triple(item.key.toString(), null, item.value.toString()))
    }

    keys.clear()
    keys.load(FileInputStream(rootProject.file("debug_keys.properties")))
    for (item in keys) {
        fields_to_generate.add(Triple(item.key.toString(), "String?", if (debug) item.value.toString() else null.toString()))
    }

    fields_to_generate.add(Triple("IS_DEBUG", "Boolean", debug.toString()))
}

val buildConfigDebug = tasks.register("buildConfigDebug", GenerateBuildConfig::class.java) {
    buildConfig(debug = true)
}
val buildConfigRelease = tasks.register("buildConfigRelease", GenerateBuildConfig::class.java) {
    buildConfig(debug = true)
}

tasks.all {
    when (name) {
        "generateDebugBuildConfig" -> dependsOn(buildConfigDebug)
        "generateReleaseBuildConfig" -> dependsOn(buildConfigRelease)
    }
}

kotlin {
    android()

    jvm("desktop")

    sourceSets {
        val compose_version = extra["compose.version"] as String

        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.material)

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                implementation("com.squareup.okhttp3:okhttp:4.10.0")

                implementation("com.beust:klaxon:5.5")
                implementation("com.godaddy.android.colorpicker:compose-color-picker:0.7.0")
                implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.22.1")
                implementation("org.apache.commons:commons-text:1.10.0")
                implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
                implementation("org.jsoup:jsoup:1.15.3")
                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
                implementation("com.github.SvenWoltmann:color-thief-java:v1.1.2")
                implementation("com.github.catppuccin:java:v1.0.0")
                implementation("com.github.paramsen:noise:2.0.0")
                implementation("com.github.jeziellago:compose-markdown:0.3.3")
//                implementation("org.xmlpull:xmlpull:1.1.4.0")
            }
            kotlin.srcDir(buildConfigDir)
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.6.1")
                api("androidx.core:core-ktx:1.9.0")
                api("androidx.appcompat:appcompat:1.6.1")

                val media3_version = "1.1.0-beta01"
                implementation("androidx.media3:media3-exoplayer:$media3_version")
                implementation("androidx.media3:media3-ui:$media3_version")
                implementation("androidx.media3:media3-session:$media3_version")

                implementation("com.google.accompanist:accompanist-pager:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-pager-indicators:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-systemuicontroller:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-swiperefresh:0.21.2-beta")
                implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
                implementation("androidx.palette:palette:1.0.0")
                //noinspection GradleDependency
                implementation("com.github.andob:android-awt:1.0.0")
                implementation("io.coil-kt:coil-compose:2.3.0")
                implementation("com.github.dead8309:KizzyRPC:1.0.71")
                implementation("dev.kord:kord-core:0.9.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation("com.github.bluemods:kxml2:4dae70b2a995e72f842eca0c778792ce90d6cfc7")
                implementation("org.zeromq:jeromq:0.5.3")
                implementation("com.github.ltttttttttttt:load-the-image:1.0.5")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.toasterofbread.spmp.shared"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["main"].kotlin.srcDir(buildConfigDir)

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain {
            version = "11"
        }
    }
    buildFeatures {
        compose = true
    }
}

open class GenerateBuildConfig : DefaultTask() {
    @get:Input
    val fields_to_generate: ListProperty<Triple<String, String?, String>> = project.objects.listProperty()

    @get:Input
    val class_fq_name: Property<String> = project.objects.property()

    @get:OutputDirectory
    val generated_output_dir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun execute() {
        val dir = generated_output_dir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()

        val class_parts = class_fq_name.get().split(".")
        val class_name = class_parts.last()
        val file = dir.resolve("$class_name.kt")

        val content = buildString {
            if (class_parts.size > 1) {
                appendLine("@file:Suppress(\"RedundantNullableReturnType\", \"MayBeConstant\")\n")
                appendLine("package ${class_parts.dropLast(1).joinToString(".")}")
            }

            appendLine()
            appendLine("/* Generated on build in shared/build.gradle.kts */")
            appendLine("object $class_name {")

            for (field in fields_to_generate.get().sortedBy { it.first }) {
                val type = field.second?.let { ": $it" } ?: ""
                appendLine("    val ${field.first}$type = ${field.third}")
            }

            appendLine("}")
        }

        file.writeText(content)
    }
}
