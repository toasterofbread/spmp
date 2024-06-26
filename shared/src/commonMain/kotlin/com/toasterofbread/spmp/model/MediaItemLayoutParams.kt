package com.toasterofbread.spmp.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import dev.toastbits.ytmkt.uistrings.UiString
import dev.toastbits.ytmkt.model.external.YoutubePage

data class MediaItemLayoutParams(
    val items: List<MediaItemHolder> = emptyList(),
    val modifier: Modifier = Modifier,
    val title_modifier: Modifier = Modifier,
    val getTitleTextStyle: (TextStyle) -> TextStyle = { it },
    val title: UiString? = null,
    val subtitle: UiString? = null,
    val view_more: YoutubePage? = null,
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val apply_filter: Boolean = false,
    val content_padding: PaddingValues = PaddingValues(),
    val show_download_indicators: Boolean = true,
    val is_song_feed: Boolean = false
) {
    @Composable
    fun rememberFilteredItems(): State<List<MediaItem>> {
        return items.rememberFilteredItems(apply_filter, is_song_feed = is_song_feed)
    }
}

data class MediaItemGridParams(
    val rows: Pair<Int, Int>? = null,
    val alt_style: Boolean = false,
    val square_item_max_text_rows: Int? = null,
    val show_download_indicators: Boolean = true,
    val itemSizeProvider: @Composable () -> DpSize = { DpSize.Unspecified },
    val startContent: (LazyGridScope.() -> Unit)? = null
)

data class MediaItemListParams(
    val numbered: Boolean = false,
    val play_as_list: Boolean = false
)
