package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.json.Json

internal typealias ContentBarReference = Pair<ContentBar, Int>

sealed class ContentBar {
    abstract fun getName(): String
    abstract fun getDescription(): String?
    abstract fun getIcon(): ImageVector

    @Composable
    fun Bar(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        val player: PlayerState = LocalPlayerState.current
        val slot_colour_source: ColourSource by slot.rememberColourSource()

        val background_colour: Color = slot_colour_source.get(player.theme)

        var result: Boolean by remember { mutableStateOf(false) }

        CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
            result = BarContent(
                slot,
                slot_colour_source.theme_colour,
                content_padding,
                modifier.background(background_colour)
            )
        }

        return result
    }

    @Composable
    protected abstract fun BarContent(slot: LayoutSlot, background_colour: Theme.Colour?, content_padding: PaddingValues, modifier: Modifier): Boolean

    interface BarSelectionState {
        val built_in_bars: List<ContentBarReference>
        val custom_bars: List<ContentBarReference>
        fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?)
        fun onThemeColourSelected(slot: LayoutSlot, colour: Theme.Colour)
        fun onCustomColourSelected(slot: LayoutSlot, colour: Color)

        fun createCustomBar(): ContentBarReference
        fun deleteCustomBar(bar: ContentBarReference)
        fun onCustomBarEditRequested(bar: ContentBarReference)
    }

    companion object {
        var _bar_selection_state: BarSelectionState? by mutableStateOf(null)
        var bar_selection_state: BarSelectionState?
            get() = if (disable_bar_selection) null else _bar_selection_state
            set(value) { _bar_selection_state = value }

        var disable_bar_selection: Boolean by mutableStateOf(false)

        fun deserialise(data: String): ContentBar {
            val internal_bar_index: Int? = data.toIntOrNull()
            if (internal_bar_index != null) {
                return InternalContentBar.getAll()[internal_bar_index]
            }

            return Json.decodeFromString<CustomContentBar>(data)
        }
    }
}

@Composable
fun LayoutSlot.DisplayBar(modifier: Modifier = Modifier, container_modifier: Modifier = Modifier): Boolean {
    val player: PlayerState = LocalPlayerState.current
    val content_bar: ContentBar? by observeContentBar()

    val base_padding: Dp = 5.dp
    val content_padding: PaddingValues = PaddingValues(
        top = base_padding,
        start = base_padding,
        end = base_padding,
        bottom = base_padding + (
                if (is_vertical) player.nowPlayingBottomPadding(
                    include_np = true,
                    include_top_items = false
                )
                else 0.dp
            )
    )

    var content_bar_result: Boolean by remember { mutableStateOf(false) }

    Crossfade(ContentBar.bar_selection_state, container_modifier) { selection_state ->
        if (selection_state == null) {
            content_bar_result = content_bar?.Bar(this, content_padding, modifier) ?: false
            return@Crossfade
        }

        val selctor_size: Dp = 50.dp
        val selector_modifier: Modifier =
            if (this.is_vertical) modifier.width(selctor_size)
            else modifier.height(selctor_size)

        ContentBarSelector(
            selection_state,
            this,
            content_padding,
            selector_modifier
        )
    }

    return ContentBar.bar_selection_state != null || content_bar_result
}
