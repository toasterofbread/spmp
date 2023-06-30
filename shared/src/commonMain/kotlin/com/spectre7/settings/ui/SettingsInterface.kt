package com.toasterofbread.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.thenIf

class SettingsInterface(
    val themeProvider: () -> Theme,
    private val root_page: Int,
    val context: PlatformContext,
    val prefs: ProjectPreferences,
    val default_provider: (String) -> Any,
    val pill_menu: PillMenu? = null,
    private val getPage: (Int) -> SettingsPage,
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

    fun openPage(target_page: SettingsPage) {
        if (target_page != current_page) {
            target_page.settings_interface = this@SettingsInterface
            page_stack.add(current_page)
            current_page = target_page
            onPageChanged?.invoke(current_page.id)
        }
    }

    fun openPageById(page_id: Int) {
        openPage(getUserPage(page_id))
    }

    @Composable
    fun Interface(height: Dp, modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(0.dp)) {
        Crossfade(current_page, modifier = modifier.requiredHeight(height)) { page ->
            var width by remember { mutableStateOf(0) }

            Column(
                Modifier
                    .fillMaxSize()
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
                    contentAlignment = Alignment.TopCenter
                ) {
                    page.Page(
                        if (!page.disable_padding) content_padding else PaddingValues(0.dp),
                        { target_page_id ->
                            if (current_page.id != target_page_id) {
                                page_stack.add(current_page)
                                current_page = getUserPage(target_page_id)
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