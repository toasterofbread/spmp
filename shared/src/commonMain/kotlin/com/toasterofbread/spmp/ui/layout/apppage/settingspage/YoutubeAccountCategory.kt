package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getYoutubeAccountCategory(): List<SettingsItem> =
    listOf(
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_ADD_SONGS_TO_HISTORY.name),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        )
    )
