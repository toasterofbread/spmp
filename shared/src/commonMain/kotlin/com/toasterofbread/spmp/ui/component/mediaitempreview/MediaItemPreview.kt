package com.toasterofbread.spmp.ui.component.mediaitempreview

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.thenIf
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.formatArtistTitles
import com.toasterofbread.spmp.model.mediaitem.db.observePlayCount
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitles
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberDownloadStatus
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.`mediaitem_play_count_$x_short`

val MEDIA_ITEM_PREVIEW_SQUARE_FONT_SIZE_SP: Float
    @Composable get() = if (FormFactor.observe().value.is_large) 15f else 12f
val MEDIA_ITEM_PREVIEW_SQUARE_DEFAULT_MAX_LINES: Int
    @Composable get() = if (FormFactor.observe().value.is_large) 2 else 1

const val MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP: Float = 20f
private const val INFO_SPLITTER: String = "\u2022"

fun MediaItem.getLongPressMenuData(
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    getTitle: (@Composable () -> String?)? = null,
    queue_index: Int? = null
): LongPressMenuData =
    LongPressMenuData(
        this,
        getType().getThumbShape(),
        multiselect_context = multiselect_context,
        multiselect_key = multiselect_key,
        getTitle = getTitle,
        queue_index = queue_index
    )

@Composable
fun MediaItem.loadIfLocalPlaylist(): MediaItem? {
    val context: AppContext = LocalPlayerState.current.context
    val state: MutableState<MediaItem?> = remember { mutableStateOf(if (this !is LocalPlaylistRef) this else null) }

    LaunchedEffect(this) {
        val item: MediaItem = this@loadIfLocalPlaylist

        if (item !is LocalPlaylistRef) {
            state.value = item
            return@LaunchedEffect
        }

        state.value = null
        state.value = item.getLocalPlaylistFile(context)?.let { PlaylistFileConverter.loadFromFile(it, context) }
    }

    return state.value
}

