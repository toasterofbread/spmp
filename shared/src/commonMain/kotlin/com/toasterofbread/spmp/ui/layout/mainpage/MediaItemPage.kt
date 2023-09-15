package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.layout.SongRelatedPage
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistPage
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistPage

data class MediaItemPage(private val holder: MediaItemHolder, private val browse_params: BrowseParamsData? = null): PlayerOverlayPage {
    override fun getItem(): MediaItem? = holder.item

    @Composable
    override fun Page(previous_item: MediaItem?, close: () -> Unit) {
        val player = LocalPlayerState.current

        when (val item = holder.item) {
            null -> close()
            is Playlist -> {
                val page = remember(item) {
                    PlaylistPage(
                        item,
                        player
                    )
                }
                page.Page(previous_item?.item, close)
            }
            is Artist -> ArtistPage(
                item,
                previous_item = previous_item?.item,
                content_padding = getContentPadding(),
                browse_params = browse_params?.let { params ->
                    Pair(params, player.context.ytapi.ArtistsWithParams)
                }
            )
            is Song -> SongRelatedPage(
                item,
                player.context.ytapi.SongRelatedContent,
                Modifier.fillMaxSize(),
                previous_item?.item,
                getContentPadding(),
                close = close
            )
            else -> throw NotImplementedError(item::class.toString())
        }
    }
}
