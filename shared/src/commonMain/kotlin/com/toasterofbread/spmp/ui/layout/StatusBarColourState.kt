package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource

abstract class StatusBarColourState {
    enum class Level {
        BAR, PLAYER
    }

    protected abstract fun onCurrentStatusBarColourChanged(colour: Color?)

    @Composable
    fun Update() {
        val colour: Color? = current_colour?.get(LocalPlayerState.current)
        LaunchedEffect(colour) {
            onCurrentStatusBarColourChanged(colour)
        }
    }

    fun setLevelColour(colour: ColourSource?, level: Level) {
        level_colours[level] = colour

        for (colour_level in Level.entries.reversed()) {
            val level_colour: ColourSource = level_colours[colour_level] ?: continue

            if (colour_level.ordinal <= level.ordinal) {
                current_colour = level_colour
                return
            }
        }

        current_colour = null
    }

    private val level_colours: MutableMap<Level, ColourSource?> = mutableMapOf()
    private var current_colour: ColourSource? by mutableStateOf(null)
}
