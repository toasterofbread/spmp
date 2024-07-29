import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import plugin.spmp.SpMpDeps
import plugin.spmp.getDeps
import java.io.FileInputStream
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

enum class OS {
    LINUX, WINDOWS;
}

val local_properties_path: String = "local.properties"
val strings_file: File = rootProject.file("shared/src/commonMain/resources/assets/values/strings.xml")

fun getString(key: String): String {
    val reader = strings_file.reader()
    val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(reader)

    while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) {
            parser.next()
            continue
        }

        if (parser.getAttributeValue(null, "name") != key) {
            parser.next()
            continue
        }

        val ret = parser.nextText()
        reader.close()
        return ret
    }

    reader.close()
    throw NoSuchElementException(key)
}

kotlin {
    jvmToolchain(21)

    jvm()
    sourceSets {
        val deps: SpMpDeps = getDeps()

        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))

                implementation(deps.get("dev.toastbits.composekit:library-desktop"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")

                // LWJGL needed by gdx-nativefilechooser in composekit
                val os_name: String = System.getProperty("os.name")
                val lwjgl_os: String = when {
                    os_name == "Linux" -> "linux"
                    os_name.startsWith("Win") -> "windows"
                    os_name == "Mac OS X" -> "macos"
                    else -> throw Error("Unknown OS '$os_name'")
                }
                val lwjgl_version: String = "3.3.1"
                for (lwjgl_library in listOf("lwjgl", "lwjgl-nfd")) {
                    implementation("org.lwjgl:$lwjgl_library:$lwjgl_version")
                    implementation("org.lwjgl:$lwjgl_library:$lwjgl_version:natives-$lwjgl_os")
                }
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        System.getenv("PACKAGE_JAVA_HOME")?.takeIf { it.isNotBlank() }?.also {
            javaHome = it
        }

        nativeDistributions {
            modules(
                "java.sql",
                "jdk.unsupported",
                "java.management"
            )

            appResourcesRootDir.set(project.file("build/package"))

            packageName = rootProject.name
            packageVersion = getString("version_string")
            version = getString("version_string")
            licenseFile.set(rootProject.file("LICENSE"))

            targetFormats(TargetFormat.AppImage, TargetFormat.Deb, TargetFormat.Exe)

            // spms
            jvmArgs += listOf("--enable-preview", "--enable-native-access=ALL-UNNAMED")

            // Required for setting WM_CLASS in main.kt
            // https://stackoverflow.com/a/69404254
            jvmArgs += listOf("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")

            System.getenv("PACKAGE_JAVA_LIBRARY_PATH")?.takeIf { it.isNotBlank() }?.also {
                jvmArgs += listOf("-Djava.library.path=$it")
            }

            linux {
                iconFile.set(rootProject.file("metadata/en-US/images/icon.png"))
                appRelease = getString("version_code")
            }

            windows {
                iconFile.set(rootProject.file("metadata/en-US/images/icon.ico"))
                shortcut = true
                dirChooser = true

                if (getString("version_string").contains("-")) {
                    exePackageVersion = "0.0.0"
                }
            }
        }

        buildTypes.release {
            proguard {
                // TODO
                isEnabled = false
            }
        }
    }
}

abstract class ActuallyPackageAppImageTask: DefaultTask() {
    @get:Input
    abstract val appimage_arch: Property<String>

    @get:OutputFile
    val appimage_output_file: RegularFileProperty = project.objects.fileProperty()

    @get:InputDirectory
    val appimage_src_dir: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val appimage_dst_dir: DirectoryProperty = project.objects.directoryProperty()

    @get:InputFile
    val icon_src_file: RegularFileProperty = project.objects.fileProperty()

    @get:OutputFile
    val icon_dst_file: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun prepareAppImageFiles() {
        val appimage_src: File = appimage_src_dir.get().asFile
        check(appimage_src.isDirectory)

        val appimage_dst: File = appimage_dst_dir.get().asFile
        check(appimage_dst.isDirectory)

        project.logger.lifecycle("Copying source AppImage files from ${appimage_src.relativeTo(project.rootDir)} to ${appimage_dst.relativeTo(project.rootDir)}")
        appimage_src.copyRecursively(appimage_dst, true)

        prepareAppImageDirectory(appimage_dst)

        val arch: String = appimage_arch.get()
        val appimage_output: File = appimage_output_file.get().asFile

        runBlocking {
            project.logger.lifecycle("Executing appimagetool with arch $arch and output file ${appimage_output.relativeTo(project.rootDir)}")
            project.exec {
                environment("ARCH", arch)
                workingDir = appimage_dst
                executable = "appimagetool"
                args = listOf(".", appimage_output.absolutePath)
            }

            delay(100)
            project.logger.lifecycle("\nAppImage successfully packaged to ${appimage_output.absolutePath}")
        }
    }

