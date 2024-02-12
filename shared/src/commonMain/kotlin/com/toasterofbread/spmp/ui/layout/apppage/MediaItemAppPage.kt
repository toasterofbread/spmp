package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.SongRelatedPage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistPage

data class MediaItemAppPage(
    override val state: AppPageState,
    override val item: MediaItemHolder,
    private val browse_params: BrowseParamsData? = null
): AppPageWithItem() {

    private var previous_item: MediaItemHolder? by mutableStateOf(null)

    override fun onOpened(from_item: MediaItemHolder?) {
        super.onOpened(from_item)
        previous_item = from_item
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        val player: PlayerState = LocalPlayerState.current

        when (val item = item.item) {
            null -> close()
            is Playlist -> {
                val page: PlaylistPage = remember(item) {
                    PlaylistPage(
                        player.app_page_state,
                        item,
                        player
                    ).apply {
                        onOpened(previous_item)
                    }
                }

                with(page) {
                    Page(multiselect_context, modifier, content_padding, close)
                }
            }
            is Song ->
                SongRelatedPage(
                    item,
                    player.context.ytapi.SongRelatedContent,
                    modifier,
                    previous_item?.item,
                    content_padding,
                    close = close
                )
            else -> throw NotImplementedError(item::class.toString())
        }
    }
}
