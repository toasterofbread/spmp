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

class SettingsInterface(val theme: Theme, val root_page: Int, val page: (Int) -> SettingsPage, val onBackPressed: () -> Unit = {}) {
    var current_page by mutableStateOf(root_page)
    private val page_stack = mutableListOf<Int>()

    @Composable
    fun Interface() {
        Crossfade(current_page) {
            page(it).Page({
                page_stack.add(current_page)
                current_page = it
            }, {
                if (page_stack.size > 0) {
                    current_page = page_stack.removeLast()
                }
                else {
                    onBackPressed()
                }
            })
        }
    }
}