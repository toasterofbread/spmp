@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import dev.toastbits.composekit.components.utils.composable.pane.model.InitialPaneRatioSource
import dev.toastbits.composekit.navigation.navigator.BaseNavigator
import dev.toastbits.composekit.navigation.navigator.CurrentScreen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsGroupScreen
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsScreen

class SettingsAppPage(override val state: AppPageState): AppPage() {
    fun openScreen(screen: Screen) {
        settingsScreen.internalNavigator.pushScreen(screen)
    }

    fun openGroup(group: SettingsGroup) {
        openScreen(PlatformSettingsGroupScreen(group))
    }

    fun goBack() {
        navigator.navigateBackward(1)
    }

    private val settingsScreen: PlatformSettingsScreen =
        PlatformSettingsScreen(
            state.context.settings.preferences,
            state.context.settings.groups_with_page,
            initialStartPaneRatioSource =
                InitialPaneRatioSource.Remembered(
                    "com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage",
                    InitialPaneRatioSource.Ratio(0.4f)
                ),
            displayExtraButtonsAboveGroups = true
        )

    private val navigator: Navigator =
        BaseNavigator(
            initialScreen = settingsScreen,
            extraButtonsHandledExternally = true
        )

    override fun onBackNavigation(): Boolean {
        if (navigator.getNavigateBackwardCount() >= 1) {
            navigator.navigateBackward(1)
            return true
        }
        return false
    }

    override fun onClosed(next_page: AppPage?, going_back: Boolean) {
        settingsScreen.reset()
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        navigator.CurrentScreen(modifier, content_padding)
    }
}
