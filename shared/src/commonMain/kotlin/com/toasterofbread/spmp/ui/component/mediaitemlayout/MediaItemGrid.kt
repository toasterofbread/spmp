package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.components.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.layout.shouldShowTitleBar
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.uistrings.UiString
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MediaItemGrid(
    layout: AppMediaItemLayout,
    layout_params: MediaItemLayoutParams,
    grid_params: MediaItemGridParams = MediaItemGridParams()
) {
    MediaItemGrid(
        layout_params =
            remember(layout, layout_params) {
                layout_params.copy(
                    items = layout.items,
                    title = layout.title,
                    subtitle = layout.subtitle,
                    view_more = layout.view_more
                )
            },
        grid_params = grid_params
    )
}

@Composable
fun MediaItemGrid(
    layout_params: MediaItemLayoutParams,
    grid_params: MediaItemGridParams = MediaItemGridParams()
) {
    val filtered_items: List<MediaItem> by layout_params.rememberFilteredItems()
    if (filtered_items.isEmpty()) {
        return
    }

    val player: PlayerState = LocalPlayerState.current

    val row_count: Int = grid_params.rows?.first ?: ((if (filtered_items.size <= 3) 1 else 2) * (if (grid_params.alt_style) 2 else 1))
    val expanded_row_count: Int = grid_params.rows?.second ?: row_count

    val item_spacing: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(
        (if (grid_params.alt_style) 7.dp else 15.dp) * (if (Platform.DESKTOP.isCurrent()) 3f else 1f)
    )

    val provided_item_size: DpSize? = grid_params.itemSizeProvider().takeIf { it.isSpecified }
    val item_size: DpSize =
        if (grid_params.alt_style) provided_item_size ?: getDefaultMediaItemPreviewSize(true)
        else (provided_item_size ?: getDefaultMediaItemPreviewSize(false)) + DpSize(0.dp, getMediaItemPreviewSquareAdditionalHeight(grid_params.square_item_max_text_rows, MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp))

    val horizontal_padding: PaddingValues = layout_params.content_padding.horizontal

    val grid_state: LazyGridState = rememberLazyGridState()
    val scrollable_state: ScrollableState? =
        if (Platform.DESKTOP.isCurrent()) grid_state
        else null

    Column(
        layout_params.modifier.padding(layout_params.content_padding.vertical),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var expanded: Boolean by remember { mutableStateOf(false) }
        Row(
            Modifier.padding(horizontal_padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (expanded_row_count > row_count) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(expanded) {
                        Icon(
                            if (it) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            null
                        )
                    }
                }
            }

            TitleBar(
                filtered_items,
                layout_params,
                modifier = layout_params.title_modifier,
                scrollable_state = scrollable_state
            )
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().animateContentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            val current_rows: Int by remember { derivedStateOf {
                val current_rows: Int = if (expanded) expanded_row_count else row_count
                val min_columns: Int = (
                    (this@BoxWithConstraints.maxWidth + item_spacing.spacing) / (item_size.width + item_spacing.spacing)
                ).roundToInt()

                val item_count: Int = filtered_items.size
                val max_rows: Int = ceil(item_count.toFloat() / min_columns).toInt()
                return@derivedStateOf current_rows.coerceAtMost(max_rows).coerceAtLeast(1)
            } }

            LazyHorizontalGrid(
                state = grid_state,
                rows = GridCells.Fixed(current_rows),
                modifier = Modifier
                    .height(item_size.height * current_rows + item_spacing.spacing * (current_rows - 1))
                    .fillMaxWidth(),
                horizontalArrangement = item_spacing,
                verticalArrangement = item_spacing,
                contentPadding = horizontal_padding
            ) {
                grid_params.startContent?.invoke(this)

                items(filtered_items.size, { filtered_items[it].item.getUid() }) { i ->
                    val item: MediaItem = filtered_items[i].item
                    val preview_modifier: Modifier = Modifier.animateItem().size(item_size)

                    if (grid_params.alt_style) {
                        MediaItemPreviewLong(
                            item,
                            preview_modifier,
                            multiselect_context = layout_params.multiselect_context,
                            show_download_indicator = layout_params.show_download_indicators
                        )
                    }
                    else {
                        MediaItemPreviewSquare(
                            item,
                            preview_modifier,
                            multiselect_context = layout_params.multiselect_context,
                            max_text_rows = grid_params.square_item_max_text_rows,
                            show_download_indicator = layout_params.show_download_indicators
                        )
                    }
                }
            }

            if (
                layout_params.multiselect_context != null
                && !shouldShowTitleBar(layout_params, scrollable_state)
            ) {
                Box(
                    Modifier
                        .background(CircleShape, { player.theme.background })
                        .padding(horizontal_padding),
                    contentAlignment = Alignment.Center
                ) {
                    layout_params.multiselect_context.CollectionToggleButton(filtered_items)
                }
            }
        }
    }
}
