package plugin.shared

import org.gradle.api.Project
import java.io.ByteArrayOutputStream

val Project.Command: CommandClass get() = CommandClass(this)

class CommandClass(project: Project): Project by project {
    fun cmd(vararg args: String): String {
        val out = ByteArrayOutputStream()
        exec {
            commandLine(args.toList())
            standardOutput = out
        }
        return out.toString().trim()
    }

    fun getCurrentGitTag(): String? {
        try {
            val tags: List<String> = cmd("git", "tag", "--points-at", "HEAD").split('\n')
            return tags.lastOrNull()
        }
        catch (e: Throwable) {
            return null
        }
    }

    fun getCurrentGitCommitHash(): String? {
        try {
            return cmd("git", "rev-parse", "HEAD").ifBlank { null }
        }
        catch (e: Throwable) {
            return null
        }
    }
}
