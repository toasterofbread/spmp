package com.spectre7.spmp.ui.layout.mainpage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.spectre7.spmp.PlayerService
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.MediaPlayerRepeatMode
import com.spectre7.spmp.platform.MediaPlayerService
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.ui.component.LongPressMenuData
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.utils.indexOfOrNull
import java.net.URI
import java.net.URISyntaxException

class PlayerStatus internal constructor(private val player: PlayerService) {
    fun getProgress(): Float = player.duration_ms.let { duration ->
        if (duration <= 0f) 0f
        else player.current_position_ms.toFloat() / duration
    }

    fun getPositionMillis(): Long = player.current_position_ms

    var m_playing: Boolean by mutableStateOf(player.is_playing)
        private set
    var m_duration_ms: Long by mutableStateOf(player.duration_ms)
        private set
    var m_song: Song? by mutableStateOf(player.getSong())
        private set
    var m_index: Int by mutableStateOf(player.current_song_index)
        private set
    var m_repeat_mode: MediaPlayerRepeatMode by mutableStateOf(player.repeat_mode)
        private set
    // TODO
    var m_has_next: Boolean by mutableStateOf(true)
        private set
    var m_has_previous: Boolean by mutableStateOf(true)
        private set
    var m_volume: Float by mutableStateOf(player.volume)
        private set
    var m_song_count: Int by mutableStateOf(player.song_count)
        private set
    var m_undo_count: Int by mutableStateOf(player.undo_count)
        private set
    var m_redo_count: Int by mutableStateOf(player.redo_count)
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
                m_undo_count = player.undo_count
                m_redo_count = player.redo_count
            }

            override fun onEvents() {
                m_duration_ms = player.duration_ms
                m_index = player.current_song_index
                m_volume = player.volume
                m_song_count = player.song_count

                if (m_index > player.active_queue_index) {
                    player.active_queue_index = m_index
                }
            }
        })
    }
}

open class PlayerState protected constructor(
    private val onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val upstream: PlayerState? = null
) {
    open val np_theme_mode: ThemeMode get() = upstream!!.np_theme_mode
    open val overlay_page: Pair<PlayerOverlayPage, MediaItem?>? get() = upstream!!.overlay_page
    open val bottom_padding: Dp get() = upstream!!.bottom_padding
    open val pill_menu: PillMenu get() = upstream!!.pill_menu
    open val main_multiselect_context: MediaItemMultiSelectContext get() = upstream!!.main_multiselect_context

    open val player: PlayerService get() = upstream!!.player
    open val nullable_player: PlayerService? get() = upstream!!.player
    open val download_manager: PlayerDownloadManager get() = upstream!!.download_manager
    open val status: PlayerStatus get() = upstream!!.status
    open val session_started: Boolean get() = upstream!!.session_started

    open fun interactService(action: (player: PlayerService) -> Unit) { upstream!!.interactService(action) }
    open fun isRunningAndFocused(): Boolean = upstream!!.isRunningAndFocused()

    fun copy(
        onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
        onLongClickedOverride: ((item: MediaItem) -> Unit)? = null
    ): PlayerState {
        return PlayerState(onClickedOverride, onLongClickedOverride, this)
    }

    fun openUri(uri_string: String): Result<Unit> {
        fun failure(reason: String): Result<Unit> = Result.failure(URISyntaxException(uri_string, reason))

        val uri = URI(uri_string)
        if (uri.host != "music.youtube.com" && uri.host != "www.youtube.com") {
            return failure("Unsupported host '${uri.host}'")
        }

        val path_parts = uri.path.split('/').filter { it.isNotBlank() }
        when (path_parts.firstOrNull()) {
            "channel" -> {
                val channel_id = path_parts.elementAtOrNull(1) ?: return failure("No channel ID")

                interactService {
                    openMediaItem(Artist.fromId(channel_id))
                }
            }
            "watch" -> {
                val v_start = (uri.query.indexOfOrNull("v=") ?: return failure("'v' query parameter not found")) + 2
                val v_end = uri.query.indexOfOrNull("&", v_start) ?: uri.query.length

                interactService {
                    playMediaItem(Song.fromId(uri.query.substring(v_start, v_end)))
                }
            }
            else -> return failure("Uri path not implemented")
        }

        return Result.success(Unit)
    }

    @Composable
    open fun nowPlayingTopOffset(base: Modifier): Modifier = upstream!!.nowPlayingTopOffset(base)
    @Composable
    open fun nowPlayingBottomPadding(): Dp = upstream!!.nowPlayingBottomPadding()

    open fun setOverlayPage(page: PlayerOverlayPage?, from_current: Boolean = false) { upstream!!.setOverlayPage(page, from_current) }

    open fun navigateBack() { upstream!!.navigateBack() }

    open fun onMediaItemClicked(item: MediaItem, multiselect_key: Int? = null) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item, multiselect_key)
        }
        else {
            upstream!!.onMediaItemClicked(item, multiselect_key)
        }
    }
    open fun onMediaItemLongClicked(item: MediaItem, queue_index: Int? = null) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item)
        }
        else {
            upstream!!.onMediaItemLongClicked(item, queue_index)
        }
    }

    open fun openPage(page: PlayerOverlayPage, from_current: Boolean = false) { upstream!!.openPage(page, from_current) }
    open fun openMediaItem(item: MediaItem, from_current: Boolean = false) { upstream!!.openMediaItem(item, from_current) }
    open fun playMediaItem(item: MediaItem, shuffle: Boolean = false) { upstream!!.playMediaItem(item, shuffle) }
    open fun openViewMoreURL(url: String) { upstream!!.openViewMoreURL(url) }

    open fun onMediaItemPinnedChanged(item: MediaItem, pinned: Boolean) { upstream!!.onMediaItemPinnedChanged(item, pinned) }

    open fun showLongPressMenu(data: LongPressMenuData) { upstream!!.showLongPressMenu(data) }
    fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }

    open fun hideLongPressMenu() { upstream!!.hideLongPressMenu() }

    // PlayerServiceHost


}
