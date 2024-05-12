package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.platform.PlatformPreferences
import io.ktor.client.request.get

open class ExternalPlayerService: SpMsPlayerService(), PlayerService {
    override val load_state: PlayerServiceLoadState get() = socket_load_state
    override val connection_error: Throwable? get() = socket_connection_error
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal lateinit var _context: AppContext
    override val context: AppContext get() = _context

    internal fun setContext(context: AppContext) {
        _context = context
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
        super.onCreate()

        _service_player = object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }
        }

        coroutine_scope.launch {
            val prefs_listener: PlatformPreferencesListener =
                PlatformPreferencesListener { _, key ->
                    if (key != context.settings.platform.SERVER_IP_ADDRESS.key && key != context.settings.platform.SERVER_PORT.key) {
                        return@PlatformPreferencesListener
                    }

                    cancel_connection = true
                    restart_connection = true
                }
            context.getPrefs().addListener(prefs_listener)

            try {
                connectToServer(
                    getIp = { context.settings.platform.SERVER_IP_ADDRESS.get() },
                    getPort = { context.settings.platform.SERVER_PORT.get() }
                )
            }
            finally {
                context.getPrefs().removeListener(prefs_listener)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }
}
