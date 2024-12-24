package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.getContrasted
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import dev.toastbits.composekit.theme.core.ThemeValues
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.Serializable

@Serializable
sealed class ContentBar {
    @Composable
    abstract fun getName(): String
    @Composable
    abstract fun getDescription(): String?
    abstract fun getIcon(): ImageVector

    @Composable
    fun Bar(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier = Modifier,
        base_content_padding: PaddingValues = PaddingValues(),
        getParentBackgroundColour: () -> Color? = { null },
        getBackgroundColour: (Color) -> Color = { it }
    ): Boolean {
        val player: PlayerState = LocalPlayerState.current
        val slot_colour_source: ColourSource by slot.rememberColourSource()

        var result: Boolean by remember { mutableStateOf(false) }

        val background_colour: Color = getBackgroundColour(slot_colour_source.get(player))
        val parent_background_colour: Color? = getParentBackgroundColour()

        val actual_background_colour: Color =
            if (parent_background_colour == null) background_colour
            else background_colour.compositeOver(parent_background_colour)

        CompositionLocalProvider(LocalContentColor provides actual_background_colour.getContrasted()) {
            result = BarContent(
                slot,
                slot_colour_source.theme_colour,
                base_content_padding,
                distance_to_page = distance_to_page,
                lazy = true,
                modifier = modifier.background(background_colour).padding(content_padding)
            )
        }

        return result
    }

    @Composable
    abstract fun isDisplaying(): Boolean

    @Composable
    abstract fun BarContent(
        slot: LayoutSlot,
        background_colour: ThemeValues.Slot?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean

    interface BarSelectionState {
        val built_in_bars: List<ContentBarReference>
        val custom_bars: List<ContentBarReference>
        suspend fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?)
        fun onColourSelected(slot: LayoutSlot, colour: ColourSource)
        fun onSlotConfigChanged(slot: LayoutSlot, config: JsonElement?)

        suspend fun createCustomBar(): ContentBarReference
        suspend fun deleteCustomBar(bar: ContentBarReference)
        fun onCustomBarEditRequested(bar: ContentBarReference)
    }

    companion object {
        private var _bar_selection_states: MutableList<BarSelectionState> = mutableStateListOf()
        val bar_selection_state: BarSelectionState?
            get() = _bar_selection_states.lastOrNull()

        fun addBarSelectionState(state: BarSelectionState) {
            _bar_selection_states.add(state)
        }
        fun removeBarSelectionState(state: BarSelectionState) {
            _bar_selection_states.remove(state)
        }

        var disable_bar_selection: Boolean by mutableStateOf(false)
    }
}

@Composable
fun LayoutSlot.DisplayBar(
    distance_to_page: Dp,
    modifier: Modifier = Modifier,
    container_modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    getParentBackgroundColour: () -> Color? = { null },
    getBackgroundColour: (Color) -> Color = { it },
    onConfigDataChanged: (JsonElement?) -> Unit = {}
): Boolean {
    val player: PlayerState = LocalPlayerState.current
    val content_bar: ContentBar? by observeContentBar()

    val config_data: JsonElement? = observeConfigData()
    LaunchedEffect(config_data) {
        onConfigDataChanged(config_data)
    }

    val base_padding: Dp = 5.dp
    val base_content_padding: PaddingValues = PaddingValues(
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

    Crossfade(getContentBarSelectionState(), container_modifier) { selection_state ->
        if (selection_state == null) {
            content_bar_result = content_bar?.Bar(
                this,
                base_content_padding = base_content_padding,
                distance_to_page = distance_to_page,
                modifier = modifier,
                getParentBackgroundColour = getParentBackgroundColour,
                getBackgroundColour = getBackgroundColour,
                content_padding = content_padding
            ) ?: false
            return@Crossfade
        }

        val selctor_size: Dp = 50.dp
        val selector_modifier: Modifier =
            if (this.is_vertical) modifier.width(selctor_size)
            else modifier.height(selctor_size + content_padding.calculateTopPadding() + content_padding.calculateBottomPadding())

        ContentBarSelector(
            selection_state,
            this,
            selector_modifier,
            slot_config = config_data,
            base_content_padding = base_content_padding,
            content_padding = content_padding
        )
    }

    return getContentBarSelectionState() != null || content_bar_result
}
