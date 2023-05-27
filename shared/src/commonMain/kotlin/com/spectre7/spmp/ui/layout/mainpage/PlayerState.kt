package com.spectre7.spmp.ui.layout.mainpage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.LongPressMenuData
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.utils.indexOfOrNull
import java.net.URI
import java.net.URISyntaxException

open class PlayerState(
    private val onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val upstream: PlayerState? = null
) {
    open val np_theme_mode: ThemeMode get() = upstream!!.np_theme_mode
    open val overlay_page: Triple<OverlayPage, Any?, MediaItem?>? get() = upstream!!.overlay_page
    open val bottom_padding: Dp get() = upstream!!.bottom_padding
    open val pill_menu: PillMenu get() = upstream!!.pill_menu
    open val main_multiselect_context: MediaItemMultiSelectContext get() = upstream!!.main_multiselect_context

    fun copy(onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null, onLongClickedOverride: ((item: MediaItem) -> Unit)? = null): PlayerState {
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

                PlayerServiceHost.instance!!.interactService {
                    openMediaItem(Artist.fromId(channel_id))
                }
            }
            "watch" -> {
                val v_start = (uri.query.indexOfOrNull("v=") ?: return failure("'v' query parameter not found")) + 2
                val v_end = uri.query.indexOfOrNull("&", v_start) ?: uri.query.length

                PlayerServiceHost.instance!!.interactService {
                    playMediaItem(Song.fromId(uri.query.substring(v_start, v_end)))
                }
            }
            else -> return failure("Uri path not implemented")
        }

        return Result.success(Unit)
    }

    @Composable
    open fun nowPlayingTopOffset(base: Modifier): Modifier = upstream!!.nowPlayingTopOffset(base)

    open fun setOverlayPage(page: OverlayPage?, data: Any? = null, from_current: Boolean = false) { upstream!!.setOverlayPage(page, data, from_current) }

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

    open fun openMediaItem(item: MediaItem, from_current: Boolean = false) { upstream!!.openMediaItem(item, from_current) }
    open fun playMediaItem(item: MediaItem, shuffle: Boolean = false) { upstream!!.playMediaItem(item, shuffle) }

    open fun openViewMoreURL(url: String) { upstream!!.openViewMoreURL(url) }

    open fun onMediaItemPinnedChanged(item: MediaItem, pinned: Boolean) { upstream!!.onMediaItemPinnedChanged(item, pinned) }

    open fun showLongPressMenu(data: LongPressMenuData) { upstream!!.showLongPressMenu(data) }
    fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }

    open fun hideLongPressMenu() { upstream!!.hideLongPressMenu() }
}
