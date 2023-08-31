package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.ui.layout.SongRelatedPage
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistPage
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistPage

data class MediaItemPage(private val holder: MediaItemHolder, private val browse_params: String? = null): PlayerOverlayPage {
    override fun getItem(): MediaItem? = holder.item

    @Composable
    override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
        val player = LocalPlayerState.current

        when (val item = holder.item) {
            null -> close()
            is Playlist -> PlaylistPage(
                item,
                previous_item?.item,
                PaddingValues(top = player.context.getStatusBarHeight(), bottom = bottom_padding),
                close
            )
            is Artist -> ArtistPage(
                item,
                previous_item?.item,
                bottom_padding,
                browse_params?.let { params ->
                    Pair(params, player.context.ytapi.ArtistsWithParams)
                },
                close
            )
            is Song -> SongRelatedPage(
                item,
                player.context.ytapi.SongRelatedContent,
                Modifier.fillMaxSize(),
                previous_item?.item,
                PaddingValues(
                    top = player.context.getStatusBarHeight(),
                    bottom = bottom_padding,
                    start = player.getDefaultHorizontalPadding(),
                    end = player.getDefaultHorizontalPadding()
                ),
                close = close
            )
            else -> throw NotImplementedError(item::class.toString())
        }
    }
}
