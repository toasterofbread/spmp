import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.resources.getString
import dev.toastbits.spms.socketapi.shared.SPMS_API_VERSION
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformFile
import java.io.File

data class ProgramArguments(
    val no_auto_server: Boolean = false,
    val is_flatpak: Boolean = false
) {
    companion object {
        fun parse(
            args: Array<String>,
            onIllegalArgument: (String) -> Unit
        ): ProgramArguments? {
            var arguments: ProgramArguments = ProgramArguments()

            val iterator: Iterator<String> = args.iterator()
            while (iterator.hasNext()) {
                val split: List<String> = iterator.next().split('=', limit = 2)
                val name: String = split.first()
                val value: String? = split.getOrNull(1)

                when (name) {
                    "--help", "-h" -> {
                        println(getHelpMessage())
                        return null
                    }
                    "--version", "-v" -> {
                        println(getVersionMessage())
                        return null
                    }
                    "--disable-auto-server", "-ds" -> {
                        arguments = arguments.copy(no_auto_server = true)
                    }
                    "--flatpak" -> {
                        arguments = arguments.copy(is_flatpak = true)
                    }
                    else -> onIllegalArgument("Unknown argument '$name'.")
                }
            }

            return arguments
        }

        fun getHelpMessage(): String {
            return getString("help_message") + "\n" + getVersionMessage()
        }

        fun getVersionMessage(split_lines: Boolean = false): String {
            val version_string: String = "v${getString("version_string")}"
            val api_version_string: String = SPMS_API_VERSION.toString()
            val split_string: String = if (split_lines) "\n" else ""

            val on_release_commit: Boolean = ProjectBuildConfig.GIT_TAG == version_string
            if (on_release_commit) {
                return getString("version_message_release_\$appver_\$apiver_\$split")
                    .replace("\$appver", version_string)
                    .replace("\$apiver", api_version_string)
                    .replace("\$split", split_string)
            }
            else {
                return getString("version_message_non_release_\$commit_\$apiver_\$split")
                    .replace("\$commit", ProjectBuildConfig.GIT_COMMIT_HASH?.take(7).toString())
                    .replace("\$apiver", api_version_string)
                    .replace("\$split", split_string)
            }
        }
    }
}
