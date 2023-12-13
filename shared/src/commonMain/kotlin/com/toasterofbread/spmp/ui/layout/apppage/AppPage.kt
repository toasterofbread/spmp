package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyRow
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

abstract class AppPageWithItem : AppPage() {
    abstract val item: MediaItemHolder
}

abstract class AppPage {
    abstract val state: AppPageState

    @Composable
    abstract fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    )

    @Composable
    open fun showTopBar() = false

    @Composable
    open fun showTopBarContent() = false
    @Composable
    open fun TopBarContent(
        modifier: Modifier,
        close: () -> Unit
    ) { }

    open fun onOpened(from_item: MediaItemHolder? = null) {}
    open fun onClosed(next_page: AppPage?) {}
    open fun onBackNavigation(): Boolean = false

    open fun canReload(): Boolean = false
    open fun onReload() {}

    @Composable
    open fun isReloading(): Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FilterChipsRow(
        chip_count: Int,
        isChipSelected: (Int) -> Boolean,
        onChipSelected: (Int) -> Unit,
        modifier: Modifier = Modifier,
        show_scrollbar: Boolean = false,
        spacing: Dp = 10.dp,
        chipContent: @Composable (Int) -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current

        ScrollBarLazyRow(
            modifier,
            show_scrollbar = show_scrollbar,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chip_count) { index ->
                Crossfade(isChipSelected(index)) { selected ->
                    ElevatedFilterChip(
                        selected,
                        {
                            onChipSelected(index)
                        },
                        { chipContent(index) },
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
}
