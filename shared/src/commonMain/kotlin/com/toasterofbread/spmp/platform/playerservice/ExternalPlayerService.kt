package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ServerSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState
import com.toasterofbread.composekit.platform.PlatformPreferencesListener

open class ExternalPlayerService: SpMsPlayerService(), PlayerService {
    override val load_state: PlayerServiceLoadState get() = socket_load_state
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal lateinit var _context: AppContext
    override val context: AppContext get() = _context
    
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
    override val radio_state: RadioInstance.RadioState
        get() = service_player.radio_state
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = _repeat_mode
        set(value) {
            if (value == _repeat_mode) {
                return
            }
            sendRequest("setRepeatMode", value.ordinal)
        }
    override var volume: Float
        get() = _volume
        set(value) {
            if (value == _volume) {
                return
            }
            sendRequest("setVolume", value)
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

    override fun seekTo(position_ms: Long) {
        sendRequest("seekToTime", position_ms)
    }

    override fun seekToSong(index: Int) {
        sendRequest("seekToItem", index)
    }

    override fun seekToNext() {
        sendRequest("seekToNext")
    }

    override fun seekToPrevious() {
        sendRequest("seekToPrevious")
    }

    override fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    override fun getSong(index: Int): Song? = playlist.getOrNull(index)

    override fun addSong(song: Song, index: Int) {
        sendRequest("addItem", song.id, index)
    }

    override fun moveSong(from: Int, to: Int) {
        sendRequest("moveItem", from, to)
    }

    override fun removeSong(index: Int) {
        sendRequest("removeItem", index)
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
                object : PlatformPreferencesListener {
                    override fun onChanged(prefs: PlatformPreferences, key: String) {
                        if (key != ServerSettings.Key.EXTERNAL_SERVER_IP_ADDRESS.getName() && key != ServerSettings.Key.SERVER_PORT.getName()) {
                            return
                        }
                        
                        cancel_connection = true
                        restart_connection = true
                    }
                }
            context.getPrefs().addListener(prefs_listener)
            
            try {
                connectToServer(
                    getIp = { ServerSettings.Key.EXTERNAL_SERVER_IP_ADDRESS.get(context) },
                    getPort = { ServerSettings.Key.SERVER_PORT.get(context) }
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
