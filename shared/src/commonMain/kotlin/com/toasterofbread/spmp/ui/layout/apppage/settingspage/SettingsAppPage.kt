@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import dev.toastbits.composekit.navigation.navigator.BaseNavigator
import dev.toastbits.composekit.navigation.navigator.CurrentScreen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsGroupScreen

internal const val PREFS_PAGE_EXTRA_PADDING_DP: Float = 10f

// TEMP
interface NewSettingsPage: Screen {
    var id: Int?
    suspend fun resetKeys()
}

class SettingsAppPage(override val state: AppPageState): AppPage() {
    private val pill_menu: PillMenu = PillMenu(follow_player = true)
    val ytm_auth: PlatformSettingsProperty<Set<String>> = state.context.settings.YoutubeAuth.YTM_AUTH

    private class Temp: NewSettingsPage {
        override var id: Int? = null
        override suspend fun resetKeys() {}

        @Composable
        override fun Content(
            navigator: Navigator,
            modifier: Modifier,
            contentPadding: PaddingValues
        ) {}
    }

    // TODO | Display pill menu(?)
    private val navigator: Navigator = BaseNavigator(Temp(), false)
    private val currentScreen: NewSettingsPage
        get() = navigator.currentScreen as NewSettingsPage

    override fun onBackNavigation(): Boolean {
        if (navigator.getNavigateBackwardCount() >= 1) {
            navigator.navigateBackward(1)
            return true
        }
        return false
    }

    override fun onReopened() {
        // TODO
    }

    fun openScreen(screen: Screen) {
        navigator.pushScreen(screen)
    }

    fun openGroup(group: SettingsGroup) {
        openScreen(PlatformSettingsGroupScreen(group))
    }

    fun goBack() {
        navigator.navigateBackward(1)
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        navigator.CurrentScreen(modifier, content_padding)

//        val player: PlayerState = LocalPlayerState.current
//        val show_reset_confirmation: MutableState<Boolean> = remember { mutableStateOf(false) }
//
//        ResetConfirmationDialog(
//            show_reset_confirmation,
//            {
//                currentScreen.resetKeys()
//            }
//        )
//
//        val extra_action: @Composable PillMenu.Action.(action_count: Int) -> Unit = remember {{
//            if (it == 1) {
//                ActionButton(
//                    Icons.Filled.Refresh
//                ) {
//                    show_reset_confirmation.value = true
//                }
//            }
//        }}
//
//        DisposableEffect(currentScreen) {
//            if (currentScreen.id == PrefsPageScreen.ROOT.ordinal) {
//                pill_menu.addExtraAction(action = extra_action)
//            }
//            else {
//                pill_menu.removeExtraAction(extra_action)
//            }
//
//            onDispose {
//                pill_menu.removeExtraAction(extra_action)
//            }
//        }
//
//        Box(modifier) {
//            pill_menu.PillMenu()
//
//            Column(Modifier.fillMaxSize()) {
//                val layout_direction: LayoutDirection = LocalLayoutDirection.current
//
//                Crossfade(currentScreen.id != PrefsPageScreen.ROOT.ordinal) { open ->
//                    if (!open) {
//                        SettingsTopPage(
//                            content_padding = content_padding.copy(
//                                start = content_padding.calculateStartPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp,
//                                end = content_padding.calculateEndPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp
//                            ),
//                            top_padding = content_padding.calculateTopPadding()
//                        )
//                    }
//                    else {
//                        BoxWithConstraints(
//                            Modifier.pointerInput(Unit) {}
//                        ) {
//                            CompositionLocalProvider(LocalContentColor provides player.theme.onBackground) {
//                                navigator.CurrentScreen(
//                                    Modifier.fillMaxSize(),
//                                    contentPadding =
//                                        content_padding.copy(
//                                            start = content_padding.calculateStartPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp,
//                                            end = content_padding.calculateEndPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp
//                                        )
//                                )
//
////                                settings_interface.Interface(
////                                    Modifier.fillMaxSize(),
////                                    content_padding = content_padding.copy(
////                                        start = content_padding.calculateStartPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp,
////                                        end = content_padding.calculateEndPadding(layout_direction) + PREFS_PAGE_EXTRA_PADDING_DP.dp
////                                    ),
////                                    titleFooter = {
////                                        WaveBorder()
////                                    },
////                                    page_top_padding = WAVE_BORDER_HEIGHT_DP.dp
////                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }
}