@Composable
fun MediaItemPreviewSquare(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentColour: (() -> Color)? = null,
    enable_long_press_menu: Boolean = true,
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    getTitle: (@Composable () -> String?)? = null,
    max_text_rows: Int? = null,
    show_download_indicator: Boolean = true,
    font_size: TextUnit = MEDIA_ITEM_PREVIEW_SQUARE_FONT_SIZE_SP.sp,
    line_height: TextUnit = MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp,
    long_press_menu_data: LongPressMenuData =
        remember(item, multiselect_context, multiselect_key, getTitle) {
            item.getLongPressMenuData(
                multiselect_context,
                multiselect_key,
                getTitle
            )
        },
    apply_size: Boolean = true
) {
    val loaded_item: MediaItem? = item.loadIfLocalPlaylist()
    if (loaded_item == null) {
        return
    }

    val player: PlayerState = LocalPlayerState.current
    val max_lines: Int = max_text_rows ?: MEDIA_ITEM_PREVIEW_SQUARE_DEFAULT_MAX_LINES

    Column(
        modifier
            .thenIf(apply_size) {
                size(
                    getDefaultMediaItemPreviewSize(false)
                    + DpSize(0.dp, getMediaItemPreviewSquareAdditionalHeight(max_lines, line_height))
                )
            }
            .mediaItemPreviewInteraction(loaded_item, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxWidth().longPressMenuIcon(long_press_menu_data, enable_long_press_menu), contentAlignment = Alignment.Center) {
            loaded_item.Thumbnail(
                ThumbnailProvider.Quality.LOW,
                Modifier.aspectRatio(1f),
                getContentColour = contentColour
            )

            multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(
                    loaded_item,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    key = long_press_menu_data.multiselect_key
                )
            }

            if (loaded_item is Playlist) {
                Icon(
                    Icons.Default.QueueMusic,
                    null,
                    Modifier
                        .size(25.dp)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val is_explicit: Boolean? by (loaded_item as? Song)?.Explicit?.observe(player.database)
            if (is_explicit == true) {
                Icon(Icons.Default.Explicit, null, Modifier.size(15.dp).alpha(0.5f))
            }

            val item_title: String? by loaded_item.observeActiveTitle()

            Text(
                item_title ?: "",
                Modifier.fillMaxWidth().weight(1f),
                fontSize = font_size,
                color = contentColour?.invoke() ?: Color.Unspecified,
                lineHeight = line_height,
                maxLines = max_lines,
                softWrap = max_lines > 1,
                overflow = TextOverflow.Clip,
                textAlign = if (FormFactor.observe().value.is_large && item !is Artist) TextAlign.Start else TextAlign.Center
            )

            val download_status: DownloadStatus? by (loaded_item as? Song)?.rememberDownloadStatus()

            if (show_download_indicator && download_status != null) {
                Icon(
                    if (download_status?.isCompleted() == true) Icons.Default.DownloadDone
                    else Icons.Default.Downloading,
                    null,
                    Modifier.alpha(0.5f).size(13.dp).padding(start = 5.dp)
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
    font_size: TextUnit = if (FormFactor.observe().value.is_large) 20.sp else 15.sp,
    getExtraInfo: (@Composable () -> List<String>)? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    multiselect_key: Int? = null,
    queue_index: Int? = null,
    getTitle: (@Composable () -> String?)? = null,
    long_press_menu_data: LongPressMenuData =
        remember(item, multiselect_context, multiselect_key, getTitle, queue_index) {
            item.getLongPressMenuData(
                multiselect_context,
                multiselect_key,
                getTitle,
                queue_index
            )
        }
) {
    val loaded_item: MediaItem? = item.loadIfLocalPlaylist()
    if (loaded_item == null) {
        return
    }

    val player = LocalPlayerState.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .size(getDefaultMediaItemPreviewSize(true))
            .mediaItemPreviewInteraction(loaded_item, long_press_menu_data)
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
            loaded_item.Thumbnail(
                ThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, enable_long_press_menu)
                    .fillMaxSize(),
                getContentColour = contentColour
            )

            (multiselect_context ?: long_press_menu_data.multiselect_context)?.also { ctx ->
                ctx.SelectableItemOverlay(loaded_item, Modifier.fillMaxSize(), key = long_press_menu_data.multiselect_key)
            }
        }

        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            val item_title: String? by loaded_item.observeActiveTitle()
            Text(
                item_title ?: "",
                color = contentColour?.invoke() ?: Color.Unspecified,
                fontSize = font_size,
//                lineHeight = font_size,
                maxLines = title_lines,
                softWrap = title_lines > 1,
                overflow = TextOverflow.Clip
            )

            val artist_titles: List<String?>? = if (show_artist) (loaded_item as? MediaItem.WithArtists)?.Artists?.observePropertyActiveTitles() else null
            val extra_info: List<String> = getExtraInfo?.invoke() ?: emptyList()
            val download_status: DownloadStatus? by (loaded_item as? Song)?.rememberDownloadStatus()
            val is_explicit: Boolean? by (loaded_item as? Song)?.Explicit?.observe(player.database)

            if (
                (show_download_indicator && download_status != null)
                || show_play_count
                || show_type
                || extra_info.isNotEmpty()
                || artist_titles?.any { it !=  null } == true
                || is_explicit == true
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (is_explicit == true) {
                        Icon(
                            Icons.Default.Explicit,
                            null,
                            Modifier.size(15.dp).alpha(0.5f),
                            tint = contentColour?.invoke() ?: LocalContentColor.current
                        )
                    }

                    var text_displayed = false

                    @Composable
                    fun InfoText(text: String?) {
                        if (text == null) {
                            return
                        }

                        if (text_displayed) {
                            InfoText(INFO_SPLITTER, contentColour)
                        }
                        text_displayed = true
                        InfoText(text, contentColour)
                    }

                    if (show_download_indicator && download_status != null) {
                        text_displayed = true
                        Icon(
                            if (download_status?.isCompleted() == true) Icons.Default.DownloadDone
                            else Icons.Default.Downloading,
                            null,
                            Modifier.alpha(0.5f).size(13.dp)
                        )
                    }

                    if (show_play_count) {
                        val play_count: Int? = loaded_item.observePlayCount(player.context)
                        InfoText(
                            stringResource(Res.string.`mediaitem_play_count_$x_short`)
                                .replace("\$x", play_count?.toString() ?: "?")
                        )
                    }

                    if (show_type) {
                        InfoText(stringResource(loaded_item.getType().getReadable(false)))
                    }

                    for (info in extra_info) {
                        InfoText(info)
                    }

                    if (artist_titles != null) {
                        InfoText(formatArtistTitles(artist_titles, player.context))
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
        lineHeight = 12.sp,
        color = contentColour?.invoke() ?: Color.Unspecified,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}
