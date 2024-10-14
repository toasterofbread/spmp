package plugins.shared

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import plugin.spmp.SpMpDeps
import plugin.spmp.getDeps
import java.io.File
import java.io.InputStreamReader

object DesktopUtils {
    fun runChecks(project: Project) {
        if (System.getenv("NIX_PATH") != null && !project.hasProperty("BUILD_ON_NIX")) {
            throw GradleException("It appears you're trying to build in a Nix environment. Desktop binaries built on Nix do not function on other systems. To build anyway, run Gradle with '-PBUILD_ON_NIX'.")
        }
    }

    fun getOutputDir(project: Project): File =
        project.layout.buildDirectory.asFile.get().resolve("outputs")

    fun getOutputFilename(project: Project): String {
        val platform: String =
            if (Os.isFamily(Os.FAMILY_WINDOWS)) "windows-x86_64"
            else "linux-x86_64"

        return project.rootProject.name.lowercase() + "-v" + project.getString("version_string") + "-$platform"
    }

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

    const val LOCAL_PROPERTIES_PATH: String = "local.properties"
    val Project.strings_file: File get() = rootProject.file("shared/src/commonMain/composeResources/values/strings.xml")

    fun Project.getString(key: String): String {
        val reader: InputStreamReader = strings_file.reader()
        val parser: XmlPullParser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(reader)

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                parser.next()
                continue
            }

            if (parser.getAttributeValue(null, "name") != key) {
                parser.next()
                continue
            }

            val ret: String = parser.nextText()
            reader.close()
            return ret
        }

        reader.close()
        throw NoSuchElementException(key)
    }

    fun Jar.excludeUnneededFiles() {
        val is_windows: Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
        val platforms: List<String> = DesktopUtils.getUnneededPlatforms(is_windows = is_windows)
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

    fun File.removeUnneededJarsFromDir(
        project: Project,
        is_windows: Boolean,
        deps: SpMpDeps = project.getDeps()
    ) {
        check(isDirectory) { "Directory passed to removeUnneededJarsFromDir does not exist ($absolutePath)" }

        val ffmpeg_version: String = deps.getVersion("org.bytedeco:ffmpeg-platform")
        val javacpp_version: String = ffmpeg_version.split('-', limit = 2)[1]

        val platforms: List<String> = DesktopUtils.getUnneededPlatforms(is_windows = is_windows)
        val jar_prefixes: List<String> = platforms.flatMap { platform ->
            DesktopUtils.getUnneededLibraries().map { library ->
                val version: String =
                    when (library) {
                        "javacpp" -> javacpp_version
                        "ffmpeg" -> ffmpeg_version
                        else -> throw NotImplementedError(library)
                    }
                return@map "$library-$version-$platform"
            }
        }

        for (file in this.listFiles().orEmpty()) {
            for (prefix in jar_prefixes) {
                if (file.name.startsWith(prefix) && file.name.endsWith(".jar")) {
                    file.delete()
                    project.logger.lifecycle("Removing lib/app/${file.name}")
                }
            }
        }
    }
}
