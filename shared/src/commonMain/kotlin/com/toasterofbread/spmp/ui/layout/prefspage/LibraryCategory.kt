package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.settings.ui.item.SettingsItem
import com.toasterofbread.settings.ui.item.SettingsToggleItem
import com.toasterofbread.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getLibraryCategory(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_SHOW_LIKES_PLAYLIST.name),
            getString("s_key_show_likes_playlist"), null
        )
    )
}