package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.rememberSongDownloads
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MEDIAITEM_LIST_DEFAULT_SPACING_DP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.common.thenIf
import com.toasterofbread.utils.modifier.horizontal

@Composable
fun LocalArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player = LocalPlayerState.current
    val downloads = rememberSongDownloads()
    var songs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(artist, downloads) {
        songs = downloads.mapNotNull { download ->
            if (download.progress != 1f) {
                return@mapNotNull null
            }

            val song_artist: ArtistRef? = download.song.Artist.get(player.database)
            if (song_artist?.id != artist.id) {
                return@mapNotNull null
            }

            if (isMediaItemHidden(download.song, player.database)) {
                return@mapNotNull null
            }

            download.song
        }
    }

    ArtistLayout(artist, modifier, previous_item, content_padding, multiselect_context) { accent_colour, show_info, content_modifier ->
        itemsIndexed(songs) { index, song ->
            MediaItemPreviewLong(
                song,
                content_modifier
                    .fillMaxWidth()
                    .padding(content_padding.horizontal)
                    .thenIf(index != 0) { padding(top = MEDIAITEM_LIST_DEFAULT_SPACING_DP.dp) }
            )
        }
    }
}
