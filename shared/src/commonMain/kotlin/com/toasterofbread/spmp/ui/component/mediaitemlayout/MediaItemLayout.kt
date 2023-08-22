package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.*
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.WidthShrinkText

fun getDefaultMediaItemPreviewSize(): DpSize = DpSize(100.dp, 120.dp)

data class MediaItemLayout(
    val items: List<MediaItem>,
    val title: LocalisedYoutubeString?,
    val subtitle: LocalisedYoutubeString?,
    val type: Type? = null,
    var view_more: ViewMore? = null,
    val continuation: Continuation? = null
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
            multiselect_context: MediaItemMultiSelectContext? = null,
            apply_filter: Boolean = false
        ) {
            when (this) {
                GRID -> MediaItemGrid(layout, modifier, multiselect_context = multiselect_context, apply_filter = apply_filter)
                GRID_ALT -> MediaItemGrid(layout, modifier, alt_style = true, multiselect_context = multiselect_context, apply_filter = apply_filter)
                ROW -> MediaItemGrid(layout, modifier, 1, multiselect_context = multiselect_context, apply_filter = apply_filter)
                LIST -> MediaItemList(layout, modifier, false, multiselect_context = multiselect_context, apply_filter = apply_filter)
                NUMBERED_LIST -> MediaItemList(layout, modifier, true, multiselect_context = multiselect_context, apply_filter = apply_filter)
                CARD -> MediaItemCard(layout, modifier, multiselect_context = multiselect_context, apply_filter = apply_filter)
            }
        }
    }

    @Composable
    fun Layout(modifier: Modifier = Modifier, multiselect_context: MediaItemMultiSelectContext? = null, apply_filter: Boolean = false) {
        type!!.Layout(this, modifier, multiselect_context, apply_filter)
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

        suspend fun loadContinuation(context: PlatformContext, filters: List<RadioBuilderModifier> = emptyList()): Result<Pair<List<MediaItemData>, String?>> {
            return when (type) {
                Type.SONG -> loadSongContinuation(filters, context)
                Type.PLAYLIST -> loadPlaylistContinuation(false, context)
                Type.PLAYLIST_INITIAL -> loadPlaylistContinuation(true, context)
            }
        }

        fun update(token: String) {
            this.token = token
            if (type == Type.PLAYLIST_INITIAL) {
                type = Type.PLAYLIST
            }
        }

        private suspend fun loadSongContinuation(filters: List<RadioBuilderModifier>, context: PlatformContext): Result<Pair<List<MediaItemData>, String?>> {
            val radio_endpoint = context.ytapi.SongRadio
            if (!radio_endpoint.isImplemented()) {
                return Result.failure(EndpointNotImplementedException(radio_endpoint))
            }

            val result = radio_endpoint.getSongRadio(param as String, token, filters)
            return result.fold(
                { Result.success(Pair(it.items, it.continuation)) },
                { Result.failure(it) }
            )
        }

        private suspend fun loadPlaylistContinuation(initial: Boolean, context: PlatformContext): Result<Pair<List<MediaItemData>, String?>> {
            val continuation_endpoint = context.ytapi.PlaylistContinuation

            if (!continuation_endpoint.isImplemented()) {
                return Result.failure(EndpointNotImplementedException(continuation_endpoint))
            }

            return continuation_endpoint.getPlaylistContinuation(initial, token, if (initial) param as Int else 0)
        }
    }

    sealed interface ViewMore {
        fun execute(player: PlayerState, title: LocalisedYoutubeString?)
    }

    data class LambdaViewMore(
        val action: (player: PlayerState, title: LocalisedYoutubeString?) -> Unit
    ): ViewMore {
        override fun execute(player: PlayerState, title: LocalisedYoutubeString?) = action(player, title)
    }

    data class MediaItemViewMore(
        val media_item: MediaItem,
        val browse_params: String? = null
    ): ViewMore {
        override fun execute(player: PlayerState, title: LocalisedYoutubeString?) {
            player.openMediaItem(media_item, true, browse_params)
        }
    }

    data class ListPageBrowseIdViewMore(
        val list_page_browse_id: String,
        val browse_params: String? = null
    ): ViewMore {
        override fun execute(player: PlayerState, title: LocalisedYoutubeString?) {
            if (browse_params != null) {
                player.openMediaItem(
                    PlaylistData(
                        list_page_browse_id,
                        browse_params = browse_params
                    ).also { playlist ->
                        playlist.title = title?.getString() ?: ""
                    },
                )
            }
            else {
                player.openViewMorePage(list_page_browse_id, title?.getString())
            }
        }
    }

    @Composable
    fun TitleBar(
        modifier: Modifier = Modifier,
        font_size: TextUnit? = null,
        multiselect_context: MediaItemMultiSelectContext? = null
    ) {
        TitleBar(items, title, subtitle, modifier, view_more, font_size, multiselect_context)
    }
}

internal fun shouldShowTitleBar(
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    view_more: MediaItemLayout.ViewMore? = null
): Boolean = title != null || subtitle != null || view_more != null

