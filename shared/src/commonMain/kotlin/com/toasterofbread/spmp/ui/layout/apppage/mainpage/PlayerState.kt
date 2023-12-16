package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.indexOfOrNull
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.playerservice.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import java.net.URI
import java.net.URISyntaxException

typealias DownloadRequestCallback = (DownloadStatus?) -> Unit

class PlayerStatus internal constructor() {
    private var player: PlatformPlayerService? = null

    internal fun setPlayer(new_player: PlatformPlayerService) {
        player = new_player

        m_playing = playing
        m_duration_ms = duration_ms
        m_song = song
        m_index = index
        m_repeat_mode = repeat_mode
        m_has_next = has_next
        m_has_previous = has_previous
        m_volume = volume
        m_song_count = song_count
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

    fun getPositionMillis(): Long = player?.current_position_ms ?: 0

    private val _song_state: MutableState<Song?> = mutableStateOf(player?.getSong())
    val song_state: State<Song?> get() = _song_state

    val playing: Boolean get() = player?.is_playing ?: false
    val duration_ms: Long get() = player?.duration_ms ?: -1
    val song: Song? get() = player?.getSong()
    val index: Int get() = player?.current_song_index ?: -1
    val repeat_mode: MediaPlayerRepeatMode get() = player?.repeat_mode ?: MediaPlayerRepeatMode.NONE
    val has_next: Boolean get() = true
    val has_previous: Boolean get() = true
    val volume: Float get() = player?.volume ?: -1f
    val song_count: Int get() = player?.song_count ?: -1
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
    var m_repeat_mode: MediaPlayerRepeatMode by mutableStateOf(repeat_mode)
        private set
    // TODO
    var m_has_next: Boolean by mutableStateOf(has_next)
        private set
    var m_has_previous: Boolean by mutableStateOf(has_previous)
        private set
    var m_volume: Float by mutableStateOf(volume)
        private set
    var m_song_count: Int by mutableStateOf(song_count)
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
            "song_count" to m_song_count,
            "undo_count" to m_undo_count,
            "redo_count" to m_redo_count
        ).toString()

