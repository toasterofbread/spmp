package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import com.toasterofbread.spmp.platform.AppContext

internal fun getFilterCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.filter.ENABLE
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_PLAYLIST_ITEMS
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_ARTISTS
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_ARTIST_ITEMS
        ),

        AppStringSetItem(
            context.settings.filter.TITLE_KEYWORDS,
            getString("s_filter_title_keywords_dialog_title")
        )
    )
}
