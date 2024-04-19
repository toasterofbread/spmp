package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import dev.toastbits.composekit.platform.Platform
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getBehaviourCategoryItems

data object BehaviourSettings: SettingsCategory("behaviour") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_behaviour"),
            getString("s_cat_desc_behaviour"),
            { getBehaviourCategoryItems() },
            { Icons.Outlined.TouchApp }
        )

    enum class Key: SettingsKey {
        OPEN_NP_ON_SONG_PLAYED,
        START_RADIO_ON_SONG_PRESS,
        MULTISELECT_CANCEL_ON_ACTION,
        MULTISELECT_CANCEL_ON_NONE_SELECTED, // TODO
        TREAT_SINGLES_AS_SONG,
        TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE,
        SHOW_LIKES_PLAYLIST,
        SEARCH_SHOW_SUGGESTIONS,
        STOP_PLAYER_ON_APP_CLOSE,
        LPM_CLOSE_ON_ACTION,
        LPM_INCREMENT_PLAY_AFTER,
        DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL;

        override val category: SettingsCategory get() = BehaviourSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                OPEN_NP_ON_SONG_PLAYED -> true
                START_RADIO_ON_SONG_PRESS -> true
                MULTISELECT_CANCEL_ON_ACTION -> true
                MULTISELECT_CANCEL_ON_NONE_SELECTED -> true
                TREAT_SINGLES_AS_SONG -> false
                TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE -> false
                SHOW_LIKES_PLAYLIST -> true
                SEARCH_SHOW_SUGGESTIONS -> true
                STOP_PLAYER_ON_APP_CLOSE -> false
                LPM_CLOSE_ON_ACTION -> true
                LPM_INCREMENT_PLAY_AFTER -> true
                DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL -> false
            } as T

        override fun isHidden(): Boolean =
            when (this) {
                DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL -> !Platform.DESKTOP.isCurrent()
                else -> false
            }
    }
}
