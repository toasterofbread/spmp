package com.spectre7.spmp

import com.spectre7.spmp.platform.PlatformContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.MediaPlayerRepeatMode
import com.spectre7.spmp.platform.MediaPlayerService
import com.spectre7.spmp.platform.PlatformService

enum class SERVICE_INTENT_ACTIONS { STOP, BUTTON_VOLUME }

class PlayerServiceHost {

    private var player: PlayerService? by mutableStateOf(null)

    private var service_connecting = false
    private var service_connection: Any? = null

    private val context: PlatformContext get() = SpMp.context
    private val download_manager = PlayerDownloadManager(context)

    lateinit var status: PlayerStatus
        private set

    init {
        assert(instance == null)
        instance = this
    }

    class PlayerStatus internal constructor(private val player: PlayerService) {
        val playing: Boolean get() = player.is_playing
        val position: Float get() = player.duration_ms.let { it ->
            if (it <= 0f) 0f
            else player.current_position_ms.toFloat() / it
        }
        val position_seconds: Float get() = player.current_position_ms / 1000f
        val duration: Float get() = player.duration_ms / 1000f
        val song: Song? get() = player.getSong()
        val index: Int get() = player.current_song_index
        val repeat_mode: MediaPlayerRepeatMode get() = player.repeat_mode
        val shuffle: Boolean get() = player.shuffle_enabled
        val has_next: Boolean get() = true // TODO
        val has_previous: Boolean get() = true // TODO
        var volume: Float
            get() = player.volume
            set(value) { player.volume = value }
        val queue_size: Int get() = player.song_count

        var m_playing: Boolean by mutableStateOf(false)
            private set
        var m_duration: Float by mutableStateOf(0f)
            private set
        var m_song: Song? by mutableStateOf(null)
            private set
        var m_index: Int by mutableStateOf(0)
            private set
        var m_shuffle: Boolean by mutableStateOf(false)
            private set
        var m_repeat_mode: MediaPlayerRepeatMode by mutableStateOf(MediaPlayerRepeatMode.values()[0])
            private set
        var m_has_next: Boolean by mutableStateOf(false)
            private set
        var m_has_previous: Boolean by mutableStateOf(false)
            private set
        var m_volume: Float by mutableStateOf(0f)
            private set
        var m_queue_size: Int by mutableStateOf(0)
            private set

        init {
            player.addListener(object : MediaPlayerService.Listener() {
                override fun onMediaItemTransition(song: Song?) {
                    m_song = song
                }
                override fun onPlayingChanged(is_playing: Boolean) {
                    m_playing = is_playing
                }
                override fun onShuffleEnabledChanged(shuffle_enabled: Boolean) {
                    m_shuffle = shuffle_enabled
                }
                override fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {
                    m_repeat_mode = repeat_mode
                }

                override fun onEvents() {
                    m_has_next = has_next
                    m_has_previous = has_previous
                    m_duration = duration
                    m_index = index
                    m_volume = volume
                    m_queue_size = queue_size

                    if (index > player.active_queue_index) {
                        player.active_queue_index = index
                    }
                }
            })
        }
    }

    companion object {
        var instance: PlayerServiceHost? = null
        val status: PlayerStatus get() = instance!!.status

        val player: PlayerService get() = instance!!.player!!
        val download_manager: PlayerDownloadManager get() = instance!!.download_manager
        val service_connected: Boolean get() = instance?.player != null
        val session_started: Boolean get() = instance?.player?.session_started ?: false

        fun isRunningAndFocused(): Boolean {
            if (instance == null) {
                return false
            }

            if (!player.has_focus) {
                return false
            }

            return true
        }

        fun release() {
            instance?.release()
        }
    }

    private fun release() {
        if (service_connection != null) {
            PlatformService.unbindService(context, service_connection!!)
            service_connection = null
        }
        download_manager.release()
    }

    @Synchronized
    fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        if (service_connecting || player != null) {
            return
        }

        service_connecting = true
        service_connection = PlatformService.startService(
            context,
            PlayerService::class.java,
            onConnected = { binder ->
                player = (binder as PlayerService.PlayerBinder).getService()
                status = PlayerStatus(player!!)
                service_connecting = false
                onConnected?.invoke()
            },
            onDisconnected = {
                player = null
                service_connecting = false
                onDisconnected?.invoke()
            }
        )
    }

    interface PlayerQueueListener {
        fun onSongAdded(song: Song, index: Int)
        fun onSongRemoved(song: Song, index: Int)
        fun onSongMoved(from: Int, to: Int)
        fun onCleared()
    }
}
