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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.toastbits.composekit.settings.ui.SettingsInterface
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
import androidx.compose.runtime.MutableState
import dev.toastbits.composekit.utils.common.copy

internal const val PREFS_PAGE_EXTRA_PADDING_DP: Float = 10f

internal enum class PrefsPageScreen {
    ROOT,
    YOUTUBE_MUSIC_LOGIN,
    DISCORD_LOGIN,
    UI_DEBUG_INFO
}

class SettingsAppPage(override val state: AppPageState, getFooterModifier: @Composable () -> Modifier): AppPage() {
    private val pill_menu: PillMenu = PillMenu(follow_player = true)
    val ytm_auth: PreferencesProperty<Set<String>> = state.context.settings.youtube_auth.YTM_AUTH
    val settings_interface: SettingsInterface =
        getPrefsPageSettingsInterface(
            state,
            pill_menu,
            ytm_auth,
            getFooterModifier,
        )

    override fun onBackNavigation(): Boolean {
        return settings_interface.goBack()
    }

    override fun onReopened() {
        settings_interface.openPage(null)
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        val player: OldPlayerStateImpl = LocalPlayerState.current
        val show_reset_confirmation: MutableState<Boolean> = remember { mutableStateOf(false) }

        ResetConfirmationDialog(
            show_reset_confirmation,
            {
                settings_interface.current_page.resetKeys()
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

            Column(Modifier.fillMaxSize()) {
                val layout_direction: LayoutDirection = LocalLayoutDirection.current

                Crossfade(settings_interface.current_page.id != PrefsPageScreen.ROOT.ordinal) { open ->
                    if (!open) {
                        SettingsTopPage(
                            content_padding = content_padding.copy(
                                start = content_padding.calculateStartPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp,
                                end = content_padding.calculateEndPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp
                            ),
                            top_padding = content_padding.calculateTopPadding()
                        )
                    }
                    else {
                        BoxWithConstraints(
                            Modifier.pointerInput(Unit) {}
                        ) {
                            CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                                settings_interface.Interface(
                                    Modifier.fillMaxSize(),
                                    content_padding = content_padding.copy(
                                        start = content_padding.calculateStartPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp,
                                        end = content_padding.calculateEndPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp
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
