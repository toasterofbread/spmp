package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.FilterSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem

internal fun getFilterCategoryItems(): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            SettingsValueState(FilterSettings.Key.ENABLE.getName()),
            getString("s_key_filter_enable"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(FilterSettings.Key.APPLY_TO_PLAYLIST_ITEMS.getName()),
            getString("s_key_filter_apply_to_playlist_items"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(FilterSettings.Key.APPLY_TO_ARTISTS.getName()),
            getString("s_key_filter_apply_to_artists"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(FilterSettings.Key.APPLY_TO_ARTIST_ITEMS.getName()),
            getString("s_key_filter_apply_to_artist_items"), null
        ),

        AppStringSetItem(
            SettingsValueState(FilterSettings.Key.TITLE_KEYWORDS.getName()),
            getString("s_key_filter_title_keywords"), getString("s_sub_filter_title_keywords"),
            getString("s_filter_title_keywords_dialog_title")
        )
    )
}
