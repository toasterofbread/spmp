import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.0"
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight") version "2.0.0"
}

val KEY_NAMES = mapOf(
    "PASTE_EE_TOKEN" to "String",
    "SUPABASE_URL" to "String",
    "SUPABASE_KEY" to "String",
    "DISCORD_APPLICATION_ID" to "String"
)
val DEBUG_KEY_NAMES = mapOf(
    "YTM_CHANNEL_ID" to "String",
    "YTM_COOKIE" to "String",
    "YTM_HEADERS" to "String",
    "DISCORD_ACCOUNT_TOKEN" to "String",
    "DISCORD_ERROR_REPORT_WEBHOOK" to "String",
    "DISCORD_STATUS_TEXT_NAME_OVERRIDE" to "String",
    "DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE" to "String",
    "DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE" to "String",
    "DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE" to "String",
    "DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE" to "String",
    "DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE" to "String",
    "MUTE_PLAYER" to "Boolean",
    "DISABLE_PERSISTENT_QUEUE" to "Boolean",
    "STATUS_WEBHOOK_URL" to "String",
    "STATUS_WEBHOOK_PAYLOAD" to "String",
    "SERVER_PORT" to "Int"
)

val buildConfigDir: Provider<Directory> get() = project.layout.buildDirectory.dir("generated/buildconfig")

fun GenerateBuildConfig.buildConfig(debug: Boolean) {
    val keys: Properties = Properties()

    fun loadKeys(file: File, getType: (key: String) -> String, key_names: Collection<String>, debug_only: Boolean = true) {
        if (file.isFile) {
            keys.clear()
            keys.load(FileInputStream(file))

            val include = keys["INCLUDE_KEYS"] != "false"

            for (key in key_names) {
                if (key == "INCLUDE_KEYS") {
                    continue
                }

                fields_to_generate.add(
                    Triple(
                        key,
                        getType(key),
                        if (!include || (debug_only && !debug)) null.toString()
                        else keys[key].toString()
                    )
                )
            }
        }
        else {
            val required_keys = key_names.filter { key ->
                !getType(key).endsWith("?")
            }

            if (required_keys.isNotEmpty()) {
                throw NullPointerException("No file found at ${file.path} for required keys $required_keys")
            }

            for (key in key_names) {
                fields_to_generate.add(
                    Triple(
                        key,
                        getType(key),
                        null.toString()
                    )
                )
            }
        }
    }

    class_fq_name.set("com.toasterofbread.spmp.ProjectBuildConfig")
    generated_output_dir.set(buildConfigDir)

    loadKeys(
        rootProject.file("keys.properties"),
        { key ->
            KEY_NAMES[key]!!
        },
        KEY_NAMES.keys,
        debug_only = false
    )

    loadKeys(
        rootProject.file("debug_keys.properties"),
        { key ->
            DEBUG_KEY_NAMES[key]!! + '?'
        },
        DEBUG_KEY_NAMES.keys,
        debug_only = true
    )

    fields_to_generate.add(Triple("GIT_COMMIT_HASH", "String?", getCurrentGitCommitHash().stringify()))
    fields_to_generate.add(Triple("GIT_TAG", "String?", getCurrentGitTag().stringify()))
    fields_to_generate.add(Triple("IS_DEBUG", "Boolean", debug.toString()))
}

fun String?.stringify(): String =
    if (this == null) null.toString()
    else "\"$this\""

val buildConfigDebug: TaskProvider<GenerateBuildConfig> = tasks.register("buildConfigDebug", GenerateBuildConfig::class.java) {
    buildConfig(debug = true)
}
val buildConfigRelease: TaskProvider<GenerateBuildConfig> = tasks.register("buildConfigRelease", GenerateBuildConfig::class.java) {
    buildConfig(debug = false)
}
val buildConfigDesktop: TaskProvider<GenerateBuildConfig> = tasks.register("buildConfigDesktop", GenerateBuildConfig::class.java) {
    gradle.taskGraph.whenReady {
        val debug: Boolean = !gradle.taskGraph.hasTask(":desktopApp:checkRuntime")
        println("buildConfigDesktop will be configured with debug=$debug")
        buildConfig(debug = debug)
    }
}

