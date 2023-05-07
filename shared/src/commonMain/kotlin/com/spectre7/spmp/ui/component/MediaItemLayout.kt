package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.beust.klaxon.Json
import com.spectre7.spmp.api.*
import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.rememberImagePainter
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import okhttp3.Request

data class MediaItemLayout(
    val title: LocalisedYoutubeString?,
    val subtitle: LocalisedYoutubeString?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_source: ThumbnailSource? = null,
    val thumbnail_item_type: MediaItem.Type? = null,
    var view_more: ViewMore? = null,
    var continuation: Continuation? = null,
    @Json(ignored = true)
    var itemSizeProvider: @Composable () -> DpSize = { DpSize(100.dp, 130.dp) }
) {
    init {
        title?.getString()
        subtitle?.getString()
    }

    enum class Type {
        GRID,
        ROW,
        LIST,
        NUMBERED_LIST,
        CARD;

        @Composable
        fun Layout(layout: MediaItemLayout, playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
            when (this) {
                GRID -> MediaItemGrid(layout, playerProvider, modifier)
                ROW -> MediaItemGrid(layout, playerProvider, modifier, 1)
                LIST -> MediaItemList(layout, false, playerProvider, modifier)
                NUMBERED_LIST -> MediaItemList(layout, true, playerProvider, modifier)
                CARD -> MediaItemCard(layout, playerProvider, modifier)
            }
        }
    }

    @Composable
    fun Layout(playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
        type!!.Layout(this, playerProvider, modifier)
    }

    class Continuation(var token: String, val type: Type, val id: String? = null) {
        enum class Type { SONG, PLAYLIST }

        init {
            if (type == Type.SONG) {
                require(id != null)
            }
        }

        fun loadContinuation(filters: List<RadioModifier> = emptyList()): Result<Pair<List<MediaItem>, String?>> {
            return when (type) {
                Type.SONG -> loadSongContinuation(filters)
                Type.PLAYLIST -> loadPlaylistContinuation()
            }
        }

        fun update(token: String) {
            this.token = token
        }

        private fun loadSongContinuation(filters: List<RadioModifier>): Result<Pair<List<MediaItem>, String?>> {
            val result = getSongRadio(id!!, token, filters)
            return result.fold(
                { Result.success(Pair(it.items, it.continuation)) },
                { Result.failure(it) }
            )
        }

        private fun loadPlaylistContinuation(): Result<Pair<List<MediaItem>, String?>> {
            val request = Request.Builder()
                .ytUrl("/youtubei/v1/browse?ctoken=$token&continuation=$token&type=next")
                .addYtHeaders()
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

    data class ViewMore(
        val list_page_url: String? = null,
        val media_item: MediaItem? = null,
        val action: (() -> Unit)? = null,

        var layout_type: Type? = null,
        val browse_params: String? = null
    ) {
        var layout: MediaItemLayout? by mutableStateOf(null)
        init { check(list_page_url != null || media_item != null || action != null) }

        fun loadLayout(): Result<MediaItemLayout> {
            check(media_item != null)
            check(browse_params != null)

            val result = loadBrowseId(media_item.id, browse_params)
            if (result.isFailure) {
                return result.cast()
            }

            layout = result.getOrThrow().single()
            return Result.success(layout!!)
        }
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
        return if (thumbnail_item_type == MediaItem.Type.ARTIST) CircleShape else RectangleShape
    }

    @Composable
    fun TitleBar(
        playerProvider: () -> PlayerViewContext,
        modifier: Modifier = Modifier,
        font_size: TextUnit? = null
    ) {
        if (thumbnail_source == null && title == null && subtitle == null && view_more == null) {
            return
        }

        val title_string: String? = remember { title?.getString() }
        val subtitle_string: String? = remember { subtitle?.getString() }

        Row(
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val thumbnail_url = thumbnail_source?.getThumbUrl(MediaItem.ThumbnailQuality.LOW)
            if (thumbnail_url != null) {
                Image(
                    rememberImagePainter(thumbnail_url), 
                    null,
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(getThumbShape())
                )
            }

            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                if (subtitle_string != null) {
                    WidthShrinkText(subtitle_string, style = MaterialTheme.typography.titleSmall.copy(color = Theme.current.on_background))
                }

                if (title_string != null) {
                    WidthShrinkText(
                        title_string,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            style.copy(
                                color = Theme.current.on_background,
                                fontSize = font_size ?: style.fontSize
                            )
                        }
                    )
                }
            }

            view_more?.also { view_more ->
                OutlinedButton(
                    {
                        if (view_more.media_item != null) {
                            playerProvider().openMediaItem(view_more.media_item, this@MediaItemLayout)
                        }
                        else if (view_more.list_page_url != null) {
                            TODO(view_more.list_page_url)
                        }
                        else if (view_more.action != null) {
                            view_more.action.invoke()
                        }
                        else {
                            throw NotImplementedError(view_more.toString())
                        }
                    }
                ) {
                    Text(getStringTODO("More"))
                }
            }
        }
    }
}

