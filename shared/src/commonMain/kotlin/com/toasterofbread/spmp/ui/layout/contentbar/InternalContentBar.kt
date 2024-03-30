package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.resources.getString
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElement
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.serialization.Serializable

@Serializable
sealed class InternalContentBar(val index: Int): ContentBar() {
    companion object {
        val PRIMARY: InternalContentBar = PrimaryInternalContentBar(0)
        val SECONDARY: InternalContentBar = SecondaryInternalContentBar(1)
        val NAVIGATION: InternalContentBar = NavigationInternalContentBar(2)
        val LYRICS: InternalContentBar = LyricsInternalContentBar(3)
        val SONG_ACTIONS: InternalContentBar = SongActionsInternalContentBar(4)

        val ALL: List<InternalContentBar> = listOf(PRIMARY, SECONDARY, NAVIGATION, LYRICS, SONG_ACTIONS)
    }
}

private class PrimaryInternalContentBar(index: Int): InternalContentBar(index) {
    override fun getName(): String = getString("content_bar_primary")
    override fun getDescription(): String = getString("content_bar_desc_primary")
    override fun getIcon(): ImageVector = Icons.Default.LooksOne

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        return LocalPlayerState.current.app_page.PrimaryBarContent(slot, content_padding, distance_to_page, modifier)
    }
}

private class SecondaryInternalContentBar(index: Int): InternalContentBar(index) {
    override fun getName(): String = getString("content_bar_secondary")
    override fun getDescription(): String = getString("content_bar_desc_secondary")
    override fun getIcon(): ImageVector = Icons.Default.LooksTwo

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        return LocalPlayerState.current.app_page.SecondaryBarContent(slot, content_padding, distance_to_page, modifier)
    }
}

private class NavigationInternalContentBar(index: Int): InternalContentBar(index) {
    override fun getName(): String = getString("content_bar_navigation")
    override fun getDescription(): String = getString("content_bar_desc_navigation")
    override fun getIcon(): ImageVector = Icons.Default.Widgets

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        val elements: List<ContentBarElement> = remember { CustomContentBarTemplate.NAVIGATION.getElements() }
        return CustomBarContent(elements, 50.dp, slot.is_vertical, content_padding, background_colour, modifier)
    }
}

private class LyricsInternalContentBar(index: Int): InternalContentBar(index) {
    override fun getName(): String = getString("content_bar_lyrics")
    override fun getDescription(): String = getString("content_bar_desc_lyrics")
    override fun getIcon(): ImageVector = Icons.Default.Lyrics

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        val elements: List<ContentBarElement> = remember { CustomContentBarTemplate.LYRICS.getElements() }
        return CustomBarContent(elements, 50.dp, slot.is_vertical, content_padding, background_colour, modifier)
    }
}

private class SongActionsInternalContentBar(index: Int): InternalContentBar(index) {
    override fun getName(): String = getString("content_bar_song_actions")
    override fun getDescription(): String = getString("content_bar_desc_song_actions")
    override fun getIcon(): ImageVector = Icons.Default.PlayArrow

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        val elements: List<ContentBarElement> = remember { CustomContentBarTemplate.SONG_ACTIONS.getElements() }
        return CustomBarContent(elements, 50.dp, slot.is_vertical, content_padding, background_colour, modifier)
    }
}