@Composable
internal fun TitleBar(
    items: List<MediaItemHolder>,
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    modifier: Modifier = Modifier,
    view_more: MediaItemLayout.ViewMore? = null,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    AnimatedVisibility(shouldShowTitleBar(title, subtitle, view_more), modifier) {
        val title_string: String? = remember { title?.getString() }
        val subtitle_string: String? = remember { subtitle?.getString() }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                if (subtitle_string != null) {
                    WidthShrinkText(subtitle_string, style = MaterialTheme.typography.titleSmall.copy(color = Theme.on_background))
                }

                if (title_string != null) {
                    WidthShrinkText(
                        title_string,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            style.copy(
                                color = Theme.on_background,
                                fontSize = font_size ?: style.fontSize
                            )
                        }
                    )
                }
            }

            Row {
                view_more?.also { view_more ->
                    val player = LocalPlayerState.current
                    IconButton({ view_more.execute(player, title) }) {
                        Icon(Icons.Default.MoreHoriz, null)
                    }
                }

                multiselect_context?.CollectionToggleButton(items)
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
    val player = LocalPlayerState.current

    val item: MediaItem = layout.items.single()
    val accent_colour: Color? by item.ThemeColour.observe(SpMp.context.database)

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

            val item_playlist_type: State<PlaylistType?>? =
                if (item is Playlist) item.TypeOfPlaylist.observe(SpMp.context.database)
                else null

            Text(
                if (item is Playlist) item_playlist_type?.value.getReadable(false)
                else item.getType().getReadable(false),
                fontSize = 15.sp
            )

            Icon(
                when (item) {
                    is Song -> Icons.Filled.MusicNote
                    is Artist -> Icons.Filled.Person
                    is Playlist -> {
                        when (item_playlist_type?.value) {
                            PlaylistType.PLAYLIST, PlaylistType.LOCAL, null -> Icons.Filled.PlaylistPlay
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
                val item_title: String? by item.Title.observe(SpMp.context.database)
                Text(
                    item_title ?: "",
                    style = LocalTextStyle.current.copy(color = (accent_colour ?: Theme.accent).getContrasted()),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                if (item is MediaItem.WithArtist) {
                    val item_artist: Artist? by item.Artist.observe(SpMp.context.database)
                    item_artist?.also { artist ->
                        MediaItemPreviewLong(artist, contentColour = { (accent_colour ?: Theme.accent).getContrasted() })
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
                    containerColor = accent_colour ?: Theme.vibrant_accent,
                    contentColor = (accent_colour ?: Theme.vibrant_accent).getContrasted()
                )
            ) {
                Text(getString(when (item.getType()) {
                    MediaItemType.SONG -> "media_play"
                    MediaItemType.ARTIST -> "artist_chip_play"
                    MediaItemType.PLAYLIST_ACC, MediaItemType.PLAYLIST_LOC -> "playlist_chip_play"
                }))
            }
        }
    }
}

//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun MediaItemGrid(
//    items: List<MediaItemHolder>,
//    modifier: Modifier = Modifier,
//    rows: Int? = null,
//    title: LocalisedYoutubeString? = null,
//    subtitle: LocalisedYoutubeString? = null,
//    view_more: MediaItemLayout.ViewMore? = null,
//    alt_style: Boolean = false,
//    square_item_max_text_rows: Int? = null,
//    itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() },
//    multiselect_context: MediaItemMultiSelectContext? = null,
//    startContent: (LazyGridScope.() -> Unit)? = null
//) {
//    val row_count = (rows ?: if (items.size <= 3) 1 else 2) * (if (alt_style) 2 else 1)
//    val item_spacing = Arrangement.spacedBy(if (alt_style) 7.dp else 15.dp)
//    val item_size = if (alt_style) DpSize(0.dp, MEDIA_ITEM_PREVIEW_LONG_HEIGHT.dp) else itemSizeProvider()
//
//    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
//        TitleBar(
//            title,
//            subtitle,
//            view_more = view_more,
//            multiselect_context = multiselect_context
//        )
//
//        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
//            LazyHorizontalGrid(
//                rows = GridCells.Fixed(row_count),
//                modifier = Modifier
//                    .height( item_size.height * row_count + item_spacing.spacing * (row_count - 1) )
//                    .fillMaxWidth(),
//                horizontalArrangement = item_spacing,
//                verticalArrangement = item_spacing
//            ) {
//                startContent?.invoke(this)
//
//
//                items(items.size, { items[it].item?.id ?: "" }) { i ->
//                    val item = items[i].item ?: return@items
//                    val preview_modifier = Modifier.animateItemPlacement().then(
//                        if (alt_style) Modifier.width(maxWidth * 0.9f)
//                        else Modifier.size(item_size)
//                    )
//
//                    if (alt_style) {
//                        MediaItemPreviewLong(item, preview_modifier, contentColour = Theme.on_background_provider, multiselect_context = multiselect_context)
//                    }
//                    else {
//                        MediaItemPreviewSquare(item, preview_modifier, contentColour = Theme.on_background_provider, multiselect_context = multiselect_context, max_text_rows = square_item_max_text_rows)
//                    }
//                }
//            }
//
//            if (multiselect_context != null && !shouldShowTitleBar(title, subtitle)) {
//                Box(Modifier.background(CircleShape, Theme.background_provider), contentAlignment = Alignment.Center) {
//                    multiselect_context.CollectionToggleButton(items)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun MediaItemList(
//    items: List<MediaItemHolder>,
//    modifier: Modifier = Modifier,
//    numbered: Boolean = false,
//    title: LocalisedYoutubeString? = null,
//    subtitle: LocalisedYoutubeString? = null,
//    view_more: MediaItemLayout.ViewMore? = null,
//    multiselect_context: MediaItemMultiSelectContext? = null
//) {
//    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
//        TitleBar(
//            title,
//            subtitle,
//            Modifier.padding(bottom = 5.dp),
//            view_more = view_more,
//            multiselect_context = multiselect_context
//        )
//
//        for (item in items.withIndex()) {
//            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//                if (numbered) {
//                    Text((item.index + 1).toString().padStart((items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
//                }
//
//                    item.value.item?.also { item ->
//                        MediaItemPreviewLong(item, multiselect_context = multiselect_context)
//                    }
//            }
//        }
//    }
//}
