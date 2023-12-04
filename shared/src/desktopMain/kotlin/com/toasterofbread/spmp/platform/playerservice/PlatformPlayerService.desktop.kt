package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlatformBinder
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.startPlatformService
import com.toasterofbread.spmp.platform.unbindPlatformService
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance

private class PlayerServiceBinder(val service: PlatformPlayerService): PlatformBinder()

actual class PlatformPlayerService: ZmqSpMsPlayerService(), PlayerService {
    actual val load_state: PlayerServiceLoadState get() = socket_load_state
    actual override val context: AppContext get() = super.context

    override val listeners: List<PlayerListener>
        get() = Companion.listeners
    private lateinit var _service_player: PlayerServicePlayer
    actual override val service_player: PlayerServicePlayer
        get() = _service_player

    actual override val state: MediaPlayerState
        get() = _state
    actual override val is_playing: Boolean
        get() = _is_playing
    actual override val song_count: Int
        get() = playlist.size
    actual override val current_song_index: Int
        get() = _current_song_index
    actual override val current_position_ms: Long
        get() {
            if (current_song_time < 0) {
                return 0
            }
            if (!_is_playing) {
                return current_song_time
            }
            return System.currentTimeMillis() - current_song_time
        }
    actual override val duration_ms: Long
        get() = _duration_ms
    actual override val has_focus: Boolean
        get() = true // TODO
    actual override val radio_state: RadioInstance.RadioState
        get() = _radio_state
    actual override var repeat_mode: MediaPlayerRepeatMode
        get() = _repeat_mode
        set(value) {
            if (value == _repeat_mode) {
                return
            }
            sendRequest("setRepeatMode", value.ordinal)
        }
    actual override var volume: Float
        get() = _volume
        set(value) {
            if (value == _volume) {
                return
            }
            sendRequest("setVolume", value)
        }

    actual override fun isPlayingOverLatentDevice(): Boolean = false // TODO

    actual override fun play() {
        sendRequest("play")
    }

    actual override fun pause() {
        sendRequest("pause")
    }

    actual override fun playPause() {
        sendRequest("playPause")
    }

    actual override fun seekTo(position_ms: Long) {
        sendRequest("seekToTime", position_ms)
    }

    actual override fun seekToSong(index: Int) {
        sendRequest("seekToItem", index)
    }

    actual override fun seekToNext() {
        sendRequest("seekToNext")
    }

    actual override fun seekToPrevious() {
        sendRequest("seekToPrevious")
    }

    actual override fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    actual override fun getSong(index: Int): Song? = playlist.getOrNull(index)

    actual override fun addSong(song: Song, index: Int) {
        sendRequest("addItem", song.id, index)
    }

    actual override fun moveSong(from: Int, to: Int) {
        sendRequest("moveItem", from, to)
    }

    actual override fun removeSong(index: Int) {
        sendRequest("removeItem", index)
    }

    actual override fun addListener(listener: PlayerListener) {}

    actual override fun removeListener(listener: PlayerListener) {}

    @Composable
    actual override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
    }

    override fun onBind(): PlatformBinder? {
        return PlayerServiceBinder(this)
    }

    actual override fun onCreate() {
        _service_player = object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }
        }

        super.onCreate()
    }

    actual override fun onDestroy() {
        super.onDestroy()
    }

    actual companion object {
        private val listeners: MutableList<PlayerListener> = mutableListOf()

        actual fun isServiceRunning(context: AppContext): Boolean = true

        actual fun addListener(listener: PlayerListener) {
            listeners.add(listener)
        }

        actual fun removeListener(listener: PlayerListener) {
            listeners.remove(listener)
        }

        actual fun connect(
            context: AppContext,
            instance: PlatformPlayerService?,
            onConnected: (PlatformPlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            return startPlatformService(
                context,
                PlatformPlayerService::class.java,
                onConnected = { binder ->
                    onConnected((binder as PlayerServiceBinder).service)
                },
                onDisconnected = onDisconnected
            )
        }

        actual fun disconnect(context: AppContext, connection: Any) {
            unbindPlatformService(context, connection)
        }
    }
}
