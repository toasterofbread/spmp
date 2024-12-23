package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.utils.composable.ScrollableRowOrColumn
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.ytmkt.endpoint.*

@Composable
internal fun SearchAppPage.SearchFiltersRow(
    modifier: Modifier,
    content_padding: PaddingValues = PaddingValues(),
    lazy: Boolean
) {
    val player: PlayerState = LocalPlayerState.current

    ScrollableRowOrColumn(
        row = true,
        lazy = lazy,
        item_count = SearchType.entries.size + 1,
        modifier = modifier.height(SEARCH_BAR_HEIGHT_DP.dp),
        arrangement = Arrangement.spacedBy(5.dp),
        content_padding = content_padding
    ) { index ->
        Crossfade(
            if (current_filter == null) index == 0
            else current_filter!!.ordinal == index - 1
        ) { selected ->
            ElevatedFilterChip(
                selected,
                {
                    if (index == 0) {
                        setFilter(null)
                    }
                    else {
                        setFilter(SearchType.entries[index - 1])
                    }
                },
                {
                    val search_type: SearchType? = if (index == 0) null else SearchType.entries[index - 1]
                    Text(search_type.getReadable())
                },
                colors = with(player.theme) {
                    FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = background,
                        labelColor = onBackground,
                        selectedContainerColor = accent,
                        selectedLabelColor = onAccent
                    )
                },
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = player.theme.onBackground,
                    enabled = true,
                    selected = selected
                )
            )
        }
    }
}
