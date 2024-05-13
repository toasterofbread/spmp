package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.*
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState
import java.io.File
import java.util.concurrent.TimeoutException
import io.ktor.client.request.get
import dev.toastbits.composekit.platform.PlatformFile
import ProgramArguments

private class PlayerServiceBinder(val service: PlatformInternalPlayerService): PlatformBinder()

// actual class PlatformInternalPlayerService: SpMsPlayerService(), PlayerService {
actual class PlatformInternalPlayerService: DesktopExternalPlayerService() {
//     actual override val load_state: PlayerServiceLoadState get() = socket_load_state
//     actual override val connection_error: Throwable? get() = socket_connection_error
//     actual override val context: AppContext get() = super.context
//     private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

//     private lateinit var _service_player: PlayerServicePlayer
//     actual override val service_player: PlayerServicePlayer
//         get() = _service_player

//     actual override val state: SpMsPlayerState
//         get() = _state
//     actual override val is_playing: Boolean
//         get() = _is_playing
//     actual override val song_count: Int
//         get() = playlist.size
//     actual override val current_song_index: Int
//         get() = _current_song_index
//     actual override val current_position_ms: Long
//         get() {
//             if (current_song_time < 0) {
//                 return 0
//             }
//             if (!_is_playing) {
//                 return current_song_time
//             }
//             return System.currentTimeMillis() - current_song_time
//         }
//     actual override val duration_ms: Long
//         get() = _duration_ms
//     actual override val has_focus: Boolean
//         get() = true // TODO
//     actual override val radio_instance: RadioInstance
//         get() = service_player.radio_instance
//     actual override var repeat_mode: SpMsPlayerRepeatMode
//         get() = _repeat_mode
//         set(value) {
//             if (value == _repeat_mode) {
//                 return
//             }
//             sendRequest("setRepeatMode", value.ordinal)
//         }
//     actual override var volume: Float
//         get() = _volume
//         set(value) {
//             if (value == _volume) {
//                 return
//             }
//             sendRequest("setVolume", value)
//         }

//     actual override fun isPlayingOverLatentDevice(): Boolean = false // TODO

//     actual override fun play() {
//         sendRequest("play")
//     }

//     actual override fun pause() {
//         sendRequest("pause")
//     }

//     actual override fun playPause() {
//         sendRequest("playPause")
//     }

//     actual override fun seekTo(position_ms: Long) {
//         sendRequest("seekToTime", position_ms)
//     }

//     actual override fun seekToSong(index: Int) {
//         sendRequest("seekToItem", index)
//     }

//     actual override fun seekToNext() {
//         sendRequest("seekToNext")
//     }

//     actual override fun seekToPrevious() {
//         sendRequest("seekToPrevious")
//     }

//     actual override fun getSong(): Song? = playlist.getOrNull(_current_song_index)

//     actual override fun getSong(index: Int): Song? = playlist.getOrNull(index)

//     actual override fun addSong(song: Song, index: Int) {
//         sendRequest("addItem", song.id, index)
//     }

//     actual override fun moveSong(from: Int, to: Int) {
//         sendRequest("moveItem", from, to)
//     }

//     actual override fun removeSong(index: Int) {
//         sendRequest("removeItem", index)
//     }

//     @Composable
//     actual override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
//     }

//     override fun onBind(): PlatformBinder? {
//         return PlayerServiceBinder(this)
//     }

//     actual override fun onCreate() {
//         super.onCreate()

//         check(isAvailable(context))

//         _service_player = object : PlayerServicePlayer(this) {
//             override fun onUndoStateChanged() {
//                 for (listener in listeners) {
//                     listener.onUndoStateChanged()
//                 }
//             }
//         }

//         coroutine_scope.launch {
//             val spms_port: Int = context.settings.platform.SERVER_PORT.get()

//             try {
//                 try {
//                     connectToServer("127.0.0.1", spms_port, timeout = 1000)
//                     return@launch
//                 }
//                 catch (_: TimeoutException) {
//                     println("Timed out waiting for connection to local server, starting internal server...")
//                 }
//                 catch (_: Throwable) {
//                     println("Exception caught while waiting for connection to local server, starting internal server...")
//                 }

//                 startLocalServer(spms_port, context.launch_arguments.getServerExecutableFile(context)!!) {
//                     throw RuntimeException(getString("error_on_server_command_execution") + " ($it)")
//                 }

//                 disconnectFromServer()
//                 connectToServer("127.0.0.1", spms_port)
//             }
//             catch (e: Throwable) {
//                 socket_load_state = PlayerServiceLoadState(
//                     false,
//                     error = RuntimeException(getString("error_while_connecting_to_server_at_\$x").replace("\$x", "127.0.0.1:$spms_port"), e)
//                 )
//             }
//         }
//     }

//     actual override fun onDestroy() {
//         super.onDestroy()
//         coroutine_scope.cancel()
//     }

    actual companion object: PlayerServiceCompanion {
        actual fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean =
            launch_arguments.getServerExecutableFile(context) != null

        override fun isServiceRunning(context: AppContext): Boolean = true

        override fun connect(
            context: AppContext,
            instance: PlayerService?,
            onConnected: (PlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            return startPlatformService(
                context,
                PlatformInternalPlayerService::class.java,
                onConnected = { binder ->
                    onConnected((binder as PlayerServiceBinder).service)
                },
                onDisconnected = onDisconnected
            )
        }

        override fun disconnect(context: AppContext, connection: Any) {
            unbindPlatformService(context, connection)
        }
    }
}

private data class LocalServerProcess(
    val launch_command: String,
    val process: Process
)

@Suppress("NewApi")
private fun startLocalServer(
    context: AppContext,
    port: Int,
    server_executable: PlatformFile?,
    onExit: (Int) -> Unit,
): LocalServerProcess? {
    val executable_path: String

    if (server_executable != null) {
        executable_path = server_executable.absolute_path
    }
    else {
        val packaged_server_filename: String = getServerExecutableFilename() ?: return null
        val packaged_server: File =
            File(System.getProperty("compose.application.resources.dir"))
                .resolve(packaged_server_filename)

        if (packaged_server.isFile) {
            executable_path = packaged_server.absolutePath
        }
        else {
            return null
        }
    }

    var command: String =
        when (hostOs) {
            OS.Windows -> "\"" + executable_path + "\""
            else -> executable_path.replace(" ", "\\ ")
        }

    val args: String = "--port $port"

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
            onExit(process.waitFor())
        }
    }

    return LocalServerProcess(command, process)
}
