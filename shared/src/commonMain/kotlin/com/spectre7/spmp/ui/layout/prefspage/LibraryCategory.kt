package com.spectre7.spmp.ui.layout.prefspage

import com.spectre7.settings.model.SettingsItem
import com.spectre7.settings.model.SettingsItemToggle
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getString

internal fun getLibraryCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_SHOW_LIKES_PLAYLIST.name),
            getString("s_key_show_likes_playlist"), null
        )
    )
}