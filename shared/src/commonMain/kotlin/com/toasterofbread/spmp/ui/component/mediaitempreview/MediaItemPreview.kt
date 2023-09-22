package com.toasterofbread.spmp.ui.component.mediaitempreview

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePlayCount
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.rememberDownloadStatus
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

const val MEDIA_ITEM_PREVIEW_LONG_HEIGHT_DP: Float = 50f
const val MEDIA_ITEM_PREVIEW_SQUARE_FONT_SIZE_SP: Float = 12f
const val MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP: Float = 14f
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
    show_download_indicator: Boolean = true,
    font_size: TextUnit = MEDIA_ITEM_PREVIEW_SQUARE_FONT_SIZE_SP.sp,
    line_height: TextUnit = MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp,
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

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            val item_title: String? by item.observeActiveTitle()
            val max_lines = max_text_rows ?: 1

            Text(
                item_title ?: "",
                Modifier.fillMaxWidth().weight(1f),
                fontSize = font_size,
                color = contentColour?.invoke() ?: Color.Unspecified,
                lineHeight = line_height,
                maxLines = max_lines,
                overflow = if (max_lines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
                textAlign = TextAlign.Center
            )

            val download_status: PlayerDownloadManager.DownloadStatus? =
                if (show_download_indicator) (item as? Song)?.rememberDownloadStatus() else null

            if (download_status != null) {
                Icon(
                    if (download_status.progress == 1f) Icons.Default.DownloadDone
                    else Icons.Default.Downloading,
                    null,
                    Modifier.alpha(0.5f).size(13.dp)
                )
            }
        }

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
    show_artist: Boolean = true,
    show_download_indicator: Boolean = true,
    title_lines: Int = 1,
    font_size: TextUnit = 15.sp,
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
    val player = LocalPlayerState.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .mediaItemPreviewInteraction(item, long_press_menu_data)
            .height(MEDIA_ITEM_PREVIEW_LONG_HEIGHT_DP.dp)
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, enable_long_press_menu)
                    .fillMaxSize(),
                getContentColour = contentColour
            )

            (multiselect_context ?: long_press_menu_data.multiselect_context)?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = long_press_menu_data.multiselect_key)
            }
        }

        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val item_title: String? by item.observeActiveTitle()
            Text(
                item_title ?: "",
                color = contentColour?.invoke() ?: Color.Unspecified,
                fontSize = font_size,
                lineHeight = font_size,
                maxLines = title_lines,
                overflow = TextOverflow.Clip
            )

            val artist_title: String? = if (show_artist) (item as? MediaItem.WithArtist)?.Artist?.observePropertyActiveTitle()?.value else null
            val extra_info = getExtraInfo?.invoke() ?: emptyList()
            val download_status: PlayerDownloadManager.DownloadStatus? =
                if (show_download_indicator) (item as? Song)?.rememberDownloadStatus() else null

            if (download_status != null || show_play_count || show_type || extra_info.isNotEmpty() || artist_title != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var text_displayed = false

                    @Composable
                    fun InfoText(text: String) {
                        if (text_displayed) {
                            InfoText(INFO_SPLITTER, contentColour)
                        }
                        text_displayed = true
                        InfoText(text, contentColour)
                    }

                    if (download_status != null) {
                        text_displayed = true
                        Icon(
                            if (download_status.progress == 1f) Icons.Default.DownloadDone
                            else Icons.Default.Downloading,
                            null,
                            Modifier.alpha(0.5f).size(13.dp)
                        )
                    }

                    if (show_play_count) {
                        val play_count = item.observePlayCount(player.context)
                        InfoText(
                            getString("mediaitem_play_count_\$x_short")
                                .replace("\$x", play_count.toString())
                        )
                    }

                    if (show_type) {
                        InfoText(item.getType().getReadable(false))
                    }

                    for (info in extra_info) {
                        InfoText(info)
                    }

                    if (artist_title != null) {
                        InfoText(artist_title)
                    }
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
        fontSize = 12.sp,
        color = contentColour?.invoke() ?: Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
