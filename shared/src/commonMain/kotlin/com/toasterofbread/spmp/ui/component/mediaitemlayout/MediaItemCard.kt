package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.isMediaItemHidden
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.getArtistLongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getPlaylistLongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getSongLongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.getContrasted

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false
) {
    val item: MediaItem = layout.items.single()
    if (apply_filter && isMediaItemHidden(item)) {
        return
    }

    var accent_colour: Color? by remember { mutableStateOf(null) }
    val player = LocalPlayerState.current

    val shape = RoundedCornerShape(16.dp)
    val long_press_menu_data = remember(item) {
        // TODO Move to MediaItem
        when (item) {
            is Song -> getSongLongPressMenuData(item, shape, multiselect_context = multiselect_context)
            is Artist -> getArtistLongPressMenuData(item, multiselect_context = multiselect_context)
            is Playlist -> getPlaylistLongPressMenuData(item, shape, multiselect_context = multiselect_context)
            else -> throw NotImplementedError(item.javaClass.name)
        }
    }

    LaunchedEffect(item.canGetThemeColour()) {
        if (accent_colour != null) {
            return@LaunchedEffect
        }

        if (item is Song && item.theme_colour != null) {
            accent_colour = item.theme_colour
            return@LaunchedEffect
        }

        if (item.canGetThemeColour()) {
            accent_colour = item.getDefaultThemeColour()
        }
    }

    Column(
        modifier
            .padding(10.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    player.onMediaItemClicked(item)
                },
                onLongClick = {
                    player.showLongPressMenu(long_press_menu_data)
                }
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            layout.TitleBar(Modifier.fillMaxWidth().weight(1f), multiselect_context = multiselect_context)

            Text(
                if (item is Playlist) item.playlist_type.getReadable(false)
                else item.type.getReadable(false),
                fontSize = 15.sp
            )

            Icon(
                when (item) {
                    is Song -> Icons.Filled.MusicNote
                    is Artist -> Icons.Filled.Person
                    is Playlist -> {
                        when (item.playlist_type) {
                            PlaylistType.PLAYLIST, null -> Icons.Filled.PlaylistPlay
                            PlaylistType.ALBUM -> Icons.Filled.Album
                            PlaylistType.AUDIOBOOK -> Icons.Filled.Book
                            PlaylistType.PODCAST -> Icons.Filled.Podcasts
                            PlaylistType.RADIO -> Icons.Filled.Radio
                        }
                    }

                    else -> throw NotImplementedError(item.type.toString())
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
            Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min)) {
                item.Thumbnail(
                    MediaItemThumbnailProvider.Quality.HIGH,
                    Modifier
                        .longPressMenuIcon(long_press_menu_data)
                        .size(100.dp),
                )

                multiselect_context?.SelectableItemOverlay(item, Modifier.fillMaxSize())
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .background(accent_colour ?: Theme.accent, shape)
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    item.title!!,
                    style = LocalTextStyle.current.copy(color = (accent_colour ?: Theme.accent).getContrasted()),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                item.artist?.PreviewLong(
                    MediaItemPreviewParams(
                        contentColour = { (accent_colour ?: Theme.accent).getContrasted() }
                    ))
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
                    containerColor = accent_colour ?: Theme.vibrant_accent,
                    contentColor = (accent_colour ?: Theme.vibrant_accent).getContrasted()
                )
            ) {
                Text(
                    getString(
                        when (item.type) {
                            MediaItemType.SONG -> "media_play"
                            MediaItemType.ARTIST -> "artist_chip_play"
                            MediaItemType.PLAYLIST_ACC, MediaItemType.PLAYLIST_LOC, MediaItemType.PLAYLIST_BROWSEPARAMS -> "playlist_chip_play"
                        }
                    )
                )
            }
        }
    }
}
