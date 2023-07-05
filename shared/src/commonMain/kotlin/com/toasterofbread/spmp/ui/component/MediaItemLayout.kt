package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import SpMp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.beust.klaxon.Json
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.radio.getSongRadio
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.platform.composable.rememberImagePainter
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.modifier.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

fun getDefaultMediaItemPreviewSize(): DpSize = DpSize(100.dp, 130.dp)

data class MediaItemLayout(
    val title: LocalisedYoutubeString?,
    val subtitle: LocalisedYoutubeString?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_source: ThumbnailSource? = null,
    val thumbnail_item_type: MediaItemType? = null,
    var view_more: ViewMore? = null,
    var continuation: Continuation? = null,
    var square_item_max_text_rows: Int? = null,
    @Json(ignored = true)
    var itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() }
) {
    init {
        title?.getString()
        subtitle?.getString()
    }

    enum class Type {
        GRID,
        GRID_ALT,
        ROW,
        LIST,
        NUMBERED_LIST,
        CARD;

        @Composable
        fun Layout(
            layout: MediaItemLayout,
            modifier: Modifier = Modifier,
            multiselect_context: MediaItemMultiSelectContext? = null
        ) {
            when (this) {
                GRID -> MediaItemGrid(layout, modifier, multiselect_context = multiselect_context)
                GRID_ALT -> MediaItemGrid(layout, modifier, alt_style = true, multiselect_context = multiselect_context)
                ROW -> MediaItemGrid(layout, modifier, 1, multiselect_context = multiselect_context)
                LIST -> MediaItemList(layout, modifier, false, multiselect_context = multiselect_context)
                NUMBERED_LIST -> MediaItemList(layout, modifier, true, multiselect_context = multiselect_context)
                CARD -> MediaItemCard(layout, modifier, multiselect_context = multiselect_context)
            }
        }
    }

    @Composable
    fun Layout(modifier: Modifier = Modifier, multiselect_context: MediaItemMultiSelectContext? = null) {
        type!!.Layout(this, modifier, multiselect_context)
    }

    data class Continuation(var token: String, var type: Type, val param: Any? = null) {
        enum class Type {
            SONG, // param is the song's ID
            PLAYLIST, // param unused
            PLAYLIST_INITIAL // param is the amount of songs to omit from the beginning
        }

        init {
            if (type == Type.SONG) {
                require(param is String)
            }
            else if (type == Type.PLAYLIST_INITIAL) {
                require(param is Int)
            }
        }

        suspend fun loadContinuation(filters: List<RadioModifier> = emptyList()): Result<Pair<List<MediaItem>, String?>> {
            return when (type) {
                Type.SONG -> loadSongContinuation(filters)
                Type.PLAYLIST -> loadPlaylistContinuation(false)
                Type.PLAYLIST_INITIAL -> loadPlaylistContinuation(true)
            }
        }

        fun update(token: String) {
            this.token = token
            if (type == Type.PLAYLIST_INITIAL) {
                type = Type.PLAYLIST
            }
        }

        private suspend fun loadSongContinuation(filters: List<RadioModifier>): Result<Pair<List<MediaItem>, String?>> {
            val result = getSongRadio(param as String, token, filters)
            return result.fold(
                { Result.success(Pair(it.items, it.continuation)) },
                { Result.failure(it) }
            )
        }

        private suspend fun loadPlaylistContinuation(initial: Boolean): Result<Pair<List<MediaItem>, String?>> = withContext(Dispatchers.IO) {
            if (initial) {
                val playlist = AccountPlaylist.fromId(token)
                playlist.data.items?.also { items ->
                    return@withContext Result.success(Pair(
                        items.subList(param as Int, items.size - 1),
                        playlist.data.continuation?.token
                    ))
                }
            }

            val hl = SpMp.data_language
            val request = Request.Builder()
                .ytUrl(
                    if (initial) "/youtubei/v1/browse"
                    else "/youtubei/v1/browse?ctoken=$token&continuation=$token&type=next"
                )
                .addYtHeaders()
                .post(Api.getYoutubeiRequestBody(
                    if (initial) mapOf("browseId" to token)
                    else null
                ))
                .build()

            val result = Api.request(request)
            if (result.isFailure) {
                return@withContext result.cast()
            }

            val stream = result.getOrThrow().getStream()
            val parsed: YoutubeiBrowseResponse = try {
                Api.klaxon.parse(stream)!!
            }
            catch (e: Throwable) {
                return@withContext Result.failure(e)
            }
            finally {
                stream.close()
            }

            val shelf =
                if (initial) parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.single().musicPlaylistShelfRenderer!!
                else parsed.continuationContents!!.musicPlaylistShelfContinuation!!

            return@withContext Result.success(Pair(
                shelf.contents!!.withIndex().mapNotNull { item ->
                    if (item.index < param as Int) {
                        return@mapNotNull null
                    }
                    item.value.toMediaItem(hl)?.first
                },
                shelf.continuations?.firstOrNull()?.nextContinuationData?.continuation
            ))
        }
    }

    data class ViewMore(
        val list_page_browse_id: String? = null,
        val media_item: MediaItem? = null,
        val action: (() -> Unit)? = null,

        var layout_type: Type? = null,
        val browse_params: String? = null
    ) {
        var layout: MediaItemLayout? by mutableStateOf(null)
        
        init {
            check(list_page_browse_id != null || media_item != null || action != null) 
            check(browse_params == null || list_page_browse_id != null)
        }
    }

    class ThumbnailSource(val media_item: MediaItem? = null, val url: String? = null) {
        init {
            check(media_item != null || url != null)
        }

        fun getThumbUrl(quality: MediaItemThumbnailProvider.Quality): String? {
            return url ?: media_item?.getThumbUrl(quality)
        }
    }

    @Composable
    fun TitleBar(
        modifier: Modifier = Modifier,
        font_size: TextUnit? = null,
        multiselect_context: MediaItemMultiSelectContext? = null
    ) {
        TitleBar(items, title, subtitle, modifier, view_more, thumbnail_source, thumbnail_item_type, font_size, multiselect_context)
    }
}