    init {
        PlatformPlayerService.addListener(object : PlayerListener() {
            init {
                onEvents()
            }

            override fun onSongTransition(song: Song?, manual: Boolean) {
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
            override fun onDurationChanged(duration_ms: Long) {
                m_duration_ms = duration_ms
            }

            override fun onEvents() {
                m_duration_ms = duration_ms
                m_index = index
                m_volume = volume
                m_song_count = song_count

                player?.also { p ->
                    if (m_index > p.service_player.active_queue_index) {
                        p.service_player.active_queue_index = m_index
                    }
                }
            }
        })
    }
}

open class PlayerState protected constructor(
    private val onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem, long_press_data: LongPressMenuData?) -> Unit)? = null,
    private val upstream: PlayerState? = null
) {
    val database: Database get() = context.database
    val theme: Theme get() = context.theme
    open val context: AppContext get() = upstream!!.context

    open fun switchNowPlayingPage(page: Int): Unit = upstream!!.switchNowPlayingPage(page)
    open val expansion: NowPlayingExpansionState get() = upstream!!.expansion

    open val app_page_state: AppPageState get() = upstream!!.app_page_state
    val app_page: AppPage get() = app_page_state.current_page

    open val main_multiselect_context: MediaItemMultiSelectContext get() = upstream!!.main_multiselect_context
    open val np_theme_mode: ThemeMode get() = upstream!!.np_theme_mode
    open val np_overlay_menu: MutableState<PlayerOverlayMenu?> get() = upstream!!.np_overlay_menu
    open val top_bar: MusicTopBar get() = upstream!!.top_bar

    open val controller: PlatformPlayerService? get() = upstream!!.controller
    open fun withPlayer(action: PlayerServicePlayer.() -> Unit): Unit = upstream!!.withPlayer(action)
    @Composable
    open fun withPlayerComposable(action: @Composable PlayerServicePlayer.() -> Unit): Unit = upstream!!.withPlayerComposable(action)

    open val status: PlayerStatus get() = upstream!!.status
    open val session_started: Boolean get() = upstream!!.session_started

    open val screen_size: DpSize get() = upstream!!.screen_size

    open fun interactService(action: (player: PlatformPlayerService) -> Unit) { upstream!!.interactService(action) }
    open fun isRunningAndFocused(): Boolean = upstream!!.isRunningAndFocused()

    fun copy(
        onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
        onLongClickedOverride: ((item: MediaItem, long_press_data: LongPressMenuData?) -> Unit)? = null
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
                    val artist = ArtistRef(channel_id)
                    artist.createDbEntry(context.database)
                    openMediaItem(artist)
                }
            }
            "watch" -> {
                val v_start = (uri.query.indexOfOrNull("v=") ?: return failure("'v' query parameter not found")) + 2
                val v_end = uri.query.indexOfOrNull("&", v_start) ?: uri.query.length

                interactService {
                    val song = SongRef(uri.query.substring(v_start, v_end))
                    song.createDbEntry(context.database)
                    playMediaItem(song)
                }
            }
            "playlist" -> {
                val list_start = (uri.query.indexOfOrNull("list=") ?: return failure("'list' query parameter not found")) + 5
                val list_end = uri.query.indexOfOrNull("&", list_start) ?: uri.query.length

                interactService {
                    val playlist = RemotePlaylistRef(uri.query.substring(list_start, list_end))
                    playlist.createDbEntry(context.database)
                    openMediaItem(playlist)
                }
            }
            else -> return failure("Uri path not implemented")
        }

        return Result.success(Unit)
    }

    @Composable
    open fun nowPlayingTopOffset(base: Modifier, force_top: Boolean = false): Modifier = upstream!!.nowPlayingTopOffset(base, force_top)
    @Composable
    open fun nowPlayingBottomPadding(include_np: Boolean = false): Dp = upstream!!.nowPlayingBottomPadding(include_np)

    open fun onNavigationBarTargetColourChanged(colour: Color?, from_lpm: Boolean): Unit = upstream!!.onNavigationBarTargetColourChanged(colour, from_lpm)

    open fun openAppPage(page: AppPage?, from_current: Boolean = false, replace_current: Boolean = false) { upstream!!.openAppPage(
        page,
        replace_current = replace_current
    ) }

    open fun navigateBack() { upstream!!.navigateBack() }

    open fun onMediaItemClicked(item: MediaItem, multiselect_key: Int? = null) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item, multiselect_key)
        }
        else {
            upstream!!.onMediaItemClicked(item, multiselect_key)
        }
    }
    open fun onMediaItemLongClicked(item: MediaItem, long_press_data: LongPressMenuData? = null) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item, long_press_data)
        }
        else {
            upstream!!.onMediaItemLongClicked(item, long_press_data)
        }
    }

    fun onMediaItemLongClicked(item: MediaItem, queue_index: Int) {
        onMediaItemLongClicked(item, LongPressMenuData(item, multiselect_key = queue_index))
    }

    open fun openMediaItem(
        item: MediaItem,
        from_current: Boolean = false,
        replace_current: Boolean = false,
        browse_params: BrowseParamsData? = null,
    ) { upstream!!.openMediaItem(
        item,
        from_current,
        replace_current,
        browse_params
    ) }

    open fun onPlayActionOccurred() { upstream!!.onPlayActionOccurred() }
    open fun playMediaItem(item: MediaItem, shuffle: Boolean = false, at_index: Int = 0) { upstream!!.playMediaItem(item, shuffle, at_index) }
    open fun playPlaylist(playlist: Playlist, from_index: Int = 0) { upstream!!.playPlaylist(playlist, from_index) }
    open fun openViewMorePage(browse_id: String, title: String?) { upstream!!.openViewMorePage(browse_id, title) }
    open fun openNowPlayingPlayerOverlayMenu(menu: PlayerOverlayMenu? = null) { upstream!!.openNowPlayingPlayerOverlayMenu(menu) }

    open fun showLongPressMenu(data: LongPressMenuData) { upstream!!.showLongPressMenu(data) }
    fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }

    open fun hideLongPressMenu() { upstream!!.hideLongPressMenu() }

    fun onSongDownloadRequested(song: Song, always_show_options: Boolean = false, onCompleted: DownloadRequestCallback? = null) { onSongDownloadRequested(listOf(song), always_show_options, onCompleted) }
    open fun onSongDownloadRequested(songs: List<Song>, always_show_options: Boolean = false, onCompleted: DownloadRequestCallback? = null) { upstream!!.onSongDownloadRequested(songs, always_show_options, onCompleted) }
}
