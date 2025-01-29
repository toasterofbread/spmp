package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import dev.toastbits.composekit.components.platform.composable.SwipeRefresh
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.composekit.components.utils.modifier.brushBackground
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.theme.core.makeVibrant
import dev.toastbits.composekit.util.getThemeColour
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

private const val ARTIST_IMAGE_SCROLL_MODIFIER = 0.25f

@Composable
fun ArtistLayout(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null,
    loading: Boolean = false,
    onReload: (() -> Unit)? = null,
    getAllSelectableItems: (() -> List<List<MultiSelectItem>>)? = null,
    content: LazyListScope.(accent_colour: Color?, Modifier) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    val thumbnail_provider: ThumbnailProvider? by artist.ThumbnailProvider.observe(player.database)

    LaunchedEffect(thumbnail_provider) {
        thumbnail_provider?.also { provider ->
            MediaItemThumbnailLoader.loadItemThumbnail(
                artist,
                provider,
                ThumbnailProvider.Quality.HIGH,
                player.context
            )
        }
    }

    // TODO display previous_item

    val main_column_state: LazyListState = rememberLazyListState()

    val background_modifier: Modifier = Modifier.background({ player.theme.background })
    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    val top_padding: Dp = content_padding.calculateTopPadding()

    Column(modifier) {
        Spacer(
            Modifier
                .height(top_padding)
                .fillMaxWidth()
                .then(background_modifier)
                .zIndex(1f)
        )

        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.TopCenter) {
            artist.Thumbnail(
                ThumbnailProvider.Quality.HIGH,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .offset {
                        IntOffset(0, (main_column_state.firstVisibleItemScrollOffset * -ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                    },
                onLoaded = { thumbnail ->
                    if (accent_colour == null) {
                        accent_colour = thumbnail?.getThemeColour()?.let { player.theme.makeVibrant(it) }
                    }
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

                    val play_button_size: Dp = 55.dp
                    val action_bar_height: Dp = 32.dp

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
                            ArtistPageTitleBar(
                                artist,
                                Modifier
                                    .offset {
                                        IntOffset(0, (main_column_state.firstVisibleItemScrollOffset * ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                                    }
                                    .padding(bottom = (play_button_size - action_bar_height) / 2f)
                            )
                        }
                    }

                    item {
                        ArtistActionBar(
                            artist,
                            background_modifier.padding(bottom = 20.dp, end = 10.dp).fillMaxWidth().requiredHeight(action_bar_height),
                            content_padding.horizontal,
                            height = action_bar_height,
                            play_button_size = play_button_size,
                            accent_colour = accent_colour
                        )
                    }

                    content(accent_colour, background_modifier.fillMaxWidth())
                }
            }
        }
    }
}
