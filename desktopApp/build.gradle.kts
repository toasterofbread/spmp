@file:Suppress("UNUSED_VARIABLE")

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

enum class OS {
    LINUX, WINDOWS;
}

val server_properties_file: File = rootProject.file("server.properties")
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
        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
                implementation(project(":ComposeKit:lib"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
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

            packageName = getString("app_name")
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
    companion object {
        const val FLAG_PACKAGE_SERVER: String = "packageServer"

        fun getResourcesDir(project: Project, os: OS): File =
            when (os) {
                OS.LINUX -> project.file("build/package/linux")
                OS.WINDOWS -> project.file("build/package/windows")
            }
    }

    fun File.addExecutePermission() {
        val permissions: MutableSet<PosixFilePermission> = getPosixFilePermissions(toPath())
        permissions.add(PosixFilePermission.OWNER_EXECUTE)
        setPosixFilePermissions(toPath(), permissions)
    }

    fun getPlatformServerFilename(target_os: OS): String =
        when (target_os) {
            OS.LINUX -> "spms.kexe"
            OS.WINDOWS -> "spms.exe"
        }

    fun downloadPlatformServer(target_os: OS, server_properties_file: File, dst_dir: File) {
        val properties: Properties = Properties()
        properties.load(FileInputStream(server_properties_file))

        val download_url: String? =
            when (target_os) {
                OS.LINUX -> properties["SPMS_TARGET_URL_LINUX"]?.toString()
                OS.WINDOWS -> properties["SPMS_TARGET_URL_WINDOWS"]?.toString()
            }

        if (download_url == null) {
            throw RuntimeException("No SpMs target URL for $target_os in ${server_properties_file.absolutePath}")
        }

        project.logger.lifecycle("Downloading server binary at $download_url")

        val server_executable_filename: String = getPlatformServerFilename(target_os)

        val destination: File = dst_dir.resolve(download_url.split('/').last())
        FileUtils.copyURLToFile(URL(download_url), destination)

        if (destination.name.endsWith(".zip")) {
            extractZip(destination, destination.parentFile)
            destination.delete()
        }

        val target_extension: String = "." + server_executable_filename.split('.').last()

        for (file in destination.parentFile.listFiles().orEmpty()) {
            if (file.name.endsWith(target_extension)) {
                val target_file: File = destination.parentFile.resolve(server_executable_filename)
                file.renameTo(target_file)

                if (target_os == OS.LINUX) {
                    project.logger.lifecycle("Adding execute permission to server binary at ${target_file.absolutePath}")
                    target_file.addExecutePermission()
                }

                break
            }
        }
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
    @Optional
    @get:InputFile
    val server_props_file: RegularFileProperty = project.objects.fileProperty()

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

    init {
        outputs.upToDateWhen { false }
    }

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
        if (project.hasProperty(FLAG_PACKAGE_SERVER)) {
            downloadPlatformServer(OS.LINUX, server_props_file.get().asFile, server_dst_dir)
        }
        else {
            val server_file: File = server_dst_dir.resolve(getPlatformServerFilename(OS.LINUX))
            if (server_file.isFile) {
                server_file.delete()
            }
        }

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
    val package_tasks = listOf(
        tasks.getByName("packageAppImage") to "finishPackagingAppImage",
        tasks.getByName("packageReleaseAppImage") to "finishPackagingReleaseAppImage"
    )

    for ((task, subtask_name) in package_tasks) {
        tasks.register<ActuallyPackageAppImageTask>(subtask_name) {
            val build_dir: File = task.outputs.files.toList().single()

            appimage_arch = "x86_64"
            appimage_output_file = build_dir.parentFile.resolve("appimage").resolve(rootProject.name.lowercase() + "-" + getString("version_string") + ".appimage")

            appimage_src_dir = projectDir.resolve("appimage")
            appimage_dst_dir = build_dir.resolve(rootProject.name)

            icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
            icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")

            server_props_file = server_properties_file
        }

        task.finalizedBy(subtask_name)
    }

    tasks.register("packageReleaseAppImageWithServer") {
        finalizedBy("packageReleaseAppImage")
        group = tasks.getByName("packageReleaseAppImage").group

        doFirst {
            project.ext.set(PackageTask.FLAG_PACKAGE_SERVER, 1)
        }
    }
}

fun registerExePackageTasks() {
    tasks.register<PackageTask>("packageReleaseExeWithServer") {
        finalizedBy("packageReleaseExe")
        group = tasks.getByName("packageReleaseExe").group

        doFirst {
            downloadPlatformServer(
                OS.WINDOWS,
                server_properties_file,
                PackageTask.getResourcesDir(project, OS.WINDOWS)
            )
        }
    }
}

afterEvaluate {
    registerAppImagePackageTasks()
    registerExePackageTasks()
}
