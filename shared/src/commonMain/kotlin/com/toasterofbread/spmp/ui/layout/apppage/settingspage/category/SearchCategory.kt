package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import com.toasterofbread.spmp.platform.AppContext

internal fun getSearchCategoryItems(context: AppContext): List<SettingsItem> =
    listOf(
        ToggleSettingsItem(
            context.settings.Search.SEARCH_FOR_NON_MUSIC
        )
    )
