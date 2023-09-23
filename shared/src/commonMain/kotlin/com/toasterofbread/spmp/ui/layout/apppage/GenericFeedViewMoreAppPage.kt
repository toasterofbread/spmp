package com.toasterofbread.spmp.ui.layout.apppage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.GenericFeedViewMorePage

data class GenericFeedViewMoreAppPage(
    override val state: AppPageState,
    private val browse_id: String,
    private val title: String?,
): AppPage() {
    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        GenericFeedViewMorePage(browse_id, modifier, content_padding = content_padding, title = title)
    }
}
