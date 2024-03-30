import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.resources.getString
import spms.socketapi.shared.SPMS_API_VERSION

data class ProgramArguments(
    val bin_dir: String? = null,
    val no_auto_server: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): ProgramArguments? {
            fun onIllegalArgument(msg: String) {
                throw IllegalArgumentException("$msg Received arguments: ${args.toList()}")
            }

            var arguments: ProgramArguments = ProgramArguments()

            val iterator: Iterator<String> = args.iterator()
            while (iterator.hasNext()) {
                val split: List<String> = iterator.next().split('=', limit = 2)
                val name: String = split.first()
                val value: String? = split.getOrNull(1)

                when (name) {
                    "--bin-dir" -> {
                        if (value == null && !iterator.hasNext()) {
                            onIllegalArgument("No value passed for argument '$name'.")
                        }
                        arguments = arguments.copy(bin_dir = value ?: iterator.next())
                    }
                    "--disable-auto-server", "-ds" -> {
                        arguments = arguments.copy(no_auto_server = true)
                    }
                    "--help", "-h" -> {
                        println(getHelpMessage())
                        return null
                    }
                    "--version", "-v" -> {
                        println(getVersionMessage())
                        return null
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
            val api_version_string: String = "v$SPMS_API_VERSION"
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
