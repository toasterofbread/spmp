package com.spectre7.composesettings.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.Crossfade
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.spectre7.utils.Theme

class SettingsInterface(val theme: Theme, val root_page: Int, val page: (Int) -> SettingsPage, val onBackPressed: () -> Unit = {}) {
    var current_page by mutableStateOf(root_page)
    private val page_stack = mutableListOf<Int>()

    @Composable
    fun Interface() {
        Crossfade(current_page) {
            page(it).Page(this, {
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