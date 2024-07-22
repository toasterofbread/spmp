import com.github.gmazzo.buildconfig.BuildConfigClassSpec
import plugin.shared.Command
import plugin.spmp.ProjectConfigValues
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.github.gmazzo.buildconfig")
}

val release_source_sets: List<String> =
    listOf(
        "androidRelease",
        "desktopMain",
        "wasmJsMain"
    )

val debug_source_sets: List<String> =
    listOf(
        "androidDebug"
    )

buildConfig {
    className = "ProjectBuildConfig"
    packageName = "com.toasterofbread.${project.parent!!.name}"

    useKotlinOutput {
        internalVisibility = false
    }

    // TODO
    buildConfig(debug_mode = false)

    // sourceSets.all {
    //     for ((source_set, debug) in release_source_sets.map { it to false } + debug_source_sets.map { it to true }) {
    //         if (name == source_set) {
    //         }
    //     }
    // }
}

private fun BuildConfigClassSpec.loadKeys(
    file: File,
    getType: (key: String) -> String,
    key_names: Collection<String>,
    include_values: Boolean = true
) {
    fun addBuildConfigField(key: String, value: String?) {
        buildConfigField(key) {
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

private fun BuildConfigClassSpec.buildConfig(debug_mode: Boolean) {
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

    buildConfigField("GIT_COMMIT_HASH", Command.getCurrentGitCommitHash())
    buildConfigField("GIT_TAG", Command.getCurrentGitTag())
    buildConfigField("IS_DEBUG", debug_mode)
}