tasks.all {
    when (name) {
        "preDebugBuild" -> dependsOn(buildConfigDebug)
        "preReleaseBuild" -> dependsOn(buildConfigRelease)
        "compileKotlinDesktop" -> dependsOn(buildConfigDesktop)
    }
}

kotlin {
    androidTarget()

    jvm("desktop")

    sourceSets {
        commonMain {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(project(":ComposeKit:lib"))

                implementation("com.squareup.okhttp3:okhttp:4.11.0")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("org.apache.commons:commons-text:1.10.0")
                implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
                implementation("org.jsoup:jsoup:1.16.1")
                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
                implementation("com.github.SvenWoltmann:color-thief-java:v1.1.2")
                implementation("com.github.catppuccin:java:v1.0.0")
                implementation("com.github.paramsen:noise:2.0.0")
                implementation("org.kobjects.ktxml:core:0.2.3")
                implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
                implementation("com.github.teamnewpipe:NewPipeExtractor:v0.22.7")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.zeromq:jeromq:0.5.3")
            }
            kotlin.srcDir(buildConfigDir)
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.8.1")
                api("androidx.core:core-ktx:1.12.0")
                api("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.palette:palette:1.0.0")
                implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

                val media3_version = "1.2.0"
                implementation("androidx.media3:media3-exoplayer:$media3_version")
                implementation("androidx.media3:media3-ui:$media3_version")
                implementation("androidx.media3:media3-session:$media3_version")

                implementation("com.google.accompanist:accompanist-pager:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-pager-indicators:0.21.2-beta")
                implementation("com.google.accompanist:accompanist-systemuicontroller:0.21.2-beta")
                //noinspection GradleDependency
                implementation("com.github.andob:android-awt:1.0.0")
                implementation("com.github.toasterofbread:KizzyRPC:84e79614b4")
                implementation("app.cash.sqldelight:android-driver:2.0.0")
                implementation("com.anggrayudi:storage:1.5.5")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.0")
                implementation("io.github.jan-tennert.supabase:functions-kt:1.3.2")
                implementation("io.ktor:ktor-client-cio:2.3.6")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation("com.github.ltttttttttttt:load-the-image:1.0.5")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
                implementation("com.github.caoimhebyrne:KDiscordIPC:0.2.2")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
    kotlin {
        jvmToolchain {
            version = "17"
        }
    }
    buildFeatures {
        compose = true
    }
}

// Version specification isn't needed, but must remain for backwards-compatibility
val DATABASE_VERSION: Int = 3

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.toasterofbread.db")
            version = DATABASE_VERSION
        }
    }
}

val fixDatabaseVersion = tasks.register("fixDatabaseVersion") {
    doLast {
        val file: File = project.file("build/generated/sqldelight/code/Database/commonMain/com/toasterofbread/db/shared/DatabaseImpl.kt")
        val lines: MutableList<String> = file.readLines().toMutableList()

        for (i in 0 until lines.size) {
            if (lines[i].endsWith("override val version: Long")) {
                lines[i + 1] = "      get() = $DATABASE_VERSION"
                break
            }
        }

        file.writer().use { writer ->
            for (line in lines) {
                writer.write(line + "\n")
            }
        }
    }
}

tasks.all {
    if (name == "generateCommonMainDatabaseInterface") {
        finalizedBy(fixDatabaseVersion)
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
        val dir: File = generated_output_dir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()

        val class_parts: List<String> = class_fq_name.get().split(".")
        val class_name: String = class_parts.last()
        val file: File = dir.resolve("$class_name.kt")

        val content: String = buildString {
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

fun cmd(vararg args: String): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine(args.toList())
        standardOutput = out
    }
    return out.toString().trim()
}

fun getCurrentGitTag(): String? {
    try {
        return cmd("git", "tag", "--points-at", "HEAD").ifBlank { null }
    }
    catch (e: Throwable) {
        return null
    }
}

fun getCurrentGitCommitHash(): String? {
    try {
        return cmd("git", "rev-parse", "HEAD").ifBlank { null }
    }
    catch (e: Throwable) {
        return null
    }
}