@Composable
fun LazyMediaItemLayoutColumn(
    layoutsProvider: () -> List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    layout_modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    topContent: (LazyListScope.() -> Unit)? = null,
    onContinuationRequested: (() -> Unit)? = null,
    loading_continuation: Boolean = false,
    continuation_alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    scroll_state: LazyListState = rememberLazyListState(),
    scroll_enabled: Boolean = true,
    spacing: Dp = 0.dp,
    layoutItem: LazyListScope.(layout: MediaItemLayout, i: Int, showLayout: LazyListScope.(MediaItemLayout) -> Unit) -> Unit = { layout, i, showLayout ->
        showLayout(this, layout)
    },
    getType: ((MediaItemLayout) -> MediaItemLayout.Type)? = null
) {
    val layouts = layoutsProvider()
    require(getType != null || layouts.all { it.type != null })

    LazyColumn(
        modifier,
        state = scroll_state,
        contentPadding = padding,
        userScrollEnabled = scroll_enabled
    ) {
        topContent?.invoke(this)

        for (layout in layouts.withIndex()) {
            if (layout.value.items.isEmpty()) {
                continue
            }

            layoutItem(
                this,
                layout.value,
                layout.index,
                { layout ->
                    item {
                        val type = getType?.invoke(layout) ?: layout.type!!
                        type.Layout(layout, playerProvider, layout_modifier)
                    }
                    item { Spacer(Modifier.height(spacing)) }
                }
            )

//            when (val type = getType?.invoke(layout) ?: layout.type!!) {
//                MediaItemLayout.Type.LIST, MediaItemLayout.Type.NUMBERED_LIST -> {
//                    item {
//                        layout.TitleBar(playerProvider)
//                    }
//                    items(layout.items.size) { index ->
//                        val item = layout.items[index]
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            if (type == MediaItemLayout.Type.NUMBERED_LIST) {
//                                Text((index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
//                            }
//
//                            Column {
//                                item.PreviewLong(MediaItem.PreviewParams(playerProvider, content_colour = Theme.current.on_background_provider))
//                            }
//                        }
//                    }
//                }
//                else -> item { type.Layout(layout, playerProvider, layout_modifier) }
//            }
        }

        item {
            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = continuation_alignment) {
                    if (data.second) {
//                        CircularProgressIndicator(color = Theme.current.on_background)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    layout: MediaItemLayout,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    val item: MediaItem = layout.items.single()
    var accent_colour: Color? by remember { mutableStateOf(null) }

    val shape = RoundedCornerShape(16.dp)
    val long_press_menu_data = remember (item) {
        when (item) {
            is Song -> getSongLongPressMenuData(item, shape)
            is Artist -> getArtistLongPressMenuData(item)
            else -> LongPressMenuData(item, shape)
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
                    playerProvider().onMediaItemClicked(item)
                },
                onLongClick = {
                    playerProvider().showLongPressMenu(long_press_menu_data)
                }
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            layout.TitleBar(playerProvider, Modifier.fillMaxWidth().weight(1f))

            Text(if (item is Playlist) item.playlist_type.getReadable(false) else item.type.getReadable(false))

            Icon(
                when (item) {
                    is Song -> Icons.Filled.MusicNote
                    is Artist -> Icons.Filled.Person
                    is Playlist -> {
                        when (item.playlist_type) {
                            Playlist.PlaylistType.PLAYLIST, null -> Icons.Filled.PlaylistPlay
                            Playlist.PlaylistType.ALBUM -> Icons.Filled.Album
                            Playlist.PlaylistType.AUDIOBOOK -> Icons.Filled.Book
                            Playlist.PlaylistType.RADIO -> Icons.Filled.Radio
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
            item.Thumbnail(
                MediaItem.ThumbnailQuality.HIGH,
                Modifier
                    .longPressMenuIcon(long_press_menu_data)
                    .size(100.dp),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .background(accent_colour ?: Theme.current.accent, shape)
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                WidthShrinkText(
                    item.title!!,
                    style = LocalTextStyle.current.copy(color = (accent_colour ?: Theme.current.accent).getContrasted())
                )
                item.artist?.PreviewLong(MediaItem.PreviewParams(
                    playerProvider,
                    contentColour = { (accent_colour ?: Theme.current.accent).getContrasted() }
                ))
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                { playerProvider().playMediaItem(item) },
                Modifier.fillMaxWidth(),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour ?: Theme.current.vibrant_accent,
                    contentColor = (accent_colour ?: Theme.current.vibrant_accent).getContrasted()
                )
            ) {
                Text(getString(when (item.type) {
                    MediaItem.Type.SONG -> "media_play"
                    MediaItem.Type.ARTIST -> "artist_chip_play"
                    MediaItem.Type.PLAYLIST -> "playlist_chip_play"
                }))
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    rows: Int? = null,
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    val row_count = rows ?: if (layout.items.size <= 3) 1 else 2
    val item_spacing = Arrangement.spacedBy(15.dp)
    val item_size = layout.itemSizeProvider()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        layout.TitleBar(playerProvider)

        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.height(item_size.height * row_count),
            horizontalArrangement = item_spacing,
            verticalArrangement = item_spacing
        ) {
            startContent?.invoke(this)

            items(layout.items.size, { layout.items[it].id }) {
                layout.items[it].PreviewSquare(MediaItem.PreviewParams(
                    playerProvider,
                    Modifier.size(item_size),
                    contentColour = Theme.current.on_background_provider
                ))
//                    if(animate?.value == true) {
//                        LaunchedEffect(Unit) {
//                            animate.value = false
//                        }
//
//                        var visible by remember { mutableStateOf(false) }
//                        LaunchedEffect(visible) {
//                            visible = true
//                        }
//                        AnimatedVisibility(
//                            visible,
//                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
//                            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
//                        ) {
//                            item.PreviewSquare(MediaItem.PreviewParams(
//                                playerProvider, content_colour = Theme.current.on_background_provider
//                            ))
//                        }
//                    }
//                    else {
//                    }
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
        layout.TitleBar(playerProvider, Modifier.padding(bottom = 5.dp))

        for (item in layout.items.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (numbered) {
                    Text((item.index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                }

                Column {
                    item.value.PreviewLong(MediaItem.PreviewParams(playerProvider))
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
            else -> if ((item as Playlist).playlist_type == Playlist.PlaylistType.ALBUM) albums++ else playlists++
        }
    }

    return when (songs + videos + artists + playlists + albums) {
        0 -> throw IllegalStateException()
        videos ->             Pair("home_feed_rec_music_videos", null)
        artists ->            Pair("home_feed_rec_artists", null)
        songs + videos ->     Pair("home_feed_rec_songs", null)
        playlists ->          Pair("home_feed_rec_playlists", null)
        albums ->             Pair("home_feed_rec_albums", null)
        playlists + albums -> Pair("home_feed_rec_albums_playlists", null)
        else ->               Pair("home_feed_rec_general", null)
    }
}
