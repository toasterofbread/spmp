package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot

@Composable
internal fun SearchAppPage.HorizontalSearchPrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues,
    lazy: Boolean
) {
    SearchFiltersRow(modifier.height(60.dp), content_padding, lazy)
}
