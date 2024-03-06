package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyRow
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.endpoint.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchAppPage.SearchFiltersRow(
    modifier: Modifier,
    content_padding: PaddingValues = PaddingValues()
) {
    val player: PlayerState = LocalPlayerState.current

    ScrollBarLazyRow(
        modifier.height(SEARCH_BAR_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = content_padding
    ) {
        items(SearchType.entries.size + 1) { index ->
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
                            labelColor = on_background,
                            selectedContainerColor = accent,
                            selectedLabelColor = on_accent
                        )
                    },
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = player.theme.on_background
                    )
                )
            }
        }
    }
}
