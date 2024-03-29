import org.gradle.api.file.Directory
import plugin.shared.Command
import plugin.spmp.ProjectConfigValues
import java.util.Properties
import java.io.FileInputStream
import java.io.Serializable

plugins {
    id("com.github.gmazzo.buildconfig")
}

buildConfig {
    className = "ProjectBuildConfig"
    packageName = "com.toasterofbread.${project.parent!!.name}"

    useKotlinOutput {
        internalVisibility = false
    }
}

fun loadKeys(
    file: File,
    getType: (key: String) -> String,
    key_names: Collection<String>,
    include_values: Boolean = true
) {
    fun addBuildConfigField(key: String, value: String?) {
        buildConfig.buildConfigField(key) {
            type(getType(key))
            expression(value ?: null.toString())
        }
    }

    if (!file.isFile) {
        val required_keys: List<String> =
            key_names.filter { key ->
                !getType(key).endsWith("?")
            }

        if (required_keys.isNotEmpty()) {
            throw NullPointerException("No file found at ${file.path} for required keys $required_keys")
        }

        for (key in key_names) {
            addBuildConfigField(key, null)
        }

        return
    }

    val keys: Properties = Properties()
    keys.load(FileInputStream(file))

    val include: Boolean = include_values && keys["INCLUDE_KEYS"] != "false"

    for (key in key_names) {
        if (key == "INCLUDE_KEYS") {
            continue
        }

        addBuildConfigField(
            key,
            if (!include) null.toString()
            else keys[key].toString()
        )
    }
}

fun Task.buildConfig(debug_mode: Boolean) {
    loadKeys(
        rootProject.file("buildSrc/project.properties"),
        { key ->
            ProjectConfigValues.CONFIG_VALUES[key]!!
        },
        ProjectConfigValues.CONFIG_VALUES.keys,
        include_values = true
    )

    loadKeys(
        rootProject.file("buildSrc/project_debug.properties"),
        { key ->
            ProjectConfigValues.DEBUG_CONFIG_VALUES[key]!! + "?"
        },
        ProjectConfigValues.DEBUG_CONFIG_VALUES.keys,
        include_values = debug_mode
    )

    buildConfig {
        buildConfigField("GIT_COMMIT_HASH", Command.getCurrentGitCommitHash())
        buildConfigField("GIT_TAG", Command.getCurrentGitTag())
        buildConfigField("IS_DEBUG", debug_mode)
    }
}

val buildConfigDebug = tasks.register("buildConfigDebug") {
    outputs.upToDateWhen { false }
    doFirst {
        buildConfig(debug_mode = true)
    }
}
val buildConfigRelease = tasks.register("buildConfigRelease") {
    outputs.upToDateWhen { false }
    doFirst {
        buildConfig(debug_mode = false)
    }
}
val buildConfigDesktop = tasks.register("buildConfigDesktop") {
    outputs.upToDateWhen { false }
    doFirst {
        var debug: Boolean = gradle.taskGraph.getAllTasks().none { task ->
            task.name.startsWith("packageRelease") || task.name == "runRelease"
        }

        println("buildConfigDesktop will be configured with debug_mode=$debug")
        buildConfig(debug_mode = debug)
    }
}

afterEvaluate {
    tasks.all {
        when (name) {
            "compileDebugKotlinAndroid" -> dependsOn(buildConfigDebug)
            "compileReleaseKotlinAndroid" -> dependsOn(buildConfigRelease)
            "compileKotlinDesktop" -> dependsOn(buildConfigDesktop)
        }
    }
}