    private fun prepareAppImageDirectory(dir: File) {
        val AppRun: File = dir.resolve("AppRun")
        if (AppRun.isFile) {
            project.logger.lifecycle("Adding execute permission to AppRun file at ${AppRun.relativeTo(project.rootDir)}")
            AppRun.addExecutePermission()
        }

        val icon_src: File = icon_src_file.get().asFile
        check(icon_src.isFile)

        project.logger.lifecycle("Copying icon at ${icon_src.relativeTo(project.rootDir)} into AppImage files")
        val icon_dst: File = icon_dst_file.get().asFile
        icon_src.copyTo(icon_dst, overwrite = true)

        project.logger.lifecycle("Removing unneeded jars")

        val deps: SpMpDeps = project.getDeps()
        val ffmpeg_version: String = deps.getVersion("org.bytedeco:ffmpeg-platform")
        val javacpp_version: String = ffmpeg_version.split('-', limit = 2)[1]

        val platforms: List<String> = JarUtil.getUnneededPlatforms(is_windows = false)
        val jar_prefixes: List<String> = platforms.flatMap { platform ->
            JarUtil.getUnneededLibraries().map { library ->
                val version: String =
                    when (library) {
                        "javacpp" -> javacpp_version
                        "ffmpeg" -> ffmpeg_version
                        else -> throw NotImplementedError(library)
                    }
                return@map "$library-$version-$platform"
            }
        }

        for (file in dir.resolve("lib/app").listFiles().orEmpty()) {
            for (prefix in jar_prefixes) {
                if (file.name.startsWith(prefix) && file.name.endsWith(".jar")) {
                    file.delete()
                    project.logger.lifecycle("Removing lib/app/${file.name}")
                }
            }
        }
    }

    private fun File.addExecutePermission() {
        if (OperatingSystem.current().isUnix) {
            val permissions: MutableSet<PosixFilePermission> = getPosixFilePermissions(toPath())
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
            setPosixFilePermissions(toPath(), permissions)
        }
    }
}

fun registerAppImagePackageTasks() {
    val package_tasks: List<Pair<String, Boolean>> = listOf(
        "packageAppImage" to false,
        "packageReleaseAppImage" to true
    )

    for ((task_name, is_release) in package_tasks) {
        val subtask_name: String =
            if (is_release) "finishPackagingReleaseAppImage"
            else "finishPackagingAppImage"

        tasks.register<ActuallyPackageAppImageTask>(subtask_name) {
            val build_dir: File = tasks.getByName(task_name).outputs.files.toList().single()

            appimage_arch = "x86_64"
            appimage_output_file = build_dir.parentFile.resolve("appimage").resolve(rootProject.name.lowercase() + "-" + getString("version_string") + ".appimage")

            appimage_src_dir = projectDir.resolve("appimage")
            appimage_dst_dir = build_dir.resolve(rootProject.name)

            icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
            icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")
        }

        tasks.getByName(task_name).finalizedBy(subtask_name)
    }
}

fun configureRunTask() {
    tasks.getByName<JavaExec>("run") {
        val local_properties: Properties = Properties().apply {
            try {
                val file: File = rootProject.file(local_properties_path)
                if (file.isFile) {
                    load(FileInputStream(file))
                }
            }
            catch (e: Throwable) {
                RuntimeException("Ignoring exception while loading '$local_properties_path' in configureRunTask()", e).printStackTrace()
            }
        }
        executable = local_properties["execTaskJavaExe"]?.toString() ?: return@getByName
    }
}

afterEvaluate {
    registerAppImagePackageTasks()
    configureRunTask()

    tasks.named<Jar>("packageUberJarForCurrentOS") {
        isZip64 = true
    }

    tasks.named<Jar>("packageReleaseUberJarForCurrentOS") {
        isZip64 = true
    }
}

private object JarUtil {
    fun getUnneededLibraries(): List<String> =
        listOf("javacpp", "ffmpeg")

    fun getUnneededPlatforms(is_windows: Boolean): List<String> =
        listOf(
            if (is_windows) "linux-x86_64"
            else "windows-x86_64",
            "linux-arm64",
            "linux-ppc64le",
            "android-arm64",
            "android-x86_64",
            "macosx-arm64",
            "macosx-x86_64",
            "ios-arm64",
            "ios-x86_64"
        )

    fun Jar.excludeUnneededFiles() {
        val is_windows: Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
        val platforms: List<String> = JarUtil.getUnneededPlatforms(is_windows = is_windows)
        val libraries: List<String> = listOf("javacpp", "ffmpeg")

        exclude(platforms.flatMap { platform -> libraries.map { library -> "/org/bytedeco/$library/$platform/*" } })

        val unneeded_sqlite_architectures: List<String> =
            listOf(
                "arm",
                "armv6",
                "armv7",
                "aarch64",
                "x86",
                "ppc64"
            )
        val unneeded_sqlite_platforms: List<String> =
            listOf(
                if (is_windows) "Linux"
                else "Windows",
                "Linux-Android",
                "Linux-Musl",
                "FreeBSD",
                "Mac"
            )

        exclude(
            unneeded_sqlite_architectures.flatMap { arch ->
                unneeded_sqlite_platforms.map { platform ->
                    "/org/sqlite/native/$platform/$arch/*"
                }
            }
        )
    }
}

afterEvaluate {
    tasks.named<Jar>("packageUberJarForCurrentOS") {
        with (JarUtil) { excludeUnneededFiles() }
    }
    tasks.named<Jar>("packageReleaseUberJarForCurrentOS") {
        with (JarUtil) { excludeUnneededFiles() }
    }
}
