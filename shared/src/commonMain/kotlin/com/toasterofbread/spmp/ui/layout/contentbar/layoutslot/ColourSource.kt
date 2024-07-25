package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.State
import LocalAppState
import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.model.state.UiState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import dev.toastbits.composekit.settings.ui.ThemeValues

@Serializable
sealed interface ColourSource {
    fun get(state: UiState): Color
    val theme_colour: ThemeValues.Colour? get() = null
}

@Serializable
internal data class ThemeColourSource(override val theme_colour: ThemeValues.Colour): ColourSource {
    override fun get(state: UiState): Color = theme_colour.get(state.theme)
}

@Serializable
internal class PlayerBackgroundColourSource: ColourSource {
    override fun get(state: UiState): Color = state.getNPBackground()
}

@Serializable
data class CustomColourSource(val colour: Int): ColourSource {
    constructor(colour: Color): this(colour.toArgb())

    override fun get(state: UiState): Color = Color(colour)
}

@Composable
internal fun LayoutSlot.rememberColourSource(): State<ColourSource> {
    val state: SpMp.State = LocalAppState.current
    val colours: Map<String, ColourSource> by state.settings.layout.SLOT_COLOURS.observe()

    return remember { derivedStateOf {
        colours[getKey()] ?: getDefaultBackgroundColour(state.theme)
    } }
}
