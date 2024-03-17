package com.toasterofbread.spmp.ui.layout.apppage

import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.SongRelatedPage
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import dev.toastbits.ytmkt.model.external.YoutubePage
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

data class SongAppPage(
    override val state: AppPageState,
    override val item: Song,
    private val browse_params: YoutubePage.BrowseParamsData? = null
): AppPageWithItem() {
    private var previous_item: MediaItemHolder? by mutableStateOf(null)

    override fun onOpened(from_item: MediaItemHolder?) {
        super.onOpened(from_item)
        previous_item = from_item
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        SongRelatedPage(
            item,
            state.context.ytapi.SongRelatedContent,
            modifier,
            previous_item?.item,
            content_padding,
            close = close
        )
    }
}
