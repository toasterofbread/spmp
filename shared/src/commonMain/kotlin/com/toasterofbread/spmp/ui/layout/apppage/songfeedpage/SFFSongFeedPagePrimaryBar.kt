package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.animation.Crossfade
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlin.Unit
import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import LocalPlayerState
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyRow
import com.toasterofbread.composekit.utils.composable.ScrollableRowOrColumn

@Composable
internal fun SongFeedAppPage.SFFSongFeedPagePrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues,
    lazy: Boolean
): Boolean {
    val chips: List<SongFeedFilterChip> = filter_chips ?: return false
    val player: PlayerState = LocalPlayerState.current

    ScrollableRowOrColumn(
        row = true,
        lazy = lazy,
        item_count = chips.size,
        modifier = modifier,
        arrangement = Arrangement.spacedBy(10.dp),
        alignment = 0,
        content_padding = content_padding
    ) { index ->
        val chip: SongFeedFilterChip = chips[index]

        Crossfade(index == selected_filter_chip) { is_selected ->
            ElevatedFilterChip(
                is_selected,
                {
                    selectFilterChip(index)
                },
                {
                    Text(chip.text.getString(player.context))
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
                    borderColor = player.theme.on_background,
                    enabled = true,
                    selected = is_selected
                )
            )
        }
    }

    return true
}
