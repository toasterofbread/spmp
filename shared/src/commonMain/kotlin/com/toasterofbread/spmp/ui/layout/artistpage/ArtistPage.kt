@file:OptIn(ExperimentalMaterial3Api::class)

package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.*
import com.toasterofbread.utils.modifier.background
import com.toasterofbread.utils.modifier.brushBackground
import com.toasterofbread.utils.modifier.drawScopeBackground
import kotlinx.coroutines.*

private const val ARTIST_IMAGE_SCROLL_MODIFIER = 0.25f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPage(
    artist: Artist,
    previous_item: MediaItem? = null,
    bottom_padding: Dp = 0.dp,
    browse_params: Pair<String, ArtistWithParamsEndpoint>?,
    close: () -> Unit
) {
    val player = LocalPlayerState.current
    val db = player.context.database

    lazyAssert {
        !artist.IsForItem.get(db)
    }

    var load_error: Throwable? by remember { mutableStateOf(null) }
    val loading by artist.loadDataOnChange(player.context, load = browse_params == null) { load_error = it }
    var refreshed by remember { mutableStateOf(false) }

    val item_layouts: List<ArtistLayout>? by artist.Layouts.observe(db)
    val single_layout: MediaItemLayout? = item_layouts?.singleOrNull()?.rememberMediaItemLayout(db)
    var browse_params_rows: List<ArtistWithParamsRow>? by remember { mutableStateOf(null) }

    val thumbnail_provider: MediaItemThumbnailProvider? by artist.ThumbnailProvider.observe(db)
    val thumbnail_load_state = MediaItemThumbnailLoader.rememberItemState(artist, player.context)

    LaunchedEffect(artist.id, browse_params) {
        browse_params_rows = null

        if (browse_params == null) {
            return@LaunchedEffect
        }

        load_error = null

        val (params, params_endpoint) = browse_params
        require(params_endpoint.isImplemented())

        params_endpoint.loadArtistWithParams(artist.id, params).fold(
            { browse_params_rows = it },
            { load_error = it }
        )
    }

    LaunchedEffect(thumbnail_provider) {
        thumbnail_provider?.also { provider ->
            MediaItemThumbnailLoader.loadItemThumbnail(
                artist,
                provider,
                MediaItemThumbnailProvider.Quality.HIGH,
                player.context
            )
        }
    }

    // TODO display previous_item

    val screen_width = player.screen_size.width
    val coroutine_scope = rememberCoroutineScope()

    val main_column_state = rememberLazyListState()
    var show_info by remember { mutableStateOf(false) }
    val multiselect_context = remember { MediaItemMultiSelectContext() {} }

    val apply_filter: Boolean by Settings.KEY_FILTER_APPLY_TO_ARTIST_ITEMS.rememberMutableState()
    val background_modifier = Modifier.background(Theme.background_provider)
    val content_padding = PaddingValues(horizontal = 10.dp)
    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    val top_bar_over_image: Boolean by Settings.KEY_TOPBAR_DISPLAY_OVER_ARTIST_IMAGE.rememberMutableState()
    var music_top_bar_showing by remember { mutableStateOf(false) }
    val top_bar_alpha by animateFloatAsState(if (!top_bar_over_image || music_top_bar_showing || multiselect_context.is_active) 1f else 0f)

    fun Density.getBackgroundColour(): Color =
        Theme.background.setAlpha(
            if (!top_bar_over_image || main_column_state.firstVisibleItemIndex > 0) top_bar_alpha
            else (0.5f + ((main_column_state.firstVisibleItemScrollOffset / screen_width.toPx()) * 0.5f)) * top_bar_alpha
        )

    @Composable
    fun TopBar() {
        Column(
            Modifier
                .drawScopeBackground {
                    getBackgroundColour()
                }
                .pointerInput(Unit) {}
                .zIndex(1f)
        ) {
            val showing = music_top_bar_showing || multiselect_context.is_active
            AnimatedVisibility(showing) {
                Spacer(Modifier.height(player.context.getStatusBarHeight()))
            }

            MusicTopBar(
                Settings.KEY_LYRICS_SHOW_IN_ARTIST,
                Modifier.fillMaxWidth().zIndex(1f),
                padding = content_padding
            ) { music_top_bar_showing = it }

            AnimatedVisibility(multiselect_context.is_active) {
                multiselect_context.InfoDisplay(Modifier.padding(top = 10.dp).padding(content_padding))
            }

            AnimatedVisibility(showing) {
                WaveBorder(Modifier.fillMaxWidth(), getColour = { getBackgroundColour() })
            }
        }
    }

    Column {
        if (!top_bar_over_image) {
            TopBar()
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (top_bar_over_image) {
                TopBar()
            }

            // Thumbnail
            Crossfade(thumbnail_load_state.loaded_images.values.firstOrNull()?.get()) { thumbnail ->
                if (thumbnail != null) {
                    if (accent_colour == null) {
                        accent_colour = Theme.makeVibrant(thumbnail.getThemeColour() ?: Theme.accent)
                    }

                    Image(
                        thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .offset {
                                IntOffset(0, (main_column_state.firstVisibleItemScrollOffset * -ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                            }
                    )

                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .brushBackground {
                                Brush.verticalGradient(
                                    0f to Theme.background,
                                    gradient_size to Color.Transparent
                                )
                            }
                    )
                }
            }

            SwipeRefresh(
                state = refreshed && loading,
                onRefresh = {
                    refreshed = true
                    load_error = null
                    coroutine_scope.launch {
                        MediaItemLoader.loadArtist(artist.getEmptyData(), player.context)
                    }
                },
                swipe_enabled = !loading,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(Modifier.fillMaxSize(), main_column_state, contentPadding = PaddingValues(bottom = bottom_padding)) {

                    val play_button_size = 55.dp
                    val filter_bar_height = 32.dp

                    // Image spacing
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f)
                                .brushBackground {
                                    Brush.verticalGradient(
                                        1f - gradient_size to Color.Transparent,
                                        1f to Theme.background
                                    )
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            TitleBar(
                                artist,
                                Modifier
                                    .offset {
                                        IntOffset(0, (main_column_state.firstVisibleItemScrollOffset * ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                                    }
                                    .padding(bottom = (play_button_size - filter_bar_height) / 2f)
                            )
                        }
                    }

                    // Action / play button bar
                    item {
                        Box(
                            background_modifier.padding(bottom = 20.dp, end = 10.dp).fillMaxWidth().requiredHeight(filter_bar_height),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            LazyRow(
                                Modifier.fillMaxWidth().padding(end = play_button_size / 2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = content_padding.copy(end = content_padding.calculateEndPadding(LocalLayoutDirection.current) + (play_button_size / 2)),
                            ) {
                                fun chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                                    item {
                                        ElevatedAssistChip(
                                            onClick,
                                            { Text(text, style = typography.labelLarge) },
                                            Modifier.height(filter_bar_height),
                                            leadingIcon = {
                                                Icon(icon, null, tint = accent_colour ?: Color.Unspecified)
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Theme.background,
                                                labelColor = Theme.on_background,
                                                leadingIconContentColor = accent_colour ?: Color.Unspecified
                                            )
                                        )
                                    }
                                }

                                chip(getString("artist_chip_shuffle"), Icons.Outlined.Shuffle) { player.playMediaItem(artist, true) }

                                if (player.context.canShare()) {
                                    chip(
                                        getString("action_share"),
                                        Icons.Outlined.Share
                                    ) {
                                        player.context.shareText(
                                            artist.getURL(player.context),
                                            artist.Title.get(db) ?: ""
                                        )
                                    }
                                }
                                if (player.context.canOpenUrl()) {
                                    chip(
                                        getString("artist_chip_open"),
                                        Icons.Outlined.OpenInNew
                                    ) {
                                        player.context.openUrl(
                                            artist.getURL(player.context)
                                        )
                                    }
                                }

                                chip(
                                    getString("artist_chip_details"),
                                    Icons.Outlined.Info
                                ) {
                                    show_info = !show_info
                                }
                            }

                            Box(Modifier.requiredHeight(filter_bar_height)) {
                                ShapedIconButton(
                                    { player.playMediaItem(artist) },
                                    Modifier.requiredSize(play_button_size),
                                    colours = IconButtonDefaults.iconButtonColors(
                                        containerColor = accent_colour ?: LocalContentColor.current,
                                        contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                }
                            }
                        }
                    }

                    if (load_error != null) {
                        item {
                            load_error?.also { error ->
                                ErrorInfoDisplay(error, background_modifier)
                            }
                        }
                    }
                    else if (loading) {
                        item {
                            Box(background_modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                SubtleLoadingIndicator()
                            }
                        }
                    }
                    else if ((browse_params != null && browse_params_rows == null) || (browse_params == null && item_layouts == null)) {
                        item {
                            Box(background_modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                            }
                        }
                    }
                    else if (browse_params != null) {
                        items(browse_params_rows.orEmpty()) { row ->
                            MediaItemList(
                                row.items,
                                background_modifier.padding(content_padding),
                                title = row.title?.let { title ->
                                    LocalisedYoutubeString.Type.RAW.create(title)
                                }
                            )
                        }
                    }
                    else if (single_layout != null) {
                        item {
                            single_layout.TitleBar(background_modifier.padding(content_padding).padding(bottom = 5.dp))
                        }

                        items(single_layout.items) { item ->
                            Row(background_modifier.padding(content_padding), verticalAlignment = Alignment.CenterVertically) {
                                MediaItemPreviewLong(
                                    item,
                                    multiselect_context = multiselect_context
                                )
                            }
                        }
                    }
                    else {
                        item {
                            Column(
                                background_modifier
                                    .fillMaxSize()
                                    .padding(content_padding),
                                verticalArrangement = Arrangement.spacedBy(30.dp)
                            ) {
                                for (artist_layout in item_layouts ?: emptyList()) {
                                    val layout = artist_layout.rememberMediaItemLayout(db)
                                    val layout_id = layout.title?.getID()

                                    val is_singles =
                                        Settings.KEY_TREAT_SINGLES_AS_SONG.get()
                                        && layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES

                                    val is_artist_row: Boolean =
                                        layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES || layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER

                                    CompositionLocalProvider(LocalPlayerState provides remember {
                                        player.copy(
                                            onClickedOverride = { item, multiselect_key ->
                                                if (is_singles && item is Playlist) {
                                                    onSinglePlaylistClicked(item, player)
                                                }
                                                else if (item !is Song) {
                                                    player.openMediaItem(item, is_artist_row)
                                                }
                                                else {
                                                    player.playMediaItem(item)
                                                }
                                            },
                                            onLongClickedOverride = { item, long_press_data ->
                                                player.onMediaItemLongClicked(
                                                    item,
                                                    if (is_singles && item is Playlist)
                                                        long_press_data?.copy(playlist_as_song = true)
                                                            ?: LongPressMenuData(item, playlist_as_song = true)
                                                    else long_press_data
                                                )
                                            }
                                        )
                                    }) {
                                        val type =
                                            if (layout.type == null) MediaItemLayout.Type.GRID
                                            else if (layout.type == MediaItemLayout.Type.NUMBERED_LIST && artist is Artist) MediaItemLayout.Type.LIST
                                            else layout.type

                                        type.Layout(
                                            if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                            multiselect_context = multiselect_context,
                                            apply_filter = apply_filter
                                        )
                                    }
                                }

                                val artist_description: String? by artist.Description.observe(db)
                                artist_description?.also { description ->
                                    if (description.isNotBlank()) {
                                        DescriptionCard(description, { Theme.background }, { accent_colour }) { show_info = !show_info }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun onSinglePlaylistClicked(playlist: Playlist, player: PlayerState) {
    GlobalScope.launch {
        playlist.loadData(player.context).onSuccess { data ->
            (data as PlaylistData).items?.firstOrNull()?.also { first_item ->
                withContext(Dispatchers.Main) {
                    player.onMediaItemClicked(first_item)
                }
            }
        }
    }
}
