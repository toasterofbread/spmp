import java.io.File
import plugin.spmp.SpMpDeps

val OUTPUT_PACKAGE_NAME: String = "com.toasterofbread.spmp"
val INPUT_PACKAGE_NAME: String = "plugin.spmp"

plugins {
    id("com.github.gmazzo.buildconfig")
}

fun Task.generateDependencyList() {
    val output_directory: File = project.layout.buildDirectory.dir("generated/sources/buildConfig/main/com/toasterofbread/spmp").get().getAsFile()
    output_directory.mkdirs()

    val dependency_info_file_content: String =
        rootProject.file("buildSrc/src/main/kotlin/plugins/spmp/DependencyInfo.kt").readText()
            .replace("package $INPUT_PACKAGE_NAME", "package $OUTPUT_PACKAGE_NAME")

    output_directory.resolve("DependencyInfo.kt").writeText(dependency_info_file_content)

    val dependencies_string: StringBuilder = StringBuilder()

    for ((key, dep) in SpMpDeps(project.extra.properties).dependencies) {
        dependencies_string.append(
"""
        "$key" to DependencyInfo(
            version = ${dep.version?.let { "\"$it\"" }},
            name = "${dep.name}",
            author = "${dep.author}",
            url = "${dep.url}",
            license = "${dep.license}",
            license_url = "${dep.license_url}",
            fork_url = ${dep.fork_url?.let { "\"$it\"" }}
        ),"""
        )
    }

    val dependencies_file_content: String =
"""package $OUTPUT_PACKAGE_NAME

object SpMpDeps {
    val dependencies: Map<String, DependencyInfo> = mapOf($dependencies_string
    )
}
"""

    output_directory.resolve("Dependencies.kt").writeText(dependencies_file_content)
}

val generateDependencyList by tasks.registering {
    doFirst {
        generateDependencyList()
    }
}

afterEvaluate {
    tasks.all {
        when (name) {
            "compileDebugKotlinAndroid" -> dependsOn(generateDependencyList)
            "compileReleaseKotlinAndroid" -> dependsOn(generateDependencyList)
            "compileKotlinDesktop" -> dependsOn(generateDependencyList)
        }
    }
}
