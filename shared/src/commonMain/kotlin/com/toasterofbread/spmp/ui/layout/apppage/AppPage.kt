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
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.composekit.utils.composable.ScrollBarLazyRowOrColumn

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
    open fun PrimaryBarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean = false
    @Composable
    open fun SecondaryBarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean = false

    open fun onOpened(from_item: MediaItemHolder? = null) {}
    open fun onReopened() {}
    open fun onClosed(next_page: AppPage?) {}
    open fun onBackNavigation(): Boolean = false

    open fun canReload(): Boolean = false
    open fun onReload() {}

    @Composable
    open fun isReloading(): Boolean = false
}
