package com.spectre7.composesettings.ui

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.utils.Theme

class SettingsInterface(
    val theme: Theme, 
    val root_page: Int, 
    val getPage: (Int) -> SettingsPage?, 
    val context: Context,
    val onPageChanged: ((page: Int) -> Unit)? = null,
    val onCloseRequested: (() -> Unit)? = null
) {
    private var current_page by mutableStateOf(root_page)
    private val page_stack = mutableListOf<Int>()

    fun goBack() {
        if (page_stack.size > 0) {
            val target_page = page_stack.removeLast()
            if (current_page != target_page) {
                current_page = target_page
                onPageChanged?.invoke(current_page)
            }
        }
        else {
            onCloseRequested?.invoke()
        }
    }

    @Composable
    fun Interface(modifier: Modifier = Modifier) {
        Crossfade(current_page, modifier = modifier) {
            val page = getPage(it)
            Column(Modifier.padding(top = 18.dp, start = 20.dp, end = 20.dp)) {
                page?.TitleBar(this@SettingsInterface, it == root_page, { goBack() })
                LazyColumn(Modifier.fillMaxHeight()) {
                    item {
                        Box(Modifier.padding(bottom = 60.dp)) {
                            page?.Page(this@SettingsInterface, { target_page ->
                                if (current_page != target_page) {
                                    page_stack.add(current_page)
                                    current_page = target_page
                                    onPageChanged?.invoke(current_page)
                                }
                            }, { goBack() })
                        }
                    }
                }
            }
        }
    }
}