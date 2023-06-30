package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.settings.model.SettingsItem
import com.toasterofbread.settings.model.SettingsItemToggle
import com.toasterofbread.settings.model.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getLibraryCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_SHOW_LIKES_PLAYLIST.name),
            getString("s_key_show_likes_playlist"), null
        )
    )
}