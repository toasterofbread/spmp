package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.resources.getString
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebar
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import com.toasterofbread.composekit.utils.composable.getTop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo

sealed class InternalContentBar(
    val ordinal: Int
): ContentBar() {
    companion object {
        val PRIMARY: InternalContentBar = PrimaryInternalContentBar()
        val SECONDARY: InternalContentBar = SecondaryInternalContentBar()
        val NAVIGATION: InternalContentBar = NavigationInternalContentBar()

        fun getAll(): List<InternalContentBar> = listOf(PRIMARY, SECONDARY, NAVIGATION)
        val REQUIRED: List<InternalContentBar> = listOf(PRIMARY, SECONDARY)
    }
}

private class PrimaryInternalContentBar: InternalContentBar(0) {
    override fun getName(): String = getString("content_bar_primary")
    override fun getDescription(): String = getString("content_bar_desc_primary")
    override fun getIcon(): ImageVector = Icons.Default.LooksOne

    @Composable
    override fun BarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        return LocalPlayerState.current.app_page.PrimaryBarContent(slot, content_padding, modifier)
    }
}

private class SecondaryInternalContentBar: InternalContentBar(1) {
    override fun getName(): String = getString("content_bar_secondary")
    override fun getDescription(): String = getString("content_bar_desc_secondary")
    override fun getIcon(): ImageVector = Icons.Default.LooksTwo

    @Composable
    override fun BarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        return LocalPlayerState.current.app_page.SecondaryBarContent(slot, content_padding, modifier)
    }
}

private class NavigationInternalContentBar: InternalContentBar(2) {
    override fun getName(): String = getString("content_bar_navigation")
    override fun getDescription(): String = getString("content_bar_desc_navigation")
    override fun getIcon(): ImageVector = Icons.Default.Widgets

    @Composable
    override fun BarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        val player: PlayerState = LocalPlayerState.current

        // TODO
        AppPageSidebar(
            slot,
            modifier,
            content_padding = PaddingValues(
                top = WindowInsets.getTop() + content_padding.calculateTopPadding(),
                bottom = content_padding.calculateBottomPadding(),
                start = content_padding.calculateStartPadding(LocalLayoutDirection.current),
                end = content_padding.calculateEndPadding(LocalLayoutDirection.current)
            ),
            multiselect_context = player.main_multiselect_context
        )

        return true
    }
}
