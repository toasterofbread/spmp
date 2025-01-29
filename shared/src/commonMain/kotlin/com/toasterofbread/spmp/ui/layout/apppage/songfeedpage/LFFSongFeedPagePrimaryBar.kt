package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.getDisplayStringResource
import com.toasterofbread.spmp.model.getIcon
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredYtmItems
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LargeFilterList
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import dev.toastbits.composekit.components.utils.composable.RowOrColumn
import dev.toastbits.composekit.components.utils.composable.ScrollableRowOrColumn
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.platform.Platform
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SongFeedAppPage.LFFSongFeedPagePrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues,
    lazy: Boolean = true
): Boolean {
    val size: Dp =
        when (Platform.current) {
            Platform.ANDROID -> 100.dp
            Platform.DESKTOP,
            Platform.WEB -> 125.dp
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
                        containerColor = player.theme.vibrantAccent.copy(alpha = 0.85f),
                        contentColor = player.theme.vibrantAccent.getContrasted()
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
                        filter_chips?.get(i)?.getDisplayStringResource()?.let { stringResource(it) } ?: ""
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
