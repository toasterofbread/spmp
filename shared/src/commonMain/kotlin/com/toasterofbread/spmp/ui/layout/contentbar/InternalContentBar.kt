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
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.getTop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo

sealed class InternalContentBar(
    val ordinal: Int
): ContentBar() {
    @Composable
    override fun BarContent(slot: LayoutSlot, modifier: Modifier) {
        Row(modifier) {
            Text("$ordinal")
        }
    }

    companion object {
        val PRIMARY: InternalContentBar = PrimaryInternalContentBar()
        val SECONDARY: InternalContentBar = SecondaryInternalContentBar()
        val NAVIGATION: InternalContentBar = NavigationInternalContentBar()

        fun getAll(): List<InternalContentBar> = listOf(PRIMARY, SECONDARY, NAVIGATION)
        val REQUIRED: List<InternalContentBar> = listOf(PRIMARY, SECONDARY)
    }
}

private class PrimaryInternalContentBar: InternalContentBar(0) {
    override fun getName(): String = "Primary // TODO"
    override fun getDescription(): String = "An informative description of this bar's function // TODO"
    override fun getIcon(): ImageVector = Icons.Default.LooksOne
}

private class SecondaryInternalContentBar: InternalContentBar(1) {
    override fun getName(): String = "Secondary // TODO"
    override fun getDescription(): String = "An informative description of this bar's function // TODO"
    override fun getIcon(): ImageVector = Icons.Default.LooksTwo
}

private class NavigationInternalContentBar: InternalContentBar(2) {
    override fun getName(): String = "Navigation // TODO"
    override fun getDescription(): String = "An informative description of this bar's function // TODO"
    override fun getIcon(): ImageVector = Icons.Default.Widgets

    @Composable
    override fun BarContent(slot: LayoutSlot, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        AppPageSidebar(
            modifier,
            content_padding = PaddingValues(
                top = 10.dp + WindowInsets.getTop(),
                bottom = 10.dp,
                start = 10.dp,
                end = 10.dp
            ),
            multiselect_context = player.main_multiselect_context
        )
    }
}