private fun shouldShowTitleBar(
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    view_more: MediaItemLayout.ViewMore? = null,
    thumbnail_source: MediaItemLayout.ThumbnailSource? = null
): Boolean = thumbnail_source != null || title != null || subtitle != null || view_more != null

@Composable
private fun TitleBar(
    items: List<MediaItemHolder>,
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    modifier: Modifier = Modifier,
    view_more: MediaItemLayout.ViewMore? = null,
    thumbnail_source: MediaItemLayout.ThumbnailSource? = null,
    thumbnail_item_type: MediaItemType? = null,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    AnimatedVisibility(shouldShowTitleBar(title, subtitle, view_more, thumbnail_source)) {
        val title_string: String? = remember { title?.getString() }
        val subtitle_string: String? = remember { subtitle?.getString() }

        Row(
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val thumbnail_url = thumbnail_source?.getThumbUrl(MediaItemThumbnailProvider.Quality.LOW)
            if (thumbnail_url != null) {
                Image(
                    rememberImagePainter(thumbnail_url),
                    null,
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(if (thumbnail_item_type == MediaItemType.ARTIST) CircleShape else RectangleShape)
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

            Row {
                view_more?.also { view_more ->
                    val player = LocalPlayerState.current
                    IconButton(
                        {
                            if (view_more.media_item != null) {
                                player.openMediaItem(view_more.media_item, true)
                            }
                            else if (view_more.list_page_browse_id != null) {
                                if (view_more.browse_params != null) {
                                    player.openMediaItem(
                                        BrowseParamsPlaylist
                                            .fromId(view_more.list_page_browse_id, view_more.browse_params)
                                            .apply { editData {
                                                supplyTitle(title?.getString() ?: "")
                                            }}
                                    )
                                }
                                else {
                                    player.openViewMorePage(view_more.list_page_browse_id)
                                }
                            }
                            else if (view_more.action != null) {
                                view_more.action.invoke()
                            }
                            else {
                                throw NotImplementedError(view_more.toString())
                            }
                        }
                    ) {
                        Icon(Icons.Default.MoreHoriz, null)
                    }
                }

                multiselect_context?.CollectionToggleButton(items)
            }
        }
    }
}

@Composable
fun LazyMediaItemLayoutColumn(
    getLayouts: () -> List<MediaItemLayout>,
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
    multiselect_context: MediaItemMultiSelectContext? = null,
    getType: ((MediaItemLayout) -> MediaItemLayout.Type)? = null
) {
    val layouts = getLayouts()
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
                        type.Layout(layout, layout_modifier, multiselect_context)
                    }
                    item { Spacer(Modifier.height(spacing)) }
                }
            )

