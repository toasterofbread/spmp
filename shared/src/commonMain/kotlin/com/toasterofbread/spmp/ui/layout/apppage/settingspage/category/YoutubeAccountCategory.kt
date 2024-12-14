package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.platform.AppContext

internal fun getYoutubeAccountCategory(context: AppContext): List<SettingsItem> =
    listOf(
        ToggleSettingsItem(
            context.settings.Misc.ADD_SONGS_TO_HISTORY
        )
    )
