package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import LocalPlayerState
import dev.toastbits.composekit.theme.core.ThemeValues
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.content_bar_primary
import spmp.shared.generated.resources.content_bar_desc_primary
import spmp.shared.generated.resources.content_bar_secondary
import spmp.shared.generated.resources.content_bar_desc_secondary

@Serializable
sealed class InternalContentBar(val index: Int): ContentBar() {
    companion object {
        val PRIMARY: InternalContentBar = PrimaryInternalContentBar(0)
        val SECONDARY: InternalContentBar = SecondaryInternalContentBar(1)

        val ALL: List<InternalContentBar> = listOf(PRIMARY, SECONDARY)
    }
}

private class PrimaryInternalContentBar(index: Int): InternalContentBar(index) {
    @Composable
    override fun getName(): String = stringResource(Res.string.content_bar_primary)
    @Composable
    override fun getDescription(): String = stringResource(Res.string.content_bar_desc_primary)
    override fun getIcon(): ImageVector = Icons.Default.LooksOne

    @Composable
    override fun isDisplaying(): Boolean =
        LocalPlayerState.current.app_page.shouldShowPrimaryBarContent()

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: ThemeValues.Slot?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        val page: AppPage = LocalPlayerState.current.app_page
        if (page.shouldShowPrimaryBarContent()) {
            if (!page.PrimaryBarContent(slot, content_padding, distance_to_page, lazy, modifier)) {
                return false
            }
            return true
        }

        return false
    }
}

private class SecondaryInternalContentBar(index: Int): InternalContentBar(index) {
    @Composable
    override fun getName(): String = stringResource(Res.string.content_bar_secondary)
    @Composable
    override fun getDescription(): String = stringResource(Res.string.content_bar_desc_secondary)
    override fun getIcon(): ImageVector = Icons.Default.LooksTwo

    @Composable
    override fun isDisplaying(): Boolean =
        LocalPlayerState.current.app_page.shouldShowSecondaryBarContent()

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: ThemeValues.Slot?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        val page: AppPage = LocalPlayerState.current.app_page
        if (page.shouldShowSecondaryBarContent()) {
            if (!page.SecondaryBarContent(slot, content_padding, distance_to_page, lazy, modifier)) {
                return false
            }
            return true
        }
        return false
    }
}
