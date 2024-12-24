package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.State
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import LocalPlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.get

@Serializable
sealed interface ColourSource {
    fun get(player: PlayerState): Color
    val theme_colour: ThemeValues.Slot? get() = null
}

@Serializable
internal data class ThemeColourSource(override val theme_colour: ThemeValues.Slot): ColourSource {
    override fun get(player: PlayerState): Color = player.theme[theme_colour]
}

@Serializable
internal class PlayerBackgroundColourSource: ColourSource {
    override fun get(player: PlayerState): Color = player.getNPBackground()
}

@Serializable
data class CustomColourSource(val colour: Int): ColourSource {
    constructor(colour: Color): this(colour.toArgb())

    override fun get(player: PlayerState): Color = Color(colour)
}

@Composable
internal fun LayoutSlot.rememberColourSource(): State<ColourSource> {
    val player: PlayerState = LocalPlayerState.current
    val colours: Map<String, ColourSource> by player.settings.Layout.SLOT_COLOURS.observe()

    return remember { derivedStateOf {
        colours[getKey()] ?: getDefaultBackgroundColour(player.theme)
    } }
}
