package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observeIsMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.db.rememberThemeColour
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.TitleBar
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.getThumbShape
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.artist_chip_play
import spmp.shared.generated.resources.media_play
import spmp.shared.generated.resources.playlist_chip_play

@Composable
fun MediaItemCard(
    layout: AppMediaItemLayout,
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false
) {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    val item: MediaItemData = remember(layout) { layout.items.first() }
    val item_hidden: Boolean by observeIsMediaItemHidden(item)

    if (apply_filter && item_hidden) {
        return
    }

    val accent_colour: Color? = item.rememberThemeColour()

    val shape: Shape = item.getType().getThumbShape()
    val long_press_menu_data: LongPressMenuData = remember(item, shape) {
        LongPressMenuData(item, shape, multiselect_context = multiselect_context)
    }

    Column(
        modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            layout.TitleBar(Modifier.fillMaxWidth().weight(1f), multiselect_context = multiselect_context)

            val playlist_type: State<PlaylistType?>? =
                if (item is RemotePlaylist) item.TypeOfPlaylist.observe(player.database)
                else null

            Text(
                if (item is RemotePlaylist) playlist_type?.value.getReadable(false)
                else stringResource(item.getType().getReadable(false)),
                fontSize = 15.sp,
                lineHeight = 15.sp
            )

            Icon(
                when (item) {
                    is Song -> Icons.Filled.MusicNote
                    is Artist -> Icons.Filled.Person
                    is RemotePlaylist -> {
                        when (playlist_type?.value) {
                            PlaylistType.PLAYLIST,
                            PlaylistType.LOCAL,
                            null -> Icons.Filled.PlaylistPlay
                            PlaylistType.ALBUM -> Icons.Filled.Album
                            PlaylistType.AUDIOBOOK -> Icons.Filled.Book
                            PlaylistType.PODCAST -> Icons.Filled.Podcasts
                            PlaylistType.RADIO -> Icons.Filled.Radio
                        }
                    }

                    else -> throw NotImplementedError(item::class.toString())
                },
                null,
                Modifier.size(15.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val density: Density = LocalDensity.current
            var row_height: Dp by remember { mutableStateOf(0.dp) }

            Box(
                Modifier
                    .heightIn(max = row_height)
                    .aspectRatio(1f)
                    .platformClickable(
                        onClick = {
                            click_overrides.onMediaItemClicked(item, player)
                        },
                        onAltClick = {
                            player.showLongPressMenu(long_press_menu_data)
                        }
                    )
            ) {
                item.Thumbnail(
                    ThumbnailProvider.Quality.HIGH,
                    Modifier.longPressMenuIcon(long_press_menu_data)
                )

                multiselect_context?.SelectableItemOverlay(item, Modifier.fillMaxSize())
            }

            Column(
                Modifier
                    .onSizeChanged {
                        row_height = with (density) { it.height.toDp() }
                    }
                    .fillMaxWidth()
                    .background(accent_colour ?: player.theme.accent, shape)
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val item_title: String? by item.observeActiveTitle()
                Text(
                    item_title ?: "",
                    style = LocalTextStyle.current.copy(color = (accent_colour ?: player.theme.accent).getContrasted()),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                if (item is MediaItem.WithArtists) {
                    val item_artists: List<Artist>? by item.Artists.observe(player.database)
                    item_artists?.firstOrNull()?.also { artist ->
                        MediaItemPreviewLong(artist, contentColour = { (accent_colour ?: player.theme.accent).getContrasted() })
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                { player.playMediaItem(item) },
                Modifier.fillMaxWidth(),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour ?: player.theme.vibrantAccent,
                    contentColor = (accent_colour ?: player.theme.vibrantAccent).getContrasted()
                )
            ) {
                Text(
                    when (item.getType()) {
                        MediaItemType.SONG -> stringResource(Res.string.media_play)
                        MediaItemType.ARTIST -> stringResource(Res.string.artist_chip_play)
                        MediaItemType.PLAYLIST_REM, MediaItemType.PLAYLIST_LOC -> stringResource(Res.string.playlist_chip_play)
                    }
                )
            }
        }
    }
}
