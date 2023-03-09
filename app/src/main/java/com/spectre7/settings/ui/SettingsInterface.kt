package com.spectre7.composesettings.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.getScreenHeight
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.MeasureUnconstrainedView
import com.spectre7.utils.thenIf

class SettingsInterface(
    val themeProvider: () -> Theme,
    private val root_page: Int,
    val context: Context,
    val prefs: SharedPreferences,
    val default_provider: (String) -> Any,
    private val getPage: (Int) -> SettingsPage,
    val pill_menu: PillMenu? = null,
    private val onPageChanged: ((page: Int?) -> Unit)? = null,
    private val onCloseRequested: (() -> Unit)? = null
) {
    val theme: Theme get() = themeProvider()
    var current_page: SettingsPage by mutableStateOf(getUserPage(root_page))
        private set
    private val page_stack = mutableListOf<SettingsPage>()

    private fun getUserPage(page_id: Int): SettingsPage {
        return getPage(page_id).also { page ->
            page.id = page_id
            page.settings_interface = this
        }
    }

    suspend fun goBack() {
        if (page_stack.size > 0) {
            val target_page = page_stack.removeLast()
            if (current_page != target_page) {
                current_page.onClosed()
                current_page = target_page
                onPageChanged?.invoke(current_page.id)
            }
        }
        else {
            onCloseRequested?.invoke()
        }
    }

    @Composable
    fun Interface(height: Dp, modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(0.dp)) {
        Crossfade(current_page, modifier = modifier.requiredHeight(height)) { page ->
            var width by remember { mutableStateOf(0) }

            Column(
                Modifier
                    .thenIf(!page.disable_padding, Modifier.padding(top = 18.dp, start = 20.dp, end = 20.dp))
                    .onSizeChanged { width = it.width }
            ) {

                var go_back by remember { mutableStateOf(false) }
                LaunchedEffect(go_back) {
                    if (go_back) {
                        goBack()
                    }
                }

                page.TitleBar(page.id == root_page, Modifier.requiredHeight(30.dp)) { go_back = true }

                Box(
                    Modifier
                        .thenIf(page.scrolling, Modifier.verticalScroll(remember { ScrollState(0) }))
                        .thenIf(!page.disable_padding, Modifier.padding(content_padding))
                ) {
                    page.Page(
                        { target_page_id ->
                            if (current_page.id != target_page_id) {
                                page_stack.add(current_page)
                                current_page = getUserPage(target_page_id)
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
                        { go_back = true }
                    )
                }
            }
        }
    }
}