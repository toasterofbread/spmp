package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LargeFilterList
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.apppage.searchpage.getReadable
import com.toasterofbread.spmp.ui.layout.apppage.searchpage.getIcon
import dev.toastbits.ytmkt.endpoint.*
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.components.utils.modifier.vertical

@Composable
internal fun SearchAppPage.VerticalSearchPrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues,
    lazy: Boolean
) {
    val player: PlayerState = LocalPlayerState.current

    LargeFilterList(
        SearchType.entries.size + 1,
        getItemText = { index ->
            val search_type: SearchType? = if (index == 0) null else SearchType.entries[index - 1]
            return@LargeFilterList search_type.getReadable()
        },
        getItemIcon = { index ->
            val search_type: SearchType? = if (index == 0) null else SearchType.entries[index - 1]
            return@LargeFilterList search_type.getIcon()
        },
        isItemSelected = { index ->
            if (current_filter == null) index == 0
            else current_filter!!.ordinal == index - 1
        },
        modifier = modifier.width(125.dp),
        content_padding = content_padding,
        onSelected = { index ->
            if (index == 0) {
                setFilter(null)
            }
            else {
                setFilter(SearchType.entries[index - 1])
            }
        },
        lazy = lazy
    )
}
