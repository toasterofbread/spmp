package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.toasterofbread.spmp.platform.isLargeFormFactor
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_LONG_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

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
    itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() },
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
    itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() },
    multiselect_context: MediaItemMultiSelectContext? = null,
    content_padding: PaddingValues = PaddingValues(),
    startContent: (LazyGridScope.() -> Unit)? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val filtered_items: List<MediaItem> by items.rememberFilteredItems(apply_filter)

    val row_count: Int = rows?.first ?: ((if (filtered_items.size <= 3) 1 else 2) * (if (alt_style) 2 else 1))
    val expanded_row_count: Int = rows?.second ?: row_count

    val item_spacing: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(
        (if (alt_style) 7.dp else 15.dp) * (if (player.isLargeFormFactor()) 3f else 1f)
    )
    val item_size: DpSize =
        if (alt_style) DpSize(0.dp, MEDIA_ITEM_PREVIEW_LONG_HEIGHT_DP.dp)
        else itemSizeProvider() + DpSize(0.dp, getMediaItemPreviewSquareAdditionalHeight(square_item_max_text_rows, MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp))
    val horizontal_padding: PaddingValues = content_padding.horizontal

    val grid_state: LazyGridState = rememberLazyGridState()

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
                items,
                title,
                subtitle,
                title_modifier,
                view_more = view_more,
                multiselect_context = multiselect_context,
                scrollable_state =
                    if (Platform.DESKTOP.isCurrent()) grid_state
                    else null
            )
        }

        Column {
            BoxWithConstraints(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.CenterEnd) {
                val current_rows: Int = if (expanded) expanded_row_count else row_count

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
                        val item = filtered_items[i].item
                        val preview_modifier = Modifier.animateItemPlacement().then(
                            if (alt_style)
                                if (player.isLargeFormFactor()) Modifier.width(300.dp)
                                else Modifier.width(maxWidth * 0.9f)
                            else Modifier.size(item_size)
                        )

                        if (alt_style) {
                            MediaItemPreviewLong(item, preview_modifier, contentColour = player.theme.on_background_provider, multiselect_context = multiselect_context, show_download_indicator = show_download_indicators)
                        }
                        else {
                            MediaItemPreviewSquare(item, preview_modifier, contentColour = player.theme.on_background_provider, multiselect_context = multiselect_context, max_text_rows = square_item_max_text_rows, show_download_indicator = show_download_indicators)
                        }
                    }
                }

                if (multiselect_context != null && !shouldShowTitleBar(title, subtitle)) {
                    Box(Modifier.background(CircleShape, player.theme.background_provider).padding(horizontal_padding), contentAlignment = Alignment.Center) {
                        multiselect_context.CollectionToggleButton(filtered_items)
                    }
                }
            }
        }
    }
}
