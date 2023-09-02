package com.toasterofbread.spmp.ui.component.mediaitempreview

import SpMp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.db.observePlayCount
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

const val MEDIA_ITEM_PREVIEW_LONG_HEIGHT: Float = 40f
private const val INFO_SPLITTER: String = "\u2022"

fun MediaItem.getLongPressMenuData(
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    getInfoText: (@Composable () -> String?)? = null
): LongPressMenuData = when (this) {
    is Song -> getSongLongPressMenuData(
        this,
        multiselect_key = multiselect_key,
        multiselect_context = multiselect_context,
        getInfoText = getInfoText
    )
    is Artist -> getArtistLongPressMenuData(this, multiselect_context = multiselect_context)
    is Playlist -> getPlaylistLongPressMenuData(this, multiselect_context = multiselect_context)
    else -> throw NotImplementedError(this.getType().toString())
}

@Composable
fun MediaItemPreviewSquare(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentColour: (() -> Color)? = null,
    enable_long_press_menu: Boolean = true,
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    getInfoText: (@Composable () -> String?)? = null,
    max_text_rows: Int? = null,
    long_press_menu_data: LongPressMenuData =
        remember(item, multiselect_context, multiselect_key, getInfoText) {
            item.getLongPressMenuData(
                multiselect_context,
                multiselect_key,
                getInfoText
            )
        }
) {
    Column(
        modifier.mediaItemPreviewInteraction(item, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, enable_long_press_menu).aspectRatio(1f),
                getContentColour = contentColour
            )

            multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxWidth().aspectRatio(1f), key = long_press_menu_data.multiselect_key)
            }
        }

        val item_title: String? by item.Title.observe(SpMp.context.database)

        Text(
            item_title ?: "",
//            Modifier.fillMaxSize().weight(1f),
            fontSize = 12.sp,
            color = contentColour?.invoke() ?: Color.Unspecified,
            maxLines = max_text_rows ?: 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MediaItemPreviewLong(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentColour: (() -> Color)? = null,
    enable_long_press_menu: Boolean = true,
    show_type: Boolean = true,
    show_play_count: Boolean = false,
    getExtraInfo: (@Composable () -> List<String>)? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    getInfoText: (@Composable () -> String?)? = null,
    long_press_menu_data: LongPressMenuData =
        remember(item, multiselect_context, multiselect_key, getInfoText) {
            item.getLongPressMenuData(
                multiselect_context,
                multiselect_key,
                getInfoText
            )
        }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .mediaItemPreviewInteraction(item, long_press_menu_data)
            .height(MEDIA_ITEM_PREVIEW_LONG_HEIGHT.dp)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, enable_long_press_menu)
                    .size(MEDIA_ITEM_PREVIEW_LONG_HEIGHT.dp),
                getContentColour = contentColour
            )

            multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = long_press_menu_data.multiselect_key)
            }
        }

        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val item_title: String? by item.Title.observe(SpMp.context.database)
            Text(
                item_title ?: "",
                fontSize = 15.sp,
                color = contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                var text_displayed = false

                @Composable
                fun InfoText(text: String) {
                    if (text_displayed) {
                        InfoText(INFO_SPLITTER, contentColour)
                    }
                    text_displayed = true
                    InfoText(text, contentColour)
                }

                if (show_play_count) {
                    val play_count = item.observePlayCount(SpMp.context)
                    InfoText(
                        getString("mediaitem_play_count_\$x_short")
                            .replace("\$x", play_count.toString())
                    )
                }

                if (show_type) {
                    InfoText(item.getType().getReadable(false))
                }

                val extra_info = getExtraInfo?.invoke() ?: emptyList()
                for (info in extra_info) {
                    InfoText(info)
                }

                val artist_title: String? =
                    if (item is MediaItem.WithArtist)
                        item.Artist.observeOn(SpMp.context.database) {
                            it?.Title
                        }
                    else null

                if (artist_title != null) {
                    InfoText(artist_title)
                }
            }
        }
    }
}

@Composable
private fun InfoText(text: String, contentColour: (() -> Color)?) {
    Text(
        text,
        Modifier.alpha(0.5f),
        fontSize = 11.sp,
        color = contentColour?.invoke() ?: Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
