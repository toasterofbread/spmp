package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.modifier.horizontal
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MEDIAITEM_LIST_DEFAULT_SPACING_DP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@Composable
fun LocalArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player = LocalPlayerState.current
    val downloads by rememberSongDownloads()
    var songs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(artist, downloads) {
        songs = downloads.mapNotNull { download ->
            if (download.progress != 1f) {
                return@mapNotNull null
            }

            val song_artists: List<ArtistRef>? = download.song.Artists.get(player.database)
            if (song_artists?.any { it.id == artist.id } != true) {
                return@mapNotNull null
            }

            if (isMediaItemHidden(download.song, player.context)) {
                return@mapNotNull null
            }

            download.song
        }
    }

    multiselect_context?.CollectionToggleButton(
        songs.map { Pair(it, null) },
        show = false
    )

    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current
    CompositionLocalProvider(LocalPlayerClickOverrides provides
        LocalPlayerClickOverrides.current.copy(
            onClickOverride = { item, multiselect_key ->
                if (multiselect_key != null) {
                    player.withPlayer {
                        addMultipleToQueue(songs, clear = true)
                        seekToSong(multiselect_key)
                        player.onPlayActionOccurred()
                    }
                }
                else {
                    click_overrides.onMediaItemClicked(item, player)
                }
            }
        )
    ) {
        ArtistLayout(artist, modifier, previous_item, content_padding, multiselect_context) { accent_colour, content_modifier ->
            itemsIndexed(songs) { index, song ->
                MediaItemPreviewLong(
                    song,
                    content_modifier
                        .fillMaxWidth()
                        .padding(content_padding.horizontal)
                        .thenIf(index != 0) { padding(top = MEDIAITEM_LIST_DEFAULT_SPACING_DP.dp) },
                    multiselect_context = multiselect_context
                )
            }
        }
    }
}
