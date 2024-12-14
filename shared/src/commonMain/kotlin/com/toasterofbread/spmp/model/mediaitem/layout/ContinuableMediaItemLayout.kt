package com.toasterofbread.spmp.model.mediaitem.layout

import LocalPlayerState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.platform.Platform
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.toMediaItemRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.model.MediaItemListParams
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemCard
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.uistrings.UiString

data class ContinuableMediaItemLayout(
    val layout: AppMediaItemLayout,
    val continuation: Continuation? = null
) {
    data class Continuation(var token: String) {
        suspend fun loadContinuation(
            context: AppContext
        ): Result<Pair<List<YtmMediaItem>, String?>> = runCatching {
            val (items, continuation) =
                context.ytapi.PlaylistContinuation.getPlaylistContinuation(
                    false,
                    token
                ).getOrThrow()

            return@runCatching Pair(items, continuation?.token)
        }

        fun update(token: String) {
            this.token = token
        }
    }
}

@Composable
fun getDefaultMediaItemPreviewSize(long: Boolean): DpSize {
    val form_factor: FormFactor by FormFactor.observe()
    return when (Platform.current) {
        Platform.ANDROID -> {
            if (long)
                DpSize(
                    (
                        if (form_factor.is_large) 450.dp
                        else 300.dp
                    ),
                    50.dp
                )
            else DpSize(100.dp, 100.dp)
        }
        Platform.DESKTOP,
        Platform.WEB -> {
            if (long) {
                if (form_factor.is_large) DpSize(400.dp, 75.dp)
                else DpSize(200.dp, 50.dp)
            }
            else {
                if (form_factor.is_large) DpSize(150.dp, 150.dp)
                else DpSize(100.dp, 100.dp)
            }
        }
    }
}

@Composable
fun getMediaItemPreviewSquareAdditionalHeight(text_rows: Int?, line_height: TextUnit): Dp =
    with(LocalDensity.current) {
        line_height.toDp() * (text_rows ?: 1)
    } + 5.dp

@Composable
fun ItemLayoutType.Layout(
    layout: AppMediaItemLayout,
    layout_params: MediaItemLayoutParams,
    grid_params: MediaItemGridParams = MediaItemGridParams(),
    list_params: MediaItemListParams = MediaItemListParams()
) {
    when (this) {
        ItemLayoutType.GRID -> MediaItemGrid(layout, layout_params, remember(grid_params) { grid_params.copy(alt_style = false) })
        ItemLayoutType.GRID_ALT -> MediaItemGrid(layout, layout_params, remember(grid_params) { grid_params.copy(alt_style = true) })
        ItemLayoutType.ROW -> MediaItemGrid(layout, layout_params, remember(grid_params) { grid_params.copy(rows = Pair(1, 1)) })
        ItemLayoutType.LIST -> MediaItemList(layout, layout_params, remember(list_params) { list_params.copy(numbered = false) })
        ItemLayoutType.NUMBERED_LIST -> MediaItemList(layout, layout_params, remember(list_params) { list_params.copy(numbered = false) })
        ItemLayoutType.CARD -> MediaItemCard(
            layout,
            layout_params.modifier,
            multiselect_context = layout_params.multiselect_context,
            apply_filter = layout_params.apply_filter
        )
    }
}

@Composable
fun AppMediaItemLayout.Layout(
    layout_params: MediaItemLayoutParams,
    grid_params: MediaItemGridParams = MediaItemGridParams(),
    list_params: MediaItemListParams = MediaItemListParams()
) {
    type!!.Layout(this, layout_params, grid_params, list_params)
}

@Composable
fun AppMediaItemLayout.TitleBar(
    modifier: Modifier = Modifier,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val media_items: List<MediaItem> = remember(items) { items.map { it.toMediaItemRef() } }
    val layout_params: MediaItemLayoutParams = remember(this, multiselect_context) {
        MediaItemLayoutParams(
            title = title,
            subtitle = subtitle,
            view_more = view_more,
            multiselect_context = multiselect_context
        )
    }

    com.toasterofbread.spmp.ui.component.mediaitemlayout.TitleBar(
        media_items,
        layout_params,
        modifier = modifier,
        font_size = font_size
    )
}

internal fun shouldShowTitleBar(
    layout_params: MediaItemLayoutParams,
    scrollable_state: ScrollableState? = null
): Boolean =
    layout_params.title != null
    || layout_params.subtitle != null
    || layout_params.view_more != null
    || (scrollable_state != null && (scrollable_state.canScrollForward || scrollable_state.canScrollBackward))
