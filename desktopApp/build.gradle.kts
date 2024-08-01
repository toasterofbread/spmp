import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import plugin.spmp.SpMpDeps
import plugin.spmp.getDeps
import plugins.shared.DesktopUtils
import java.io.FileInputStream
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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

        nativeDistributions { with (DesktopUtils) {
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
        } }

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
            val runtime_file_path: String = project.file("appimage-runtime/runtime-x86_64").absolutePath
            project.logger.lifecycle("Executing appimagetool with arch $arch, output ${appimage_output.relativeTo(project.rootDir)}, and runtime $runtime_file_path")
            project.exec {
                environment("ARCH", arch)
                workingDir = appimage_dst
                executable = "appimagetool"
                args = listOf(
                    "--verbose",
                    "--no-appstream",
                    "--runtime-file", runtime_file_path,
                    ".", appimage_output.absolutePath
                )
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

        with (DesktopUtils) {
            dir.resolve("lib/app").removeUnneededJarsFromDir(project)
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
            appimage_output_file = DesktopUtils.getOutputDir(project).resolve(DesktopUtils.getOutputFilename(project) + ".appimage")

            appimage_src_dir = projectDir.resolve("appimage")
            appimage_dst_dir = build_dir.resolve(rootProject.name)

            icon_src_file = rootDir.resolve("metadata/en-US/images/icon.png")
            icon_dst_file = appimage_dst_dir.get().asFile.resolve("${rootProject.name}.png")
        }

        tasks.getByName(task_name).finalizedBy(subtask_name)
    }
}

fun configureRunTask() = with (DesktopUtils) {
    tasks.getByName<JavaExec>("run") {
        val local_properties: Properties = Properties().apply {
            try {
                val file: File = rootProject.file(LOCAL_PROPERTIES_PATH)
                if (file.isFile) {
                    load(FileInputStream(file))
                }
            }
            catch (e: Throwable) {
                RuntimeException("Ignoring exception while loading '$LOCAL_PROPERTIES_PATH' in configureRunTask()", e).printStackTrace()
            }
        }
        executable(local_properties["execTaskJavaExe"]?.toString() ?: return@getByName)
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

afterEvaluate {
    tasks.named<Jar>("packageUberJarForCurrentOS") {
        destinationDirectory = DesktopUtils.getOutputDir(project)
        archiveFileName = DesktopUtils.getOutputFilename(project) + "-debug.jar"
        doFirst {
            DesktopUtils.runChecks(project)
        }
        with (DesktopUtils) { excludeUnneededFiles() }
    }
    tasks.named<Jar>("packageReleaseUberJarForCurrentOS") {
        destinationDirectory = DesktopUtils.getOutputDir(project)
        archiveFileName = DesktopUtils.getOutputFilename(project) + ".jar"
        doFirst {
            DesktopUtils.runChecks(project)
        }
        with (DesktopUtils) { excludeUnneededFiles() }
    }

    tasks.withType<AbstractJPackageTask> {
        doFirst {
            DesktopUtils.runChecks(project)
        }

        doLast {
            val jars_directory: File = outputs.files.singleFile.resolve("spmp/lib/app")
            with (DesktopUtils) {
                jars_directory.removeUnneededJarsFromDir(project)
            }
        }
    }
}

tasks.register<Tar>("packageReleaseTarball") {
    val dist_task: Task by tasks.named("createReleaseDistributable")
    dependsOn(dist_task)
    mustRunAfter("finishPackagingReleaseAppImage")

    into("/") {
        from(dist_task.outputs.files.singleFile)
    }

    destinationDirectory = DesktopUtils.getOutputDir(project)
    archiveFileName = DesktopUtils.getOutputFilename(project) + ".tar.gz"
    compression = Compression.GZIP
}
