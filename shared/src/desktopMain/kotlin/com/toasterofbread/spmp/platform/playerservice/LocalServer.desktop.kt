package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformFile
import dev.toastbits.composekit.platform.Platform
import java.io.BufferedReader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import ProgramArguments

actual object LocalServer {
    actual fun canStartLocalServer(): Boolean =
        Platform.DESKTOP.isCurrent()

    actual fun startLocalServer(
        context: AppContext,
        launch_arguments: ProgramArguments?,
        port: Int,
        onExit: (Int, String) -> Unit
    ): LocalServerProcess? {
        var command: String = context.settings.platform.SERVER_LOCAL_COMMAND.get()
        if (command.isBlank()) {
            val executable_path: String = getServerExecutableFile(launch_arguments)?.absolutePath ?: return null
            command =
                when (hostOs) {
                    OS.Windows -> "\"" + executable_path + "\""
                    else -> executable_path.replace(" ", "\\ ")
                }
        }

        val args: String = "--port $port --no-media-session"

        val args_index: Int = command.indexOf("\$@")
        if (args_index != -1) {
            command = command.substring(0, args_index) + args + command.substring(args_index + 2)
        }
        else {
            command += ' ' + args
        }

        val builder: ProcessBuilder = ProcessBuilder(command.split(' '))
        builder.inheritIO()

        val process: Process = builder.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            if (context.settings.platform.SERVER_KILL_CHILD_ON_EXIT.get()) {
                process.destroy()
            }
        })

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val reader: BufferedReader = process.getErrorStream().bufferedReader()
                val output: StringBuilder = StringBuilder()

                while (true) {
                    val line: String = reader.readLine() ?: break
                    output.appendLine(line)
                }

                val result: Int = process.waitFor()
                onExit(result, output.toString())
            }
        }

        return LocalServerProcess(command, process)
    }

    fun getServerExecutableFile(launch_arguments: ProgramArguments?): File? {
        val filename: String = getServerExecutableFilename() ?: return null

        for (directory in listOfNotNull(launch_arguments?.bin_dir, System.getProperty("compose.application.resources.dir"))) {
            val file: File = File(directory).resolve(filename)
            if (file.isFile) {
                return file
            }
        }

        return null
    }
}
