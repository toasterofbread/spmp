import java.util.Properties
import java.io.FileInputStream

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

val buildConfigDir get() = project.layout.buildDirectory.dir("generated/buildconfig")

fun GenerateBuildConfig.buildConfig(debug: Boolean) {
    classFqName.set("com.spectre7.spmp.ProjectBuildConfig")
    generatedOutputDir.set(buildConfigDir)

    val keys_name = "LocalKeys"
    val keys_type = "Map<String, String>?"
    val keys_file = rootProject.file("keys.properties")

    if (debug && keys_file.exists()) {
        val keys = Properties()
        keys.load(FileInputStream(keys_file))

        var map = "mapOf("
        var i = 0

        for (item in keys) {
            map += "\"${item.key}\" to ${item.value}"
            if (++i != keys.size) {
                map += ", "
            }
        }

        fieldsToGenerate.add(Triple(keys_name, keys_type, "$map)"))
    }
    else {
        fieldsToGenerate.add(Triple(keys_name, keys_type, "null"))
    }

    fieldsToGenerate.add(Triple("IS_DEBUG", "Boolean", debug.toString()))
}

val buildConfigDebug = tasks.register("buildConfigDebug", GenerateBuildConfig::class.java) {
    buildConfig(true)
}
val buildConfigRelease = tasks.register("buildConfigRelease", GenerateBuildConfig::class.java) {
    buildConfig(true)
}

tasks.all {
    if (name.startsWith("buildConfig")) {
        return@all
    }

    if (name.toLowerCase().contains("debug")) {
        dependsOn(buildConfigDebug)
    }
    else {
        dependsOn(buildConfigRelease)
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
                implementation("org.xmlpull:xmlpull:1.1.4.0")
                implementation("org.jsoup:jsoup:1.15.3")
                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
                implementation("com.github.ltttttttttttt:load-the-image:1.0.5")
            }
            kotlin.srcDir(buildConfigDir)
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.6.1")
                api("androidx.core:core-ktx:1.9.0")
                api("androidx.appcompat:appcompat:1.6.1")

                // Exoplayer
                implementation(project(":library-core"))
                implementation(project(":library-ui"))
                implementation(project(":extension-mediasession"))

                implementation("com.google.accompanist:accompanist-pager:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-pager-indicators:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-systemuicontroller:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-swiperefresh:0.21.2-beta")
                implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation("com.github.bluemods:kxml2:4dae70b2a995e72f842eca0c778792ce90d6cfc7")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.spectre7.spmp"

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
    val fieldsToGenerate: ListProperty<Triple<String, String, String>> = project.objects.listProperty()

    @get:Input
    val classFqName: Property<String> = project.objects.property()

    @get:OutputDirectory
    val generatedOutputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun execute() {
        val dir = generatedOutputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()

        val fqName = classFqName.get()
        val parts = fqName.split(".")
        val className = parts.last()
        val file = dir.resolve("$className.kt")
        val content = buildString {
            if (parts.size > 1) {
                appendLine("package ${parts.dropLast(1).joinToString(".")}")
            }

            appendLine()
            appendLine("/* GENERATED, DO NOT EDIT MANUALLY! */")
            appendLine("object $className {")

            for (field in fieldsToGenerate.get().sortedBy { it.first }) {
                appendLine("val ${field.first}: ${field.second} = ${field.third}")
            }

            appendLine("}")
        }
        file.writeText(content)
    }
}
