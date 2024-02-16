package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.layout.shouldShowTitleBar
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    title_modifier: Modifier = Modifier,
    rows: Pair<Int, Int>? = null,
    alt_style: Boolean = false,
    apply_filter: Boolean = false,
    multiselect_context: MediaItemMultiSelectContext? = null,
    square_item_max_text_rows: Int? = null,
    show_download_indicators: Boolean = true,
    content_padding: PaddingValues = PaddingValues(),
    itemSizeProvider: @Composable () -> DpSize = { DpSize.Unspecified },
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    MediaItemGrid(
        layout.items,
        modifier,
        title_modifier,
        rows,
        layout.title,
        layout.subtitle,
        layout.view_more,
        alt_style = alt_style,
        apply_filter = apply_filter,
        square_item_max_text_rows = square_item_max_text_rows,
        show_download_indicators = show_download_indicators,
        itemSizeProvider = itemSizeProvider,
        multiselect_context = multiselect_context,
        content_padding = content_padding,
        startContent = startContent
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemGrid(
    items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    title_modifier: Modifier = Modifier,
    rows: Pair<Int, Int>? = null,
    title: LocalisedString? = null,
    subtitle: LocalisedString? = null,
    view_more: ViewMore? = null,
    alt_style: Boolean = false,
    square_item_max_text_rows: Int? = null,
    apply_filter: Boolean = false,
    show_download_indicators: Boolean = true,
    itemSizeProvider: @Composable () -> DpSize = { DpSize.Unspecified },
    multiselect_context: MediaItemMultiSelectContext? = null,
    content_padding: PaddingValues = PaddingValues(),
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val filtered_items: List<MediaItem> by items.rememberFilteredItems(apply_filter)

    val row_count: Int = rows?.first ?: ((if (filtered_items.size <= 3) 1 else 2) * (if (alt_style) 2 else 1))
    val expanded_row_count: Int = rows?.second ?: row_count

    val item_spacing: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(
        (if (alt_style) 7.dp else 15.dp) * (if (Platform.DESKTOP.isCurrent()) 3f else 1f)
    )

    val provided_item_size: DpSize? = itemSizeProvider().takeIf { it.isSpecified }
    val item_size: DpSize =
        if (alt_style) provided_item_size ?: getDefaultMediaItemPreviewSize(true)
        else (provided_item_size ?: getDefaultMediaItemPreviewSize(false)) + DpSize(0.dp, getMediaItemPreviewSquareAdditionalHeight(square_item_max_text_rows, MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp))

    val horizontal_padding: PaddingValues = content_padding.horizontal

    val grid_state: LazyGridState = rememberLazyGridState()
    val scrollable_state: ScrollableState? =
        if (Platform.DESKTOP.isCurrent()) grid_state
        else null

    Column(modifier.padding(content_padding.vertical), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                title,
                subtitle,
                title_modifier,
                view_more = view_more,
                multiselect_context = multiselect_context,
                scrollable_state = scrollable_state
            )
        }

        Column {
            BoxWithConstraints(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.CenterEnd) {
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
                    contentPadding = content_padding
                ) {
                    startContent?.invoke(this)

                    items(filtered_items.size, { filtered_items[it].item.getUid() }) { i ->
                        val item: MediaItem = filtered_items[i].item
                        val preview_modifier: Modifier = Modifier.animateItemPlacement().size(item_size)

                        if (alt_style) {
                            MediaItemPreviewLong(item, preview_modifier, multiselect_context = multiselect_context, show_download_indicator = show_download_indicators)
                        }
                        else {
                            MediaItemPreviewSquare(item, preview_modifier, multiselect_context = multiselect_context, max_text_rows = square_item_max_text_rows, show_download_indicator = show_download_indicators)
                        }
                    }
                }

                if (multiselect_context != null && !shouldShowTitleBar(title, subtitle, view_more, scrollable_state)) {
                    Box(
                        Modifier
                            .background(CircleShape, { player.theme.background })
                            .padding(horizontal_padding),
                        contentAlignment = Alignment.Center
                    ) {
                        multiselect_context.CollectionToggleButton(filtered_items)
                    }
                }
            }
        }
    }
}
