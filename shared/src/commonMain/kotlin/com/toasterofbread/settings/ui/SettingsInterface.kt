package com.toasterofbread.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.copy

class SettingsInterface(
    val themeProvider: () -> Theme,
    private val root_page: Int,
    val prefs: PlatformPreferences,
    val default_provider: (String) -> Any,
    val pill_menu: PillMenu? = null,
    private val getPage: (Int, Any?) -> SettingsPage,
    private val onPageChanged: ((page: Int?) -> Unit)? = null,
    private val onCloseRequested: (() -> Unit)? = null
) {
    val theme: Theme get() = themeProvider()
    var current_page: SettingsPage by mutableStateOf(getUserPage(root_page, null))
        private set
    private val page_stack = mutableListOf<SettingsPage>()

    private fun getUserPage(page_id: Int, param: Any?): SettingsPage {
        return getPage(page_id, param).also { page ->
            page.id = page_id
            page.settings_interface = this
        }
    }

    suspend fun goBack() {
        pill_menu?.clearAlongsideActions()
        pill_menu?.clearExtraActions()
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

    fun openPage(target_page: SettingsPage) {
        if (target_page != current_page) {
            target_page.settings_interface = this@SettingsInterface
            page_stack.add(current_page)
            current_page = target_page
            onPageChanged?.invoke(current_page.id)
        }
    }

    fun openPageById(page_id: Int, param: Any?) {
        openPage(getUserPage(page_id, param))
    }

    @Composable
    fun Interface(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(0.dp)) {
        Crossfade(current_page, modifier = modifier) { page ->
            var width by remember { mutableStateOf(0) }

            Column(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { width = it.width }
            ) {
                var go_back by remember { mutableStateOf(false) }
                LaunchedEffect(go_back) {
                    if (go_back) {
                        goBack()
                    }
                }

                page.TitleBar(
                    page.id == root_page,
                    Modifier.zIndex(10f).padding(content_padding.copy(bottom = 0.dp))
                ) {
                    go_back = true
                }

                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    page.Page(
                        content_padding.copy(top = WAVE_BORDER_DEFAULT_HEIGHT.dp),
                        { target_page_id, param ->
                            if (current_page.id != target_page_id) {
                                page_stack.add(current_page)
                                current_page = getUserPage(target_page_id, param)
                                onPageChanged?.invoke(current_page.id)
                            }
                        },
                        this@SettingsInterface::openPage,
                        { go_back = true }
                    )
                }
            }
        }
    }
}