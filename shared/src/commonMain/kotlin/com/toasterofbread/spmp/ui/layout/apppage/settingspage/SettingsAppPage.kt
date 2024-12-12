@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import dev.toastbits.composekit.components.utils.composable.pane.model.InitialPaneRatioSource
import dev.toastbits.composekit.navigation.navigator.BaseNavigator
import dev.toastbits.composekit.navigation.navigator.CurrentScreen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsGroupScreen
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsScreen

class SettingsAppPage(override val state: AppPageState): AppPage() {
    private val navigator: Navigator = BaseNavigator(
        PlatformSettingsScreen(
            state.context.settings.prefs,
            state.context.settings.groups_with_page,
            initialStartPaneRatioSource =
                InitialPaneRatioSource.Remembered(
                    "com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage",
                    InitialPaneRatioSource.Ratio(0.4f)
                ),
            displayExtraButtonsAboveGroups = true
        ),
        isTopLevel = false,
        extraButtonsHandledExternally = true
    )

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
    }
}
