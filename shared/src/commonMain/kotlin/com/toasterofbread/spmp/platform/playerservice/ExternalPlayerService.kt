package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Job
import kotlinx.serialization.json.JsonPrimitive
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.platform.PlatformPreferences
import io.ktor.client.request.get
import LocalPlayerState
import LocalProgramArguments

open class ExternalPlayerService(plays_audio: Boolean): SpMsPlayerService(plays_audio = plays_audio), PlayerService {
    override val load_state: PlayerServiceLoadState get() =
        (local_server_error ?: connect_error)?.let {
            socket_load_state.copy(error = it)
        } ?: socket_load_state

    private var connect_error: Throwable? by mutableStateOf(null)
    private var local_server_error: Throwable? by mutableStateOf(null)
    private var local_server_process: Job? by mutableStateOf(null)

    override fun getIpAddress(): String =
        if (local_server_process != null) "127.0.0.1" else context.settings.platform.SERVER_IP_ADDRESS.get()
    override fun getPort(): Int =
        context.settings.platform.SERVER_PORT.get()

    internal lateinit var _context: AppContext
    override val context: AppContext get() = _context

    internal fun setContext(context: AppContext) {
        _context = context
    }

    internal fun notifyReadyToPlay(song_duration_ms: Long) {
        require(song_duration_ms > 0) { song_duration_ms }

        val song: Song = getSong() ?: return
        sendRequest("readyToPlay", JsonPrimitive(current_song_index), JsonPrimitive(song.id), JsonPrimitive(song_duration_ms))
    }

    private var cancelling_radio: Boolean = false

    override fun onRadioCancelRequested() {
        cancelling_radio = true
        radio_instance.cancelRadio()
        cancelling_radio = false
    }

    internal fun onRadioCancelled() {
        if (cancelling_radio) {
            return
        }
        sendRequest("cancelRadio")
    }

    protected open fun createServicePlayer(): PlayerServicePlayer =
        object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }

            override val radio: RadioHandler =
                object : RadioHandler(this, context) {
                    override fun onRadioCancelled() {
                        super.onRadioCancelled()
                        this@ExternalPlayerService.onRadioCancelled()
                    }
                }
        }

    private lateinit var _service_player: PlayerServicePlayer
    override val service_player: PlayerServicePlayer
        get() = _service_player

    override val state: SpMsPlayerState
        get() = _state
    override val is_playing: Boolean
        get() = _is_playing
    override val song_count: Int
        get() = playlist.size
    override val current_song_index: Int
        get() = _current_song_index
    override val current_position_ms: Long
        get() {
            if (current_song_time < 0) {
                return 0
            }
            if (!_is_playing) {
                return current_song_time
            }
            return System.currentTimeMillis() - current_song_time
        }
    override val duration_ms: Long
        get() = _duration_ms
    override val has_focus: Boolean
        get() = true // TODO
    override val radio_instance: RadioInstance
        get() = service_player.radio_instance
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = _repeat_mode
        set(value) {
            if (value == _repeat_mode) {
                return
            }
            sendRequest("setRepeatMode", JsonPrimitive(value.ordinal))
        }
    override var volume: Float
        get() = _volume
        set(value) {
            if (value == _volume) {
                return
            }
            sendRequest("setVolume", JsonPrimitive(value))
        }

    override fun isPlayingOverLatentDevice(): Boolean = false // TODO

    override fun play() {
        sendRequest("play")
    }

    override fun pause() {
        sendRequest("pause")
    }

    override fun playPause() {
        sendRequest("playPause")
    }

    private val song_seek_undo_stack: MutableList<Pair<Int, Long>> = mutableListOf()
    private fun getSeekPosition(): Pair<Int, Long> = Pair(current_song_index, current_position_ms)

    override fun seekTo(position_ms: Long) {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToTime", JsonPrimitive(position_ms))
        song_seek_undo_stack.add(current)
    }

    override fun seekToSong(index: Int) {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToItem", JsonPrimitive(index))
        song_seek_undo_stack.add(current)
    }

    override fun seekToNext() {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToNext")
        song_seek_undo_stack.add(current)
    }

    override fun seekToPrevious() {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToPrevious")
        song_seek_undo_stack.add(current)
    }

    override fun undoSeek() {
        val (index: Int, position_ms: Long) = song_seek_undo_stack.removeLastOrNull() ?: return

        if (index != current_song_index) {
            sendRequest("seekToItem", JsonPrimitive(index), JsonPrimitive(position_ms))
        }
        else {
            sendRequest("seekToTime", JsonPrimitive(position_ms))
        }
    }

    override fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    override fun getSong(index: Int): Song? = playlist.getOrNull(index)

    override fun addSong(song: Song, index: Int) {
        sendRequest("addItem", JsonPrimitive(song.id), JsonPrimitive(index))
    }

    override fun moveSong(from: Int, to: Int) {
        sendRequest("moveItem", JsonPrimitive(from), JsonPrimitive(to))
    }

    override fun removeSong(index: Int) {
        sendRequest("removeItem", JsonPrimitive(index))
    }

    @Composable
    override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
    }

    override fun onCreate() {
        _service_player = createServicePlayer()
        super.onCreate()
    }

    @Composable
    override fun LoadScreenExtraContent(item_modifier: Modifier, requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val launch_arguments: ProgramArguments = LocalProgramArguments.current

        LaunchedEffect(Unit) {
            local_server_error = null
            local_server_process = null
        }

        val external_server_mode: Boolean by player.settings.platform.ENABLE_EXTERNAL_SERVER_MODE.observe()

        fun startServer(stop_if_running: Boolean, automatic: Boolean) {
            if (automatic && launch_arguments.no_auto_server) {
                return
            }

            local_server_process?.also { process ->
                if (stop_if_running) {
                    local_server_process = null
                    process.cancel()
                }
                return
            }

            LocalServer.startLocalServer(
                player.context,
                player.settings.platform.SERVER_PORT.get()
            ).fold(
                onSuccess = {
                    local_server_process = it
                    if (!automatic && local_server_process == null) {
                        local_server_error = RuntimeException(getString("loading_splash_local_server_command_not_set"))
                    }
                },
                onFailure = { e ->
                    local_server_process = null
                    local_server_error = e
                }
            )
        }

        val server_unavailability_reason: String? = remember { LocalServer.getLocalServerUnavailabilityReason() }
        var show_unavailability_dialog: Boolean by remember { mutableStateOf(false) }

        if (show_unavailability_dialog) {
            AlertDialog(
                onDismissRequest = { show_unavailability_dialog },
                confirmButton = {
                    Button({ show_unavailability_dialog = false }) {
                        Text(getString("action_close"))
                    }
                },
                title = {
                    Text(getString("warning_server_unavailable_title"))
                },
                text = {
                    Text(server_unavailability_reason ?: "")
                }
            )
        }

        if (server_unavailability_reason == null || local_server_process != null) {
            Button(
                { startServer(stop_if_running = true, automatic = false) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = player.theme.accent,
                        contentColor = player.theme.on_accent
                    ),
                modifier = item_modifier
            ) {
                Crossfade(local_server_process) { process ->
                    if (process == null) {
                        Text(getString("loading_splash_button_start_local_server"))
                    }
                    else {
                        Text(getString("loading_splash_button_stop_local_server"))
                    }
                }
            }
        }
        else if (server_unavailability_reason != null) {
            OutlinedButton(
                { show_unavailability_dialog = !show_unavailability_dialog },
                border = BorderStroke(Dp.Hairline, player.theme.accent),
                modifier = item_modifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null)
                    Text(getString("loading_splash_button_local_server_unavailable"))
                }
            }
        }

        // Crossfade(local_server_error ?: local_server_process as Any?) { state ->
        //     if (state != null) {
        //         Column(
        //             Modifier.padding(top = 20.dp),
        //             horizontalAlignment = Alignment.CenterHorizontally,
        //             verticalArrangement = Arrangement.spacedBy(10.dp)
        //         ) {
        //             if (state is Throwable) {
        //                 Text(getString("error_on_server_command_execution"))
        //                 ErrorInfoDisplay(
        //                     state,
        //                     show_throw_button = true,
        //                     onDismiss = { local_server_error = null }
        //                 )
        //             }
        //             else if (state is LocalServerProcess) {
        //                 Text(getString("loading_splash_process_running_with_command_\$x").replace("\$x", state.launch_command))
        //             }
        //         }
        //     }
        // }
    }
}
