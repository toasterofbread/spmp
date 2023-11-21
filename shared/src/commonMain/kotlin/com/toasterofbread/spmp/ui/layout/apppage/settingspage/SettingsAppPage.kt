@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.settings.ui.SettingsInterface
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AuthSettings
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.model.settings.category.TopBarSettings
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState

internal const val PREFS_PAGE_EXTRA_PADDING_DP: Float = 10f

internal enum class PrefsPageScreen {
    ROOT,
    YOUTUBE_MUSIC_LOGIN,
    DISCORD_LOGIN,
    UI_DEBUG_INFO
}

class SettingsAppPage(override val state: AppPageState, footer_modifier: Modifier): AppPage() {
    private val pill_menu: PillMenu = PillMenu(follow_player = true)
    var current_category: SettingsCategory.Page? by mutableStateOf(null)
    val ytm_auth: SettingsValueState<Set<String>> =
        SettingsValueState<Set<String>>(
            AuthSettings.Key.YTM_AUTH.getName()
        ).init(Settings.prefs, Settings::provideDefault)
    val settings_interface: SettingsInterface =
        getPrefsPageSettingsInterface(
            state,
            pill_menu,
            ytm_auth,
            footer_modifier,
            { current_category },
            { current_category = null }
        )

    override fun onBackNavigation(): Boolean {
        if (current_category != null) {
            settings_interface.goBack()
            return true
        }
        return false
    }

    @Composable
    override fun ColumnScope.SFFPage(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        val player = LocalPlayerState.current
        val category_open by remember { derivedStateOf { current_category != null } }
        val show_reset_confirmation = remember { mutableStateOf(false) }

        ResetConfirmationDialog(
            show_reset_confirmation,
            {
                if (category_open) {
                    settings_interface.current_page.resetKeys()
                }
                else {
                    TODO("Reset keys in all categories (w/ different confirmation text)")
                }
            }
        )

        val extra_action: @Composable PillMenu.Action.(action_count: Int) -> Unit = remember {{
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Refresh
                ) {
                    show_reset_confirmation.value = true
                }
            }
        }}

        DisposableEffect(settings_interface.current_page) {
            if (settings_interface.current_page.id == PrefsPageScreen.ROOT.ordinal) {
                pill_menu.addExtraAction(action = extra_action)
            }
            else {
                pill_menu.removeExtraAction(extra_action)
            }

            onDispose {
                pill_menu.removeExtraAction(extra_action)
            }
        }

        Box(modifier) {
            pill_menu.PillMenu()

            Column(Modifier.fillMaxSize().padding(horizontal = PREFS_PAGE_EXTRA_PADDING_DP.dp)) {
                val top_padding: Dp = player.top_bar.MusicTopBar(
                    TopBarSettings.Key.SHOW_IN_SETTINGS,
                    Modifier.fillMaxWidth().zIndex(10f),
                    getBottomBorderColour = if (current_category == null) player.theme.background_provider else null,
                    padding = PaddingValues(top = content_padding.calculateTopPadding())
                ).top_padding

                Crossfade(category_open || settings_interface.current_page.id!! != PrefsPageScreen.ROOT.ordinal) { open ->
                    if (!open) {
                        SettingsTopPage(content_padding = content_padding, top_padding = top_padding)
                    }
                    else {
                        BoxWithConstraints(
                            Modifier.pointerInput(Unit) {}
                        ) {
                            val layout_direction: LayoutDirection = LocalLayoutDirection.current
                            CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                                settings_interface.Interface(
                                    Modifier.fillMaxSize(),
                                    content_padding = PaddingValues(
                                        top = top_padding,
                                        bottom = content_padding.calculateBottomPadding(),
                                        start = content_padding.calculateStartPadding(layout_direction),
                                        end = content_padding.calculateEndPadding(layout_direction)
                                    ),
                                    titleFooter = {
                                        WaveBorder()
                                    },
                                    page_top_padding = WAVE_BORDER_HEIGHT_DP.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
