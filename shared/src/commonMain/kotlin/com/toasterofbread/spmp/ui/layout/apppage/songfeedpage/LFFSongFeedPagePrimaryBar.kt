package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LargeFilterList
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot

@Composable
internal fun SongFeedAppPage.LFFSongFeedPagePrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues,
    lazy: Boolean = true
): Boolean {
    val size: Dp = when (Platform.current) {
        Platform.ANDROID -> 100.dp
        Platform.DESKTOP -> 125.dp
    }

    val player: PlayerState = LocalPlayerState.current

    val artists: List<MediaItem>? by artists_layout?.items?.rememberFilteredYtmItems()
    var show_filters: Boolean by remember { mutableStateOf(false) }

    val can_show_artists: Boolean = !artists.isNullOrEmpty()
    val can_show_filters: Boolean = !filter_chips.isNullOrEmpty()

    Crossfade(
        show_filters,
        modifier.then(
            if (slot.is_vertical) Modifier.width(size)
            else Modifier.height(size)
        )
    ) { filters ->
        RowOrColumn(
            !slot.is_vertical,
            Modifier.padding(top = content_padding.calculateTopPadding()),
            alignment = 0,
            arrangement = Arrangement.spacedBy(10.dp)
        ) {
            RowOrColumn(
                slot.is_vertical,
                Modifier.padding(content_padding.horizontal),
                arrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selected_colours: IconButtonColors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = player.theme.vibrant_accent.copy(alpha = 0.85f),
                        contentColor = player.theme.vibrant_accent.getContrasted()
                    )

                ShapedIconButton(
                    { show_filters = false },
                    if (!filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_artists || !filters
                ) {
                    Icon(Icons.Default.Person, null)
                }

                ShapedIconButton(
                    { show_filters = true },
                    if (filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_filters || filters
                ) {
                    Icon(Icons.Default.FilterAlt, null)
                }
            }

            val list_modifier: Modifier = Modifier.weight(1f)
            val side_padding: Dp = 10.dp

            if (filters) {
                LargeFilterList(
                    filter_chips?.size ?: 0,
                    getItemText = { i ->
                        filter_chips?.get(i)?.text?.getString(player.context) ?: ""
                    },
                    getItemIcon = { i ->
                        filter_chips?.get(i)?.getIcon()
                    },
                    isItemSelected = { i ->
                        selected_filter_chip == i
                    },
                    onSelected = { i ->
                        selectFilterChip(i)
                    },
                    modifier = list_modifier,
                    content_padding = PaddingValues(bottom = content_padding.calculateBottomPadding(), start = side_padding, end = side_padding),
                    vertical = slot.is_vertical,
                    lazy = lazy
                )
            }
            else {
                ScrollableRowOrColumn(
                    row = !slot.is_vertical,
                    lazy = lazy,
                    item_count = artists?.size ?: 0,
                    content_padding = PaddingValues(bottom = content_padding.calculateBottomPadding()),
                    arrangement = Arrangement.spacedBy(15.dp),
                    reverse_scroll_bar_layout = slot.is_vertical,
                    scroll_bar_colour = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = list_modifier
                ) { index ->
                    val artist: MediaItem = artists?.getOrNull(index) ?: return@ScrollableRowOrColumn
                    MediaItemPreviewSquare(
                        artist,
                        Modifier
                            .run {
                                if (slot.is_vertical) fillMaxWidth()
                                else fillMaxHeight()
                            }
                            .aspectRatio(1f)
                            .padding(horizontal = side_padding),
                        multiselect_context = player.main_multiselect_context,
                        apply_size = false
                    )
                }
            }
        }
    }

    return true
}