//            when (val type = getType?.invoke(layout) ?: layout.type!!) {
//                MediaItemLayout.Type.LIST, MediaItemLayout.Type.NUMBERED_LIST -> {
//                    item {
//                        layout.TitleBar()
//                    }
//                    items(layout.items.size) { index ->
//                        val item = layout.items[index]
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            if (type == MediaItemLayout.Type.NUMBERED_LIST) {
//                                Text((index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
//                            }
//
//                            Column {
//                                item.PreviewLong(MediaItem.PreviewParams(content_colour = Theme.current.on_background_provider))
//                            }
//                        }
//                    }
//                }
//                else -> item { type.Layout(layout, layout_modifier) }
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
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val item: MediaItem = layout.items.single()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    val player = LocalPlayerState.current

    val shape = RoundedCornerShape(16.dp)
    val long_press_menu_data = remember (item) {
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

            Text(if (item is Playlist) item.playlist_type.getReadable(false) else item.type.getReadable(false))

            Icon(
                when (item) {
                    is Song -> Icons.Filled.MusicNote
                    is Artist -> Icons.Filled.Person
                    is Playlist -> {
                        when (item.playlist_type) {
                            PlaylistType.PLAYLIST, null -> Icons.Filled.PlaylistPlay
                            PlaylistType.ALBUM -> Icons.Filled.Album
                            PlaylistType.AUDIOBOOK -> Icons.Filled.Book
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
                    .background(accent_colour ?: Theme.current.accent, shape)
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                    Text(
                        item.title!!,
                        style = LocalTextStyle.current.copy(color = (accent_colour ?: Theme.current.accent).getContrasted()),
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                item.artist?.PreviewLong(
                    MediaItemPreviewParams(
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
                { player.playMediaItem(item) },
                Modifier.fillMaxWidth(),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour ?: Theme.current.vibrant_accent,
                    contentColor = (accent_colour ?: Theme.current.vibrant_accent).getContrasted()
                )
            ) {
                Text(getString(when (item.type) {
                    MediaItemType.SONG -> "media_play"
                    MediaItemType.ARTIST -> "artist_chip_play"
                    MediaItemType.PLAYLIST_ACC, MediaItemType.PLAYLIST_LOC, MediaItemType.PLAYLIST_BROWSEPARAMS -> "playlist_chip_play"
                }))
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    rows: Int? = null,
    alt_style: Boolean = false,
    multiselect_context: MediaItemMultiSelectContext? = null,
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    MediaItemGrid(
        layout.items,
        modifier,
        rows,
        layout.title,
        layout.subtitle,
        layout.view_more,
        alt_style = alt_style,
        itemSizeProvider = layout.itemSizeProvider,
        multiselect_context = multiselect_context,
        startContent = startContent
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemGrid(
    items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    rows: Int? = null,
    title: LocalisedYoutubeString? = null,
    subtitle: LocalisedYoutubeString? = null,
    view_more: MediaItemLayout.ViewMore? = null,
    alt_style: Boolean = false,
    itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() },
    multiselect_context: MediaItemMultiSelectContext? = null,
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    val row_count = (rows ?: if (items.size <= 3) 1 else 2) * (if (alt_style) 2 else 1)
    val item_spacing = Arrangement.spacedBy(if (alt_style) 0.dp else 15.dp)
    val item_size = if (alt_style) DpSize(0.dp, 50.dp) else itemSizeProvider()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TitleBar(
            items,
            title,
            subtitle,
            view_more = view_more,
            multiselect_context = multiselect_context
        )

        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(row_count),
                modifier = Modifier
                    .height(item_size.height * row_count)
                    .fillMaxWidth(),
                horizontalArrangement = item_spacing,
                verticalArrangement = item_spacing
            ) {
                startContent?.invoke(this)

                items(items.size, { items[it].item?.id ?: "" }) { i ->
                    val item = items[i].item ?: return@items
                    val params = MediaItemPreviewParams(
                        Modifier.animateItemPlacement().then(
                            if (alt_style) Modifier.width(maxWidth * 0.9f)
                            else Modifier.size(item_size)
                        ),
                        contentColour = Theme.current.on_background_provider,
                        multiselect_context = multiselect_context,
                        square_item_max_text_rows = square_item_max_text_rows
                    )

                    if (alt_style) {
                        item.PreviewLong(params)
                    }
                    else {
                        item.PreviewSquare(params)
                    }
                }
            }

            if (multiselect_context != null && !shouldShowTitleBar(title, subtitle)) {
                Box(Modifier.background(CircleShape, Theme.current.background_provider), contentAlignment = Alignment.Center) {
                    multiselect_context.CollectionToggleButton(items)
                }
            }
        }
    }
}

@Composable
fun MediaItemList(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    MediaItemList(layout.items, modifier, numbered, layout.title, layout.subtitle, layout.view_more, multiselect_context)
}

@Composable
fun MediaItemList(
    items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    title: LocalisedYoutubeString? = null,
    subtitle: LocalisedYoutubeString? = null,
    view_more: MediaItemLayout.ViewMore? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    Column(modifier) {
        TitleBar(
            items,
            title,
            subtitle,
            Modifier.padding(bottom = 5.dp),
            view_more = view_more,
            multiselect_context = multiselect_context
        )

        for (item in items.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (numbered) {
                    Text((item.index + 1).toString().padStart((items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                }

                Column {
                    item.value.item?.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context))
                }
            }
        }
    }
}
