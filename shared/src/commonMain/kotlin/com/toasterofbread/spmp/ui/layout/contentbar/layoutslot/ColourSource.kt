package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.State
import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import LocalPlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import dev.toastbits.composekit.settings.ui.ThemeValues

@Serializable
sealed interface ColourSource {
    fun get(player: OldPlayerStateImpl): Color
    val theme_colour: ThemeValues.Colour? get() = null
}

@Serializable
internal data class ThemeColourSource(override val theme_colour: ThemeValues.Colour): ColourSource {
    override fun get(player: OldPlayerStateImpl): Color = theme_colour.get(player.theme)
}

@Serializable
internal class PlayerBackgroundColourSource: ColourSource {
    override fun get(player: OldPlayerStateImpl): Color = player.getNPBackground()
}

@Serializable
data class CustomColourSource(val colour: Int): ColourSource {
    constructor(colour: Color): this(colour.toArgb())

    override fun get(player: OldPlayerStateImpl): Color = Color(colour)
}

@Composable
internal fun LayoutSlot.rememberColourSource(): State<ColourSource> {
    val player: OldPlayerStateImpl = LocalPlayerState.current
    val colours: Map<String, ColourSource> by player.settings.layout.SLOT_COLOURS.observe()

    return remember { derivedStateOf {
        colours[getKey()] ?: getDefaultBackgroundColour(player.theme)
    } }
}
