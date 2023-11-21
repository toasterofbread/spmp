package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.platform.composable.SwipeRefresh
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.getThemeColour
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.composekit.utils.modifier.brushBackground
import com.toasterofbread.composekit.utils.modifier.drawScopeBackground
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.settings.category.TopBarSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

private const val ARTIST_IMAGE_SCROLL_MODIFIER = 0.25f

@Composable
fun ArtistLayout(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null,
    show_top_bar: Boolean = true,
    loading: Boolean = false,
    onReload: (() -> Unit)? = null,
    getAllSelectableItems: (() -> List<Pair<MediaItem, Int?>>)? = null,
    content: LazyListScope.(accent_colour: Color?, show_info: MutableState<Boolean>, Modifier) -> Unit
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current

    val thumbnail_provider: MediaItemThumbnailProvider? by artist.ThumbnailProvider.observe(player.database)
    val thumbnail_load_state: MediaItemThumbnailLoader.ItemState = MediaItemThumbnailLoader.rememberItemState(artist)

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

    val main_column_state = rememberLazyListState()
    val show_info = remember { mutableStateOf(false) }

    val background_modifier = Modifier.background(player.theme.background_provider)
    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    if (show_info.value) {
        InfoDialog(artist) { show_info.value = false }
    }

    val top_bar_over_image: Boolean by TopBarSettings.Key.DISPLAY_OVER_ARTIST_IMAGE.rememberMutableState()
    var music_top_bar_showing by remember { mutableStateOf(false) }
    val top_bar_alpha by animateFloatAsState(if (!top_bar_over_image || music_top_bar_showing || multiselect_context?.is_active == true) 1f else 0f)

    fun Theme.getBackgroundColour(): Color = with(density) {
        background.copy(alpha = 
            if (!top_bar_over_image || main_column_state.firstVisibleItemIndex > 0) top_bar_alpha
            else (0.5f + ((main_column_state.firstVisibleItemScrollOffset / screen_width.toPx()) * 0.5f)) * top_bar_alpha
        )
    }

    @Composable
    fun TopBar() {
        Column(
            Modifier
                .drawScopeBackground {
                    player.theme.getBackgroundColour()
                }
                .pointerInput(Unit) {}
                .zIndex(1f)
        ) {
            val showing = music_top_bar_showing || multiselect_context?.is_active == true
            AnimatedVisibility(showing) {
                Spacer(Modifier.height(WindowInsets.getTop()))
            }

            music_top_bar_showing = player.top_bar.MusicTopBar(
                TopBarSettings.Key.SHOW_IN_ARTIST,
                Modifier.fillMaxWidth().zIndex(1f),
                padding = content_padding.horizontal
            ).showing

            AnimatedVisibility(multiselect_context?.is_active == true) {
                multiselect_context?.InfoDisplay(Modifier.padding(top = 10.dp).padding(content_padding.horizontal), getAllSelectableItems)
            }

            AnimatedVisibility(showing) {
                WaveBorder(Modifier.fillMaxWidth(), getColour = { getBackgroundColour() })
            }
        }
    }

    Column(modifier) {
        if (show_top_bar && !top_bar_over_image) {
            TopBar()
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (show_top_bar && top_bar_over_image) {
                TopBar()
            }

            val th = thumbnail_load_state.getHighestQuality()

            // Thumbnail
            Crossfade(th) { thumbnail ->
                if (thumbnail != null) {
                    if (accent_colour == null) {
                        accent_colour = player.theme.makeVibrant(thumbnail.getThemeColour() ?: player.theme.accent)
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
                                    0f to player.theme.background,
                                    gradient_size to Color.Transparent
                                )
                            }
                    )
                }
            }

            SwipeRefresh(
                state = loading,
                onRefresh = {
                    onReload?.invoke()
                },
                swipe_enabled = !loading && onReload != null,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    main_column_state,
                    contentPadding = PaddingValues(bottom = content_padding.calculateBottomPadding())
                ) {

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
                                        1f to player.theme.background
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
                                contentPadding = PaddingValues(
                                    start = content_padding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = content_padding.calculateEndPadding(LocalLayoutDirection.current) + (play_button_size / 2)
                                ),
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
                                                containerColor = player.theme.background,
                                                labelColor = player.theme.on_background,
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
                                            artist.getActiveTitle(player.database) ?: ""
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
                                    show_info.value = !show_info.value
                                }
                            }

                            Box(Modifier.requiredHeight(filter_bar_height)) {
                                ShapedIconButton(
                                    { player.playMediaItem(artist) },
                                    IconButtonDefaults.iconButtonColors(
                                        containerColor = accent_colour ?: LocalContentColor.current,
                                        contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                                    ),
                                    Modifier.requiredSize(play_button_size)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                }
                            }
                        }
                    }

                    content(accent_colour, show_info, background_modifier)
                }
            }
        }
    }
}