package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsStringSetItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getFilterCategory(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FILTER_ENABLE.name),
            getString("s_key_filter_enable"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS.name),
            getString("s_key_filter_apply_to_playlist_items"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FILTER_APPLY_TO_ARTISTS.name),
            getString("s_key_filter_apply_to_artists"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FILTER_APPLY_TO_ARTIST_ITEMS.name),
            getString("s_key_filter_apply_to_artist_items"), null
        ),

        SettingsStringSetItem(
            SettingsValueState(Settings.KEY_FILTER_TITLE_KEYWORDS.name),
            getString("s_key_filter_title_keywords"), getString("s_sub_filter_title_keywords"),
            getString("s_filter_title_keywords_dialog_title")
        )
    )
}
