package com.spectre7.composesettings.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.utils.Theme
import kotlin.math.absoluteValue

class SettingsInterface(
    val theme: Theme,
    private val root_page: Int,
    val context: Context,
    val prefs: SharedPreferences,
    val default_provider: (String) -> Any,
    private val getPage: (Int) -> SettingsPage,
    private val onPageChanged: ((page: Int?) -> Unit)? = null,
    private val onCloseRequested: (() -> Unit)? = null
) {
    var current_page: SettingsPage by mutableStateOf(getUserPage(root_page))
        private set
    private val page_stack = mutableListOf<SettingsPage>()

    private fun getUserPage(page_id: Int): SettingsPage {
        return getPage(page_id).also { page ->
            page.id = page_id
            page.settings_interface = this
        }
    }

    fun goBack() {
        if (page_stack.size > 0) {
            val target_page = page_stack.removeLast()
            if (current_page != target_page) {
                current_page = target_page
                onPageChanged?.invoke(current_page.id)
            }
        }
        else {
            onCloseRequested?.invoke()
        }
    }

    @Composable
    fun Interface(modifier: Modifier = Modifier) {
        Crossfade(current_page, modifier = modifier) { page ->
            Column(Modifier.padding(top = 18.dp, start = 20.dp, end = 20.dp)) {
                page.TitleBar(page.id == root_page, { goBack() })
                LazyColumn(Modifier.fillMaxHeight()) {
                    item {
                        Box(Modifier.padding(bottom = 60.dp)) {
                            page.Page(
                                { target_page ->
                                    if (current_page.id != target_page) {
                                        page_stack.add(current_page)
                                        current_page = getUserPage(target_page)
                                        onPageChanged?.invoke(current_page.id)
                                    }
                                },
                                { target_page ->
                                    if (current_page != target_page) {
                                        target_page.settings_interface = this@SettingsInterface
                                        page_stack.add(current_page)
                                        current_page = target_page
                                        onPageChanged?.invoke(current_page.id)
                                    }
                                },
                                { goBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}