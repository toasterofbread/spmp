package com.toasterofbread.spmp.platform.playerservice

import LocalPlayerState
import LocalProgramArguments
import ProgramArguments
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.loading_splash_button_local_server_unavailable
import spmp.shared.generated.resources.loading_splash_button_start_local_server
import spmp.shared.generated.resources.loading_splash_button_stop_local_server
import spmp.shared.generated.resources.loading_splash_local_server_command_not_set
import spmp.shared.generated.resources.warning_server_unavailable_title
import kotlin.time.Duration

open class ExternalPlayerService(plays_audio: Boolean): SpMsPlayerService(plays_audio = plays_audio), PlayerService {
    override val load_state: PlayerServiceLoadState get() =
        (local_server_error ?: connect_error)?.let {
            socket_load_state.copy(error = it)
        } ?: socket_load_state

    private var connect_error: Throwable? by mutableStateOf(null)
    private var local_server_error: Throwable? by mutableStateOf(null)
    private var local_server_process: Job? by mutableStateOf(null)

    override suspend fun getIpAddress(): String =
        if (local_server_process != null) "127.0.0.1" else context.settings.Platform.SERVER_IP_ADDRESS.get()
    override suspend fun getPort(): Int =
        context.settings.Platform.SERVER_PORT.get()

    internal lateinit var _context: AppContext
    override val context: AppContext get() = _context

    internal fun setContext(context: AppContext) {
        _context = context
    }

    override fun release() {}

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    override fun onShutdown() {}

    internal fun notifyReadyToPlay(song_duration_ms: Long) {
        require(song_duration_ms > 0) { song_duration_ms }

        val song: Song = getSong() ?: return
        sendRequest("readyToPlay", JsonPrimitive(current_item_index), JsonPrimitive(song.id), JsonPrimitive(song_duration_ms))
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
    override val item_count: Int
        get() = playlist.size
    override val current_item_index: Int
        get() = _current_item_index
    override val current_position_ms: Long
        get() {
            if (current_song_time < 0) {
                return 0
            }
            if (!_is_playing) {
                return current_song_time
            }
            return playback_start_mark.elapsedNow().inWholeMilliseconds
        }
    override val duration_ms: Long
        get() = _duration_ms
    override val radio_instance: RadioInstance
        get() = service_player.radio_instance
    override val repeat_mode: SpMsPlayerRepeatMode
        get() = _repeat_mode
    override val volume: Float
        get() = _volume

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
    private fun getSeekPosition(): Pair<Int, Long> = Pair(current_item_index, current_position_ms)

    override fun seekToTime(position_ms: Long) {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToTime", JsonPrimitive(position_ms))
        song_seek_undo_stack.add(current)
    }

    override fun setRepeatMode(repeat_mode: SpMsPlayerRepeatMode) {
        if (repeat_mode == _repeat_mode) {
            return
        }
        sendRequest("setRepeatMode", JsonPrimitive(repeat_mode.ordinal))
    }

    override fun setVolume(value: Double) {
        if (value.toFloat() == _volume) {
            return
        }
        sendRequest("setVolume", JsonPrimitive(value))
    }

    override fun seekToItem(index: Int, position_ms: Long) {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToItem", JsonPrimitive(index), JsonPrimitive(position_ms))
        song_seek_undo_stack.add(current)
    }

    override fun seekToNext(): Boolean {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToNext")
        song_seek_undo_stack.add(current)
        return current_item_index + 1 < item_count
    }

    override fun seekToPrevious(repeat_threshold: Duration?): Boolean {
        val current: Pair<Int, Long> = getSeekPosition()
        sendRequest("seekToPrevious", JsonPrimitive(repeat_threshold?.inWholeMilliseconds ?: -1))
        song_seek_undo_stack.add(current)
        return current_item_index > 0
    }

    override fun undoSeek() {
        val (index: Int, position_ms: Long) = song_seek_undo_stack.removeLastOrNull() ?: return

        if (index != current_item_index) {
            sendRequest("seekToItem", JsonPrimitive(index), JsonPrimitive(position_ms))
        }
        else {
            sendRequest("seekToTime", JsonPrimitive(position_ms))
        }
    }

    override fun getSong(): Song? = playlist.getOrNull(_current_item_index)

    override fun getSong(index: Int): Song? = playlist.getOrNull(index)

    override fun getItem(): String? =
        getSong()?.id

    override fun getItem(index: Int): String? =
        getSong(index)?.id

    override fun addSong(song: Song, index: Int): Int {
        return addItem(song.id, index)
    }

    override fun addItem(item_id: String, index: Int): Int {
        val add_to_index: Int =
            if (index < 0) 0
            else index.coerceAtMost(item_count)

        sendRequest("addItem", JsonPrimitive(item_id), JsonPrimitive(add_to_index))

        return add_to_index
    }

    override fun canPlay(): Boolean = true

    override fun moveItem(from: Int, to: Int) {
        sendRequest("moveItem", JsonPrimitive(from), JsonPrimitive(to))
    }

    override fun removeItem(index: Int) {
        sendRequest("removeItem", JsonPrimitive(index))
    }

    override fun clearQueue() {
        sendRequest("clearQueue")
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
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        val launch_arguments: ProgramArguments = LocalProgramArguments.current

        LaunchedEffect(Unit) {
            local_server_error = null
            local_server_process = null
        }

        suspend fun startServer(stop_if_running: Boolean, automatic: Boolean) {
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
                player.settings.Platform.SERVER_PORT.get()
            ).fold(
                onSuccess = {
                    local_server_process = it
                    if (!automatic && local_server_process == null) {
                        local_server_error = RuntimeException(getString(Res.string.loading_splash_local_server_command_not_set))
                    }
                },
                onFailure = { e ->
                    local_server_process = null
                    local_server_error = e
                }
            )
        }

        var server_unavailability_reason: String? by remember { mutableStateOf(null) }
        var server_unavailability_reason_loaded: Boolean by remember { mutableStateOf(false) }
        var show_unavailability_dialog: Boolean by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            server_unavailability_reason = LocalServer.getLocalServerUnavailabilityReason()
            server_unavailability_reason_loaded = true
        }

