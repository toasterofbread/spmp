@file:Suppress("UNUSED_VARIABLE")

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import kotlin.NoSuchElementException
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import plugin.shared.CommandClass
import plugin.spmp.SpMpDeps

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
    jvm()
    sourceSets {
        val deps: SpMpDeps = SpMpDeps(extra.properties)

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

            linux {
                iconFile.set(rootProject.file("metadata/en-US/images/icon.png"))
                appRelease = getString("version_code")

                // Required for setting WM_CLASS in main.kt
                // https://stackoverflow.com/a/69404254
                jvmArgs += listOf("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
            }

            windows {
                iconFile.set(rootProject.file("metadata/en-US/images/icon.ico"))
                shortcut = true
                dirChooser = true
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

abstract class PackageTask: DefaultTask() {
    private data class Configuration(
        val target_name: String,
        val target_os: OS,
        val server_build_task: Task,
        val package_server: Boolean
    )

    private var configured: Boolean = false
    private lateinit var configuration: Configuration

    companion object {
        const val FLAG_PACKAGE_SERVER: String = "packageServer"

        fun getResourcesDir(project: Project, os: OS): File =
            when (os) {
                OS.LINUX -> project.file("build/package/linux")
                OS.WINDOWS -> project.file("build/package/windows")
            }
    }

    init {
        outputs.upToDateWhen { false }
    }

    fun configure(package_server: Boolean, spms_os: OS, spms_arch: String, is_release: Boolean) {
        val server_project = project.rootProject.project("spmp-server")
        server_project.ext.set("linkStatic", 1)

        val target_name: String =
            spms_os.name.lowercase() + '-' + spms_arch

        val build_type: String =
            if (is_release) "Release"
            else "Debug"
        val task_name: String =
            "link${build_type}Executable${target_name.replaceFirstChar { it.uppercase() }}"

        configuration = Configuration(
            target_name = target_name,
            target_os = spms_os,
            server_build_task = server_project.tasks.getByName(task_name),
            package_server = package_server
        )

        if (package_server) {
            dependsOn(configuration.server_build_task)
        }

        configured = true
    }

    fun File.addExecutePermission() {
        if (OperatingSystem.current().isUnix()) {
            val permissions: MutableSet<PosixFilePermission> = getPosixFilePermissions(toPath())
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
            setPosixFilePermissions(toPath(), permissions)
        }
    }

    fun getPlatformServerFileExtension(target_os: OS): String =
        when (target_os) {
            OS.LINUX -> "kexe"
            OS.WINDOWS -> "exe"
        }

    fun getPlatformServerFilename(target_os: OS): String =
        "spms." + getPlatformServerFileExtension(target_os)

    fun buildPlatformServer(dst_dir: File) {
        check(configured) { "PackageTask was not configured" }

        if (!configuration.package_server) {
            val server_file: File = dst_dir.resolve(getPlatformServerFilename(OS.LINUX))
            if (server_file.isFile) {
                server_file.delete()
            }
            return
        }

        val output_directory: File = configuration.server_build_task.outputs.files.files.single()
        val executable_file: File = output_directory.resolve("spms-${configuration.target_name}." + getPlatformServerFileExtension(configuration.target_os))

        for (file in output_directory.listFiles()) {
            if (file == executable_file || !file.name.endsWith(".dll")) {
                continue
            }

            file.copyTo(dst_dir.resolve(file.name), overwrite = true)
        }

        check(executable_file.isFile) {
            "Server executable $executable_file does not exist ($configuration)"
        }

        try {
            println("Attempting to strip server executable ${executable_file.absolutePath}...")
            CommandClass(project).cmd("strip", executable_file.absolutePath)
        }
        catch (e: Throwable) {
            RuntimeException("Strip failed", e).printStackTrace()
        }

        val output_file: File = dst_dir.resolve(getPlatformServerFilename(configuration.target_os))
        try {
            executable_file.copyTo(output_file, overwrite = true)
        }
        catch (e: Throwable) {
            throw RuntimeException("Copying $executable_file to $output_file failed", e)
        }

        output_file.addExecutePermission()
    }

    fun extractZip(file: File, output_dir: File) {
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entry_destination = File(output_dir, entry.name)

                if (entry.isDirectory) {
                    entry_destination.mkdirs()
                    continue
                }

                entry_destination.parentFile.mkdirs()

                zip.getInputStream(entry).use { input ->
                    FileOutputStream(entry_destination).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}

abstract class ActuallyPackageAppImageTask: PackageTask() {
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

        val AppRun: File = appimage_dst.resolve("AppRun")
        if (AppRun.isFile) {
            project.logger.lifecycle("Adding execute permission to AppRun file at ${AppRun.relativeTo(project.rootDir)}")
            AppRun.addExecutePermission()
        }

        val icon_src: File = icon_src_file.get().asFile
        check(icon_src.isFile)

        project.logger.lifecycle("Copying icon at ${icon_src.relativeTo(project.rootDir)} into AppImage files")
        val icon_dst: File = icon_dst_file.get().asFile
        icon_src.copyTo(icon_dst, overwrite = true)

        val server_dst_dir: File = appimage_dst.resolve("bin")
        buildPlatformServer(server_dst_dir)

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
}

fun registerAppImagePackageTasks() {
    val package_tasks: List<Pair<String, Boolean>> = listOf(
        "packageAppImage" to false,
        "packageReleaseAppImage" to true
    )

    for ((task_name, is_release) in package_tasks) {
        val with_server_task =
            tasks.register(task_name + "WithServer") {
                finalizedBy(task_name)
                group = tasks.getByName(task_name).group
            }

        for (with_server in listOf(false, true)) {
            val suffix: String =
                if (with_server) "WithServer"
                else ""

            val subtask_name: String =
                if (is_release) "finishPackagingReleaseAppImage" + suffix
                else "finishPackagingAppImage" + suffix

            tasks.register<ActuallyPackageAppImageTask>(subtask_name) {
                val build_dir: File = tasks.getByName(task_name).outputs.files.toList().single()

                val arch: String = "x86_64"
                configure(with_server, OS.LINUX, arch, is_release)

                appimage_arch = arch
                appimage_output_file = build_dir.parentFile.resolve("appimage").resolve(rootProject.name.lowercase() + "-" + getString("version_string") + ".appimage")

                appimage_src_dir = projectDir.resolve("appimage")
                appimage_dst_dir = build_dir.resolve(rootProject.name)

                icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
                icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")

                onlyIf {
                    with_server || !gradle.taskGraph.hasTask(":" + project.name + ":" + with_server_task.name)
                }
            }

            tasks.getByName(task_name + suffix).finalizedBy(subtask_name)
        }
    }
}

fun registerExePackageTasks() {
    val package_tasks: List<Pair<Task, Boolean>> = listOf(
        tasks.getByName("packageExe") to false,
        tasks.getByName("packageReleaseExe") to true
    )

    for ((task, is_release) in package_tasks) {
        tasks.register<PackageTask>(task.name + "WithServer") {
            finalizedBy("packageReleaseExe")
            group = task.group
            configure(true, OS.WINDOWS, "x86_64", is_release)

            doFirst {
                buildPlatformServer(PackageTask.getResourcesDir(project, OS.WINDOWS))
            }
        }
    }
}

fun configureRunTask() {
    tasks.getByName<JavaExec>("run") {
        val local_properties: Properties = Properties().apply {
            try {
                load(FileInputStream(rootProject.file(local_properties_path)))
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
    registerExePackageTasks()
    configureRunTask()
}
