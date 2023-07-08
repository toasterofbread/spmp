@file:OptIn(ExperimentalMaterial3Api::class)

package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
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
    pill_menu: PillMenu,
    item: Artist,
    previous_item: MediaItem? = null,
    bottom_padding: Dp = 0.dp,
    close: () -> Unit
) {
    require(!item.is_for_item)

    val player = LocalPlayerState.current
    val screen_width = SpMp.context.getScreenWidth()

    val main_column_state = rememberLazyListState()
    var show_info by remember { mutableStateOf(false) }
    val multiselect_context = remember { MediaItemMultiSelectContext() {} }
    val feed_layouts = item.feed_layouts

    val background_modifier = Modifier.background(Theme.current.background_provider)
    val content_padding = PaddingValues(horizontal = 10.dp)
    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    LaunchedEffect(item.id) {
        item.getFeedLayouts().getOrReport("ArtistPageLoad")
    }

    LaunchedEffect(item, item.canLoadThumbnail()) {
        item.loadThumbnail(MediaItemThumbnailProvider.Quality.HIGH)
    }

    if (show_info) {
        InfoDialog(item) { show_info = false }
    }

    val top_bar_over_image: Boolean by Settings.KEY_TOPBAR_DISPLAY_OVER_ARTIST_IMAGE.rememberMutableState()
    var music_top_bar_showing by remember { mutableStateOf(false) }
    val top_bar_alpha by animateFloatAsState(if (!top_bar_over_image || music_top_bar_showing || multiselect_context.is_active) 1f else 0f)

    fun Density.getBackgroundColour(): Color =
        Theme.current.background.setAlpha(
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
                Spacer(Modifier.height(SpMp.context.getStatusBarHeight()))
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
            Crossfade(item.getThumbnail(MediaItemThumbnailProvider.Quality.HIGH)) { thumbnail ->
                if (thumbnail != null) {
                    if (accent_colour == null) {
                        accent_colour = Theme.current.makeVibrant(item.getDefaultThemeColour() ?: Theme.current.accent)
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
                                    0f to Theme.current.background,
                                    gradient_size to Color.Transparent
                                )
                            }
                    )
                }
            }

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
                                    1f to Theme.current.background
                                )
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        TitleBar(
                            item,
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
                                        { Text(text, style = MaterialTheme.typography.labelLarge) },
                                        Modifier.height(filter_bar_height),
                                        leadingIcon = {
                                            Icon(icon, null, tint = accent_colour ?: Color.Unspecified)
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Theme.current.background,
                                            labelColor = Theme.current.on_background,
                                            leadingIconContentColor = accent_colour ?: Color.Unspecified
                                        )
                                    )
                                }
                            }

                            chip(getString("artist_chip_shuffle"), Icons.Outlined.Shuffle) { player.playMediaItem(item, true) }

                            if (SpMp.context.canShare()) {
                                chip(getString("action_share"), Icons.Outlined.Share) { SpMp.context.shareText(item.url, item.title) }
                            }
                            if (SpMp.context.canOpenUrl()) {
                                chip(getString("artist_chip_open"), Icons.Outlined.OpenInNew) { SpMp.context.openUrl(item.url) }
                            }

                            chip(getString("artist_chip_details"), Icons.Outlined.Info) { show_info = !show_info }
                        }

                        Box(Modifier.requiredHeight(filter_bar_height)) {
                            ShapedIconButton(
                                { player.playMediaItem(item) },
                                Modifier.requiredSize(play_button_size),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = accent_colour ?: LocalContentColor.current,
                                    contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                            }
                        }
                    }
                }

                if (feed_layouts == null) {
                    item {
                        Box(background_modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                        }
                    }
                } else if (feed_layouts.size == 1) {
                    val layout = feed_layouts.single()

                    item {
                        layout.TitleBar(background_modifier.padding(content_padding).padding(bottom = 5.dp))
                    }

                    items(layout.items.size) { i ->
                        Row(background_modifier.padding(content_padding), verticalAlignment = Alignment.CenterVertically) {
                            layout.items[i].PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context))
                        }
                    }
                } else {
                    item {
                        Column(
                            background_modifier
                                .fillMaxSize()
                                .padding(content_padding),
                            verticalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            for (layout in item.feed_layouts!!) {
                                val is_singles =
                                    Settings.KEY_TREAT_SINGLES_AS_SONG.get() && layout.title?.getID() == YoutubeUILocalisation.StringID.ARTIST_PAGE_SINGLES

                                CompositionLocalProvider(LocalPlayerState provides remember {
                                    if (!is_singles) player
                                    else player.copy(
                                        onClickedOverride = { item, multiselect_key ->
                                            if (item is Playlist) {
                                                onSinglePlaylistClicked(item, player)
                                            } else {
                                                player.onMediaItemClicked(item, multiselect_key)
                                            }
                                        },
                                        onLongClickedOverride = { item, long_press_data ->
                                            player.onMediaItemLongClicked(
                                                item,
                                                if (item is Playlist)
                                                    long_press_data?.copy(playlist_as_song = true)
                                                        ?: LongPressMenuData(item, playlist_as_song = true)
                                                else long_press_data
                                            )
                                        }
                                    )
                                }) {
                                    val type =
                                        if (layout.type == null) MediaItemLayout.Type.GRID
                                        else if (layout.type == MediaItemLayout.Type.NUMBERED_LIST && item is Artist) MediaItemLayout.Type.LIST
                                        else layout.type

                                    type.Layout(
                                        if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                        multiselect_context = multiselect_context
                                    )
                                }
                            }

                            val description = item.description
                            if (description?.isNotBlank() == true) {
                                DescriptionCard(description, { Theme.current.background }, { accent_colour }) { show_info = !show_info }
                            }

                            Spacer(Modifier.requiredHeight(50.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun onSinglePlaylistClicked(playlist: Playlist, player: PlayerState) {
    Api.scope.launch {
        playlist.getFeedLayouts().onSuccess { layouts ->
            layouts.firstOrNull()?.items?.firstOrNull()?.also {
                withContext(Dispatchers.Main) {
                    player.onMediaItemClicked(it)
                }
            }
        }
    }
}
