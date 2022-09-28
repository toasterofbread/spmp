package com.spectre7.composesettings.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.Crossfade
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.spectre7.utils.Theme

abstract class SettingsInterfaceState(initial_page: SettingsPage? = null) {
    var _current_page: SettingsPage? by mutableStateOf(null)
    var current_page: SettingsPage?
        get() = _current_page
        set(page: SettingsPage?) {
            _current_page = page
            if (page != null) {
                page.interface_state = this
            }
        }

    init {
        current_page = initial_page
    }

    abstract fun getTheme(): Theme
}

class SettingsInterface<PageState>(val theme: Theme, initial_page: PageState, val page: (PageState) -> SettingsPage) {
    var page_state by mutableStateOf(initial_page)

    @Composable
    fun Interface() {
        // Crossfade(page_state) {
            page(page_state).Page<PageState>({ page_state = it })
        // }
    }
}

// @Composable
// fun SettingsInterface(state: SettingsInterfaceState, modifier: Modifier = Modifier) {
//     // Crossfade(state.current_page) {
//         state.current_page?.GetPage(modifier.padding(30.dp))
//     // }
// }