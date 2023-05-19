package com.spectre7.spmp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.*

enum class SERVICE_INTENT_ACTIONS { STOP, BUTTON_VOLUME }

class PlayerServiceHost() {

    private var player: PlayerService? by mutableStateOf(null)

    private var service_connecting = false
    private var service_connected_listeners = mutableListOf<() -> Unit>()

    private val context: PlatformContext get() = SpMp.context
    private val download_manager = PlayerDownloadManager(context)

    lateinit var status: PlayerStatus
        private set

    init {
        assert(instance == null)
        instance = this
    }

    // TODO remove (at least the non-mutablestate stuff)
    class PlayerStatus internal constructor(private val player: PlayerService) {
        val playing: Boolean get() = player.is_playing
        val position: Float get() = player.duration_ms.let { it ->
            if (it <= 0f) 0f
            else player.current_position_ms.toFloat() / it
        }
        val position_ms: Long get() = player.current_position_ms
        val position_seconds: Float get() = player.current_position_ms / 1000f
        val duration: Float get() = player.duration_ms / 1000f
        val song: Song? get() = player.getSong()
        val index: Int get() = player.current_song_index
        val repeat_mode: MediaPlayerRepeatMode get() = player.repeat_mode
        var volume: Float
            get() = player.volume
            set(value) { player.volume = value }
        val queue_size: Int get() = player.song_count
        val undo_count: Int get() = player.undo_count
        val redo_count: Int get() = player.redo_count

        var m_playing: Boolean by mutableStateOf(playing)
            private set
        var m_duration: Float by mutableStateOf(duration)
            private set
        var m_song: Song? by mutableStateOf(song)
            private set
        var m_index: Int by mutableStateOf(index)
            private set
        var m_repeat_mode: MediaPlayerRepeatMode by mutableStateOf(repeat_mode)
            private set
        var m_has_next: Boolean by mutableStateOf(true)
            private set
        var m_has_previous: Boolean by mutableStateOf(true)
            private set
        var m_volume: Float by mutableStateOf(volume)
            private set
        var m_queue_size: Int by mutableStateOf(queue_size)
            private set
        var m_undo_count: Int by mutableStateOf(undo_count)
            private set
        var m_redo_count: Int by mutableStateOf(redo_count)
            private set

        init {
            player.addListener(object : MediaPlayerService.Listener() {
                override fun onSongTransition(song: Song?) {
                    m_song = song
                }
                override fun onPlayingChanged(is_playing: Boolean) {
                    m_playing = is_playing
                }
                override fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {
                    m_repeat_mode = repeat_mode
                }
                override fun onUndoStateChanged() {
                    m_undo_count = undo_count
                    m_redo_count = redo_count
                }

                override fun onEvents() {
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
        val nullable_player: PlayerService? get() = instance?.player
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
        player?.also {
            MediaPlayerService.disconnect(context, it)
            player = null
        }
        download_manager.release()
    }

    fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        synchronized(service_connected_listeners) {
            if (player != null) {
                onConnected?.invoke()
                return
            }
            else if (service_connecting) {
                onConnected?.also { service_connected_listeners.add(it) }
                return
            }
        }

        service_connecting = true
        MediaPlayerService.connect(
            context,
            PlayerService::class.java,
            onConnected = { service ->
                synchronized(service_connected_listeners) {
                    player = service
                    status = PlayerStatus(player!!)
                    service_connecting = false

                    onConnected?.invoke()
                    service_connected_listeners.forEach { it() }
                    service_connected_listeners.clear()
                }
            },
            onDisconnected = {
                synchronized(service_connected_listeners) {
                    player = null
                    service_connecting = false
                    onDisconnected?.invoke()
                }
            }
        )
    }

    fun interactService(action: (player: PlayerService) -> Unit) {
        startService({
            player?.also { action(it) }
        })
    }
}
