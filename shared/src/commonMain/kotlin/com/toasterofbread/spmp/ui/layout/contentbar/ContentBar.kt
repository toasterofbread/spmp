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
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.Serializable

@Serializable
sealed class ContentBar {
    abstract fun getName(): String
    abstract fun getDescription(): String?
    abstract fun getIcon(): ImageVector

    @Composable
    fun Bar(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        val player: PlayerState = LocalPlayerState.current
        val slot_colour_source: ColourSource by slot.rememberColourSource()

        val background_colour: Color = slot_colour_source.get(player)

        var result: Boolean by remember { mutableStateOf(false) }

        CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
            result = BarContent(
                slot,
                slot_colour_source.theme_colour,
                content_padding,
                distance_to_page,
                modifier.background(background_colour)
            )
        }

        return result
    }

    @Composable
    protected abstract fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean

    interface BarSelectionState {
        val built_in_bars: List<ContentBarReference>
        val custom_bars: List<ContentBarReference>
        fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?)
        fun onColourSelected(slot: LayoutSlot, colour: ColourSource)
        fun onSlotConfigChanged(slot: LayoutSlot, config: JsonElement?)

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
    }
}

@Composable
fun LayoutSlot.DisplayBar(
    distance_to_page: Dp,
    modifier: Modifier = Modifier,
    container_modifier: Modifier = Modifier,
    onConfigDataChanged: (JsonElement?) -> Unit = {}
): Boolean {
    val player: PlayerState = LocalPlayerState.current
    val content_bar: ContentBar? by observeContentBar()

    val config_data: JsonElement? = observeConfigData()
    LaunchedEffect(config_data) {
        onConfigDataChanged(config_data)
    }

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

    Crossfade(getContentBarSelectionState(), container_modifier) { selection_state ->
        if (selection_state == null) {
            content_bar_result = content_bar?.Bar(this, content_padding, distance_to_page, modifier) ?: false
            return@Crossfade
        }

        val selctor_size: Dp = 50.dp
        val selector_modifier: Modifier =
            if (this.is_vertical) modifier.width(selctor_size)
            else modifier.height(selctor_size)

        ContentBarSelector(
            selection_state,
            this,
            config_data,
            content_padding,
            selector_modifier
        )
    }

    return getContentBarSelectionState() != null || content_bar_result
}
