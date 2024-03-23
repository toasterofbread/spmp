package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import androidx.compose.ui.graphics.Color
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.fromHexString
import androidx.compose.runtime.State
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import androidx.compose.runtime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import LocalPlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground

@Serializable
sealed interface ColourSource {
    fun get(player: PlayerState): Color
    val theme_colour: Theme.Colour? get() = null
}

@Serializable
internal data class ThemeColourSource(override val theme_colour: Theme.Colour): ColourSource {
    override fun get(player: PlayerState): Color = theme_colour.get(player.theme)
}

@Serializable
internal class PlayerBackgroundColourSource: ColourSource {
    override fun get(player: PlayerState): Color = player.getNPBackground()
}

@Serializable
internal data class CustomColourSource(val colour: Int): ColourSource {
    override fun get(player: PlayerState): Color = Color(colour)
}

@Composable
internal fun LayoutSlot.rememberColourSource(): State<ColourSource> {
    val player: PlayerState = LocalPlayerState.current
    val colours_data: String by LayoutSettings.Key.SLOT_COLOURS.rememberMutableState()

    return remember { derivedStateOf {
        val colours: Map<String, ColourSource> = Json.decodeFromString(colours_data)
        return@derivedStateOf colours[getKey()] ?: getDefaultBackgroundColour(player.theme)
    } }
}
