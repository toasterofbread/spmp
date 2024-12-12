package com.toasterofbread.spmp.service.playercontroller

import androidx.compose.runtime.*
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerStatus internal constructor() {
    private var player: PlayerService? = null

    internal fun setPlayer(new_player: PlayerService) {
        player?.removeListener(player_listener)
        new_player.addListener(player_listener)

        player = new_player

        m_playing = playing
        m_duration_ms = duration_ms
        m_song = song
        m_index = index
        m_repeat_mode = repeat_mode
        m_has_next = has_next
        m_has_previous = has_previous
        m_volume = volume
        m_song_count = item_count
        m_undo_count = undo_count
        m_redo_count = redo_count
    }

    fun getProgress(): Float {
        val p = player ?: return 0f

        val duration = p.duration_ms
        if (duration <= 0f) {
            return 0f
        }

        return p.current_position_ms.toFloat() / duration
    }

    fun getPositionMs(): Long = player?.current_position_ms ?: 0

    private val _song_state: MutableState<Song?> = mutableStateOf(player?.getSong())
    val song_state: State<Song?> get() = _song_state

    val playing: Boolean get() = player?.is_playing ?: false
    val duration_ms: Long get() = player?.duration_ms ?: -1
    val song: Song? get() = player?.getSong()
    val index: Int get() = player?.current_item_index ?: -1
    val repeat_mode: SpMsPlayerRepeatMode get() = player?.repeat_mode ?: SpMsPlayerRepeatMode.NONE
    val has_next: Boolean get() = true
    val has_previous: Boolean get() = true
    val volume: Float get() = player?.volume ?: -1f
    val item_count: Int get() = player?.item_count ?: -1
    val undo_count: Int get() = player?.service_player?.undo_count ?: -1
    val redo_count: Int get() = player?.service_player?.redo_count ?: -1

    var m_playing: Boolean by mutableStateOf(playing)
        private set
    var m_duration_ms: Long by mutableStateOf(duration_ms)
        private set
    var m_song: Song? by _song_state
        private set
    var m_index: Int by mutableStateOf(index)
        private set
    var m_repeat_mode: SpMsPlayerRepeatMode by mutableStateOf(repeat_mode)
        private set
    // TODO
    var m_has_next: Boolean by mutableStateOf(has_next)
        private set
    var m_has_previous: Boolean by mutableStateOf(has_previous)
        private set
    var m_volume: Float by mutableStateOf(volume)
        private set
    var m_song_count: Int by mutableStateOf(item_count)
        private set
    var m_undo_count: Int by mutableStateOf(undo_count)
        private set
    var m_redo_count: Int by mutableStateOf(redo_count)
        private set

    override fun toString(): String =
        mapOf(
            "playing" to m_playing,
            "duration_ms" to m_duration_ms,
            "song" to m_song,
            "index" to m_index,
            "repeat_mode" to m_repeat_mode,
            "has_next" to m_has_next,
            "has_previous" to m_has_previous,
            "volume" to m_volume,
            "item_count" to m_song_count,
            "undo_count" to m_undo_count,
            "redo_count" to m_redo_count
        ).toString()

    private val player_listener: PlayerListener =
        object : PlayerListener() {
            init {
                onEvents()
            }

            override fun onSongTransition(song: Song?, manual: Boolean) {
                m_song = song
            }
            override fun onPlayingChanged(is_playing: Boolean) {
                m_playing = is_playing
            }
            override fun onRepeatModeChanged(repeat_mode: SpMsPlayerRepeatMode) {
                m_repeat_mode = repeat_mode
            }
            override fun onUndoStateChanged() {
                m_undo_count = undo_count
                m_redo_count = redo_count
            }
            override fun onDurationChanged(duration_ms: Long) {
                m_duration_ms = duration_ms

                val context: AppContext = player?.context ?: return
                val song: Song = m_song ?: return

                context.coroutineScope.launch(Dispatchers.IO) {
                    song.Duration.set(duration_ms, context.database)
                }
            }

            override fun onEvents() {
                m_duration_ms = duration_ms
                m_index = index
                m_volume = volume
                m_song_count = item_count

                player?.also { p ->
                    if (m_index > p.service_player.active_queue_index) {
                        p.service_player.active_queue_index = m_index
                    }
                }
            }
        }
}
