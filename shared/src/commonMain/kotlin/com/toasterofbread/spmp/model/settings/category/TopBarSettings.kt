package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Waves
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getTopBarCategoryItems

data object TopBarSettings: SettingsCategory("topbar") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_topbar"),
            getString("s_cat_desc_topbar"),
            { getTopBarCategoryItems() }
        ) { Icons.Outlined.Waves }

    enum class Key: SettingsKey {
        LYRICS_LINGER,
        VISUALISER_WIDTH,
        SHOW_LYRICS_IN_QUEUE,
        SHOW_VISUALISER_IN_QUEUE,
        DISPLAY_OVER_ARTIST_IMAGE,
        LYRICS_ENABLE,
        LYRICS_MAX_LINES,
        LYRICS_PREAPPLY_MAX_LINES,
        LYRICS_SHOW_FURIGANA,
        SHOW_IN_LIBRARY,
        SHOW_IN_RADIOBUILDER,
        SHOW_IN_SETTINGS,
        SHOW_IN_LOGIN,
        SHOW_IN_PLAYLIST,
        SHOW_IN_ARTIST,
        SHOW_IN_VIEWMORE,
        SHOW_IN_SEARCH;

        override val category: SettingsCategory get() = TopBarSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                LYRICS_LINGER -> true
                VISUALISER_WIDTH -> 0.9f
                SHOW_LYRICS_IN_QUEUE -> true
                SHOW_VISUALISER_IN_QUEUE -> false
                DISPLAY_OVER_ARTIST_IMAGE -> false
                LYRICS_ENABLE -> true
                LYRICS_MAX_LINES -> 3
                LYRICS_PREAPPLY_MAX_LINES -> false
                LYRICS_SHOW_FURIGANA -> true
                SHOW_IN_LIBRARY -> true
                SHOW_IN_RADIOBUILDER -> true
                SHOW_IN_SETTINGS -> true
                SHOW_IN_LOGIN -> true
                SHOW_IN_PLAYLIST -> true
                SHOW_IN_ARTIST -> true
                SHOW_IN_VIEWMORE -> true
                SHOW_IN_SEARCH -> true
            } as T
    }
}
