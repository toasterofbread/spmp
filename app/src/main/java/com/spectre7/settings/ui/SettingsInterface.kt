package com.spectre7.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spectre7.utils.Theme

class SettingsInterface(val theme: Theme, val root_page: Int, val page: (Int) -> SettingsPage?, val onBackPressed: () -> Unit = {}) {
    var current_page by mutableStateOf(root_page)
    private val page_stack = mutableListOf<Int>()

    @Composable
    fun Interface(modifier: Modifier = Modifier) {
        Crossfade(current_page, modifier = modifier) {
            page(it)?.Page(this, {
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