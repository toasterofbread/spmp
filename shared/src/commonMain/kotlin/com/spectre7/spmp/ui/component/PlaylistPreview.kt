package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.getReadable
import com.spectre7.spmp.platform.platformClickable
import com.spectre7.utils.setAlpha

@Composable
fun PlaylistPreviewSquare(
    playlist: Playlist,
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(playlist) {
        LongPressMenuData(
            playlist,
            RoundedCornerShape(10.dp)
        ) { } // TODO
    }

    Column(
        params.modifier
            .platformClickable(
                onClick = {
                    params.playerProvider().onMediaItemClicked(playlist)
                },
                onAltClick = {
                    params.playerProvider().showLongPressMenu(long_press_menu_data)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            playlist.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                params.contentColour
            )
        }

        Text(
            playlist.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistPreviewLong(
    playlist: Playlist, 
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(playlist) { LongPressMenuData(
        playlist,
        RoundedCornerShape(10.dp)
    ) { } // TODO
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    params.playerProvider().onMediaItemClicked(playlist)
                },
                onLongClick = {
                    params.playerProvider().showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        playlist.Thumbnail(
            MediaItem.ThumbnailQuality.LOW,
            Modifier
                .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                .size(40.dp),
            params.contentColour
        )

        Column(
            Modifier.padding(10.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                playlist.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                @Composable
                fun InfoText(text: String) {
                    Text(
                        text,
                        fontSize = 11.sp,
                        color = params.contentColour?.invoke() ?: Color.Unspecified.setAlpha(0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (params.show_type) {
                    InfoText(playlist.playlist_type.getReadable(false))
                }

                if (playlist.artist?.title != null) {
                    if (params.show_type) {
                        InfoText("\u2022")
                    }
                    InfoText(playlist.artist?.title!!)
                }
            }
        }
    }
}
