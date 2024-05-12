package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource

abstract class BarColourState {
    enum class StatusBarLevel {
        BAR, PLAYER
    }
    val status_bar: State<StatusBarLevel> = State(StatusBarLevel.entries)

    enum class NavBarLevel {
        PLAYER, BAR, LPM
    }
    val nav_bar: State<NavBarLevel> = State(NavBarLevel.entries)

    protected abstract fun onCurrentStatusBarColourChanged(colour: Color?)
    protected abstract fun onCurrentNavigationBarColourChanged(colour: Color?)

    @Composable
    fun Update() {
        val status_bar_colour: Color? = status_bar.current_colour?.get(LocalPlayerState.current)
        LaunchedEffect(status_bar_colour) {
            onCurrentStatusBarColourChanged(status_bar_colour)
        }

        val nav_bar_colour: Color? = nav_bar.current_colour?.get(LocalPlayerState.current)
        LaunchedEffect(nav_bar_colour) {
            onCurrentNavigationBarColourChanged(nav_bar_colour)
        }
    }

    class State<T: Enum<*>>(private val entries: List<T>) {
        internal val level_colours: MutableMap<T, ColourSource?> = mutableMapOf()
        internal var current_colour: ColourSource? by mutableStateOf(null)

        fun setLevelColour(colour: ColourSource?, level: T) {
            level_colours[level] = colour

            for (colour_level in entries.reversed()) {
                val level_colour: ColourSource = level_colours[colour_level] ?: continue

                if (colour_level.ordinal <= level.ordinal) {
                    current_colour = level_colour
                }
                return
            }

            current_colour = null
        }
    }
}
