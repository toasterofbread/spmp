package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.ui.graphics.Color
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.fromHexString
import androidx.compose.runtime.State
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.*
import kotlinx.serialization.json.Json
import LocalPlayerState

interface ColourSource {
    fun get(theme: Theme): Color
    val theme_colour: Theme.Colour? get() = null

    companion object {
        fun fromSlotColour(colour: String): ColourSource? {
            if (colour.startsWith("#")) {
                try {
                    return CustomColourSource(Color.fromHexString(colour))
                }
                catch (_: Throwable) {
                    return null
                }
            }

            val theme_colour_index: Int = colour.toIntOrNull()
                ?: return null
            val theme_colour: Theme.Colour = Theme.Colour.entries.getOrNull(theme_colour_index)
                ?: return null

            return ThemeColourSource(theme_colour)
        }
    }
}
internal class ThemeColourSource(override val theme_colour: Theme.Colour): ColourSource {
    override fun get(theme: Theme): Color = theme_colour.get(theme)
}
internal class CustomColourSource(val colour: Color): ColourSource {
    override fun get(theme: Theme): Color = colour
}

@Composable
internal fun LayoutSlot.rememberColourSource(): State<ColourSource> {
    val player: PlayerState = LocalPlayerState.current
    val colours_data: String by LayoutSettings.Key.SLOT_COLOURS.rememberMutableState()

    return remember { derivedStateOf {
        val colours: Map<String, String> = Json.decodeFromString(colours_data)
        return@derivedStateOf colours[getKey()]?.let { colour ->
            ColourSource.fromSlotColour(colour)
        } ?: getDefaultBackgroundColour(player.theme)
    } }
}