        if (show_unavailability_dialog) {
            AlertDialog(
                onDismissRequest = { show_unavailability_dialog = false },
                confirmButton = {
                    Button({ show_unavailability_dialog = false }) {
                        Text(stringResource(Res.string.action_close))
                    }
                },
                title = {
                    Text(stringResource(Res.string.warning_server_unavailable_title))
                },
                text = {
                    Text(server_unavailability_reason ?: "")
                }
            )
        }

        if ((server_unavailability_reason_loaded && server_unavailability_reason == null) || local_server_process != null) {
            Button(
                {
                    coroutine_scope.launch {
                        startServer(stop_if_running = true, automatic = false)
                    }
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = player.theme.accent,
                        contentColor = player.theme.onAccent
                    ),
                modifier = item_modifier
            ) {
                Crossfade(local_server_process) { process ->
                    if (process == null) {
                        Text(stringResource(Res.string.loading_splash_button_start_local_server))
                    }
                    else {
                        Text(stringResource(Res.string.loading_splash_button_stop_local_server))
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
                    Text(stringResource(Res.string.loading_splash_button_local_server_unavailable))
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
        //                 Text(stringResource(Res.string.error_on_server_command_execution))
        //                 ErrorInfoDisplay(
        //                     state,
        //                     show_throw_button = true,
        //                     onDismiss = { local_server_error = null }
        //                 )
        //             }
        //             else if (state is LocalServerProcess) {
        //                 Text(stringResource(Res.string.loading_splash_process_running_with_command_\$x).replace("\$x", state.launch_command))
        //             }
        //         }
        //     }
        // }
    }
}
