@file:Suppress("UNUSED_VARIABLE")

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

enum class OS {
    LINUX, WINDOWS;
}

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
            packageName = getString("app_name")
            version = getString("version_string")
            packageVersion = getString("version_string")
            licenseFile.set(rootProject.file("LICENSE"))

            targetFormats(TargetFormat.AppImage, TargetFormat.Deb, TargetFormat.Exe)
            includeAllModules = true

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

abstract class DownloadPlatformServerTask: DefaultTask() {
    @get:Input
    abstract val target_os: Property<OS>

    @get:InputFile
    val server_properties_file: RegularFileProperty = project.objects.fileProperty()
    
    @get:OutputDirectory
    val dst_dir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun downloadPlatformServer() {
        val properties: Properties = Properties()
        properties.load(FileInputStream(server_properties_file.get().asFile))

        val download_url: String =
            when (target_os.get()) {
                OS.LINUX -> properties["SPMS_TARGET_URL_LINUX"]
                OS.WINDOWS ->properties["SPMS_TARGET_URL_WINDOWS"]
            }

        val filename: String = 
            when (target_os.get()) {
                OS.LINUX -> "spms.kexe"
                OS.WINDOWS -> "spms.exe"
            }

        val destination: File = dst_dir.get().asFile.resolve(filename)
        FileUtils.copyURLToFile(URL(download_url), destination)
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

        val AppRun: File = appimage_dst.resolve("AppRun")
        if (AppRun.isFile) {
            project.logger.lifecycle("Adding 'execute' permission to AppRun file at ${AppRun.relativeTo(project.rootDir)}")

            val permissions: MutableSet<PosixFilePermission> = getPosixFilePermissions(AppRun.toPath())
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
            setPosixFilePermissions(AppRun.toPath(), permissions)
        }

        val icon_src: File = icon_src_file.get().asFile
        check(icon_src.isFile)

        project.logger.lifecycle("Copying icon at ${icon_src.relativeTo(project.rootDir)} into AppImage files")
        val icon_dst: File = icon_dst_file.get().asFile
        icon_src.copyTo(icon_dst, overwrite = true)

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

tasks.register<ActuallyPackageAppImageTask>("actuallyPackageAppImage") {
    val package_task: Task = getTasksByName("packageReleaseAppImage", false).first()
    dependsOn(package_task)
    group = package_task.group

    appimage_arch = "x86_64"
    appimage_output_file = buildDir.resolve(rootProject.name.lowercase() + "-" + getString("version_string") + ".appimage")

    appimage_src_dir = projectDir.resolve("appimage")
    appimage_dst_dir = package_task.outputs.files.toList().single().resolve(rootProject.name)

    icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
    icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")
}
