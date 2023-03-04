package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.beust.klaxon.Json
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.WidthShrinkText
import com.spectre7.utils.copy
import com.spectre7.utils.getString

data class MediaItemLayout(
    val title: String?,
    val subtitle: String?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_source: ThumbnailSource? = null,
    val media_item_type: MediaItem.Type? = null,
    val view_more: ViewMore? = null
) {
    enum class Type {
        GRID,
        LIST,
        NUMBERED_LIST
    }

    data class ViewMore(val list_page_url: String? = null, val media_item: MediaItem? = null) {
        init { check(list_page_url != null || media_item != null) }
    }

    class ThumbnailSource(val media_item: MediaItem? = null, val url: String? = null) {
        init {
            check(media_item != null || url != null)
        }

        fun getThumbUrl(quality: MediaItem.ThumbnailQuality): String? {
            return url ?: media_item?.getThumbUrl(quality)
        }

    }

    private fun getThumbShape(): Shape {
        return if (media_item_type == MediaItem.Type.ARTIST) CircleShape else RectangleShape
    }

    fun addItem(item: MediaItem) {
        if (items.any { it.id == item.id }) {
            return
        }
        items.add(item)
        return
    }

    @Composable
    fun TitleBar(playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val thumbnail_url = thumbnail_source?.getThumbUrl(MediaItem.ThumbnailQuality.LOW)
            println("$thumbnail_url | $thumbnail_source | ${thumbnail_source?.url}")
            if (thumbnail_url != null) {
                Image(
                    rememberAsyncImagePainter(thumbnail_url), title,
                    Modifier.fillMaxHeight().aspectRatio(1f).clip(getThumbShape())
                )
            }

            Column(Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.Center) {
                if (subtitle != null) {
                    WidthShrinkText(subtitle, style = MaterialTheme.typography.titleSmall.copy(color = Theme.current.on_background))
                }

                if (title != null) {
                    WidthShrinkText(
                        title,
                        MaterialTheme.typography.headlineMedium.copy(color = Theme.current.on_background),
                        Modifier.fillMaxWidth()//.weight(1f)
                    )
                }
            }

            if (view_more != null) {
                IconButton({
                    if (view_more.media_item != null) {
                        playerProvider().openMediaItem(view_more.media_item)
                    }
                    else if (view_more.list_page_url != null) {
                        TODO(view_more.list_page_url)
                    }
                    else {
                        throw NotImplementedError(view_more.toString())
                    }
                }, Modifier.fillMaxHeight().aspectRatio(1f)) {
                    Icon(Icons.Filled.MoreHoriz, null)
                }
            }
        }

    }

    companion object {
        @Composable
        fun ItemPreview(
            item: MediaItem,
            width: Dp,
            animate: MutableState<Boolean>?,
            playerProvider: () -> PlayerViewContext,
            modifier: Modifier = Modifier
        ) {
            Box(modifier.requiredWidth(width), contentAlignment = Alignment.Center) {
                if(animate?.value == true) {
                    LaunchedEffect(Unit) {
                        animate.value = false
                    }

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(visible) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
                    ) {
                        item.PreviewSquare(Theme.current.on_background_provider, playerProvider, true, Modifier)
                    }
                }
                else {
                    item.PreviewSquare(Theme.current.on_background_provider, playerProvider, true, Modifier)
                }
            }
        }
    }
}

fun Collection<MediaItemLayout>.removeInvalid() {
    for (layout in this) {
        layout.items.removeAll { !it.is_valid }
    }
}

@Composable
fun MediaItemLayoutColumn(
    layouts: List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    for (layout in layouts) {
        assert(layout.type != null)
    }

    Column(modifier) {
        for (layout in layouts) {
            when (layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, playerProvider)
                MediaItemLayout.Type.LIST -> MediaItemList(layout, false, playerProvider)
                MediaItemLayout.Type.NUMBERED_LIST -> MediaItemList(layout, true, playerProvider)
            }
        }
    }
}

@Composable
fun LazyMediaItemLayoutColumn(
    layouts: List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    onContinuationRequested: (() -> Unit)? = null,
    loading_continuation: Boolean = false,
    continuation_alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    scroll_state: LazyListState = rememberLazyListState(),
    vertical_arrangement: Arrangement.Vertical = Arrangement.Top,
    getType: ((MediaItemLayout) -> MediaItemLayout.Type)? = null
) {
    require(getType != null || layouts.all { it.type != null })

    LazyColumn(
        modifier,
        state = scroll_state,
        contentPadding = padding.copy(bottom = 0.dp),
        verticalArrangement = vertical_arrangement
    ) {
        items(layouts) { layout ->
            when (getType?.invoke(layout) ?: layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, playerProvider)
                MediaItemLayout.Type.LIST -> MediaItemList(layout, false, playerProvider)
                MediaItemLayout.Type.NUMBERED_LIST -> MediaItemList(layout, true, playerProvider)
            }
        }

        val bottom_padding = padding.calculateBottomPadding()
        if (bottom_padding != 0.dp) {
            item {
                Spacer(Modifier.height(bottom_padding))
            }
        }

        item {
            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                if (data.second) {
                    CircularProgressIndicator(color = Theme.current.on_background)
                }
                else if (data.first != null) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = continuation_alignment) {
                        IconButton({ data.first!!.invoke() }) {
                            Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.current.on_background)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    val row_count = if (layout.items.size <= 3) 1 else 2
    val item_width = 125.dp

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        layout.TitleBar(playerProvider)

        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.requiredHeight(item_width * row_count * 1.1f)
        ) {
            items(layout.items.size, { layout.items[it].id }) {
                MediaItemLayout.ItemPreview(layout.items[it], item_width, null, playerProvider, Modifier)
            }
        }
    }
}

@Composable
fun MediaItemList(
    layout: MediaItemLayout,
    numbered: Boolean,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        layout.TitleBar(playerProvider)

        for (item in layout.items.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (numbered) {
                    Text((item.index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                }

                Column {
                    item.value.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
                }
            }
        }
    }
}

fun List<MediaItem>.generateLayoutTitle(): Pair<String, String?> {
    check(isNotEmpty())

    var songs = 0
    var videos = 0
    var artists = 0
    var playlists = 0
    var albums = 0

    for (item in this) {
        when (item.type) {
            MediaItem.Type.SONG -> if ((item as Song).song_type == Song.SongType.VIDEO) videos++ else songs++
            MediaItem.Type.ARTIST -> artists++
            MediaItem.Type.PLAYLIST -> if ((item as Playlist).playlist_type == Playlist.PlaylistType.ALBUM) albums++ else playlists++
        }
    }

    return when (songs + videos + artists + playlists + albums) {
        0 -> throw IllegalStateException()
        videos ->             Pair(getString("おすすめのミュージックビデオ"), null)
        artists ->            Pair(getString("おすすめのアーティスト"), null)
        songs + videos ->     Pair(getString("おすすめの曲"), null)
        playlists ->          Pair(getString("おすすめのプレイリスト"), null)
        albums ->             Pair(getString("おすすめのアルバム"), null)
        playlists + albums -> Pair(getString("プレイリストとアルバム"), null)
        else ->               Pair(getString("おすすめ"), null)
    }
}
