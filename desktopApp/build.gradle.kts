@file:Suppress("UNUSED_VARIABLE")

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission

val strings_file: File = rootProject.file("shared/src/commonMain/resources/assets/values/strings.xml")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

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

            targetFormats(TargetFormat.AppImage, TargetFormat.Deb, TargetFormat.Rpm)
            includeAllModules = true

            linux {
                iconFile.set(rootProject.file("metadata/en-US/images/icon.png"))
            }
        }
    }
}

abstract class ActuallyPackageAppImage: DefaultTask() {
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

    enum class CompressionMethod {
        GZIP, XZ;

        val arg_value: String get() = name.lowercase()
    }
    @get:Input
    abstract val compression_method: Property<CompressionMethod>

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
        val comp_method: String = compression_method.get().arg_value
        val appimage_output: File = appimage_output_file.get().asFile

        runBlocking {
            project.logger.lifecycle("Executing appimagetool with arch $arch, compression method $comp_method, and output file ${appimage_output.relativeTo(project.rootDir)}")
            project.exec {
                environment("ARCH", arch)
                workingDir = appimage_dst
                executable = "appimagetool"
                args = listOf("--comp", comp_method, ".", appimage_output.absolutePath)
            }

            delay(100)
            project.logger.lifecycle("\nAppImage successfully packaged to ${appimage_output.absolutePath}")
        }
    }
}

tasks.register<ActuallyPackageAppImage>("actuallyPackageAppImage") {
    val package_task: Task = getTasksByName("packageAppImage", false).first()
    dependsOn(package_task)
    group = package_task.group

    appimage_arch = "x86_64"
    appimage_output_file = buildDir.resolve(rootProject.name.lowercase() + "-" + getString("version_string") + ".appimage")

    appimage_src_dir = projectDir.resolve("appimage")
    appimage_dst_dir = package_task.outputs.files.toList().single().resolve(rootProject.name)

    icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
    icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")

    compression_method = ActuallyPackageAppImage.CompressionMethod.GZIP
}
