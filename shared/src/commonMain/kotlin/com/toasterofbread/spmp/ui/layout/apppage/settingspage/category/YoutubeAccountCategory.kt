package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.resources.getString

internal fun getYoutubeAccountCategory(): List<SettingsItem> =
    listOf(
        ToggleSettingsItem(
            SettingsValueState(SystemSettings.Key.ADD_SONGS_TO_HISTORY.getName()),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        )
    )
