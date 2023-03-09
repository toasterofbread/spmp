package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
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
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.YoutubeiBrowseResponse
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.WidthShrinkText
import com.spectre7.utils.getString
import okhttp3.Request

data class MediaItemLayout(
    val title: String?,
    val subtitle: String?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_source: ThumbnailSource? = null,
    val media_item_type: MediaItem.Type? = null,
    val view_more: ViewMore? = null,
    var continuation: Continuation? = null
) {
    enum class Type {
        GRID,
        LIST,
        NUMBERED_LIST
    }

    class Continuation(var token: String, val type: Type) {
        enum class Type { PLAYLIST }

        fun loadContinuation(): Result<Pair<List<MediaItem>, String?>> {
            return when (type) {
                Type.PLAYLIST -> loadPlaylistContinuation()
            }
        }

        fun update(token: String) {
            this.token = token
        }

        private fun loadPlaylistContinuation(): Result<Pair<List<MediaItem>, String?>> {
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?ctoken=$token&continuation=$token&type=next&key=${getString(R.string.yt_i_api_key)}")
                .headers(DataApi.getYTMHeaders())
                .post(DataApi.getYoutubeiRequestBody())
                .build()

            val result = DataApi.request(request)
            if (result.isFailure) {
                return result.cast()
            }

            val stream = result.getOrThrow().body!!.charStream()
            val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(stream)!!
            stream.close()

            val shelf = parsed.continuationContents!!.musicPlaylistShelfContinuation!!
            return Result.success(Pair(shelf.contents.mapNotNull { it.toMediaItem() }, shelf.continuations?.firstOrNull()?.nextContinuationData?.continuation))
        }

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

    @Json(ignored = true)
    var loading_continuation: Boolean by mutableStateOf(false)
        private set

    @Synchronized
    fun loadContinuation(): Result<Any> {
        check(continuation != null)

        loading_continuation = true
        val result = continuation!!.loadContinuation()
        loading_continuation = false

        if (result.isFailure) {
            return result
        }

        val (new_items, cont_token) = result.getOrThrow()
        items += new_items

        if (cont_token == null) {
            continuation = null
        }
        else {
            continuation!!.update(cont_token)
        }

        return Result.success(Unit)
    }

    private fun getThumbShape(): Shape {
        return if (media_item_type == MediaItem.Type.ARTIST) CircleShape else RectangleShape
    }

    @Composable
    fun TitleBar(playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val thumbnail_url = thumbnail_source?.getThumbUrl(MediaItem.ThumbnailQuality.LOW)
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
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.copy(color = Theme.current.on_background)
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
    modifier: Modifier = Modifier,
    onContinuationRequested: (() -> Unit)? = null,
    loading_continuation: Boolean = false,
    continuation_alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    require(layouts.all { it.type != null })

    Column(modifier) {
        for (layout in layouts) {
            when (layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, playerProvider)
                MediaItemLayout.Type.LIST -> MediaItemList(layout, false, playerProvider)
                MediaItemLayout.Type.NUMBERED_LIST -> MediaItemList(layout, true, playerProvider)
            }
        }

        Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = continuation_alignment) {
                if (data.second) {
                    CircularProgressIndicator(color = Theme.current.on_background)
                }
                else if (data.first != null) {
                    IconButton({ data.first!!.invoke() }) {
                        Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.current.on_background)
                    }
                }
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
    topContent: (LazyListScope.() -> Unit)? = null,
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
        contentPadding = padding,
        verticalArrangement = vertical_arrangement
    ) {
        topContent?.invoke(this)

        for (layout in layouts) {
            when (val type = getType?.invoke(layout) ?: layout.type!!) {
                MediaItemLayout.Type.GRID -> item { MediaItemGrid(layout, playerProvider) }
                MediaItemLayout.Type.LIST, MediaItemLayout.Type.NUMBERED_LIST -> {
                    item {
                        layout.TitleBar(playerProvider)
                    }
                    items(layout.items.size) { index ->
                        val item = layout.items[index]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (type == MediaItemLayout.Type.NUMBERED_LIST) {
                                Text((index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                            }

                            Column {
                                item.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
                            }
                        }
                    }
                }
            }
        }

        item {
            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = continuation_alignment) {
                    if (data.second) {
                        CircularProgressIndicator(color = Theme.current.on_background)
                    }
                    else if (data.first != null) {
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
