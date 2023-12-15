package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFilterCategoryItems

data object FilterSettings: SettingsCategory("filter") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_filter"),
            getString("s_cat_desc_filter"),
            { getFilterCategoryItems() }
        ) { Icons.Outlined.FilterAlt }

    enum class Key: SettingsKey {
        ENABLE,
        APPLY_TO_PLAYLIST_ITEMS,
        APPLY_TO_ARTISTS,
        APPLY_TO_ARTIST_ITEMS,
        TITLE_KEYWORDS;

        override val category: SettingsCategory get() = FilterSettings

        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        override fun <T> getDefaultValue(): T =
            when (this) {
                ENABLE -> true
                APPLY_TO_PLAYLIST_ITEMS -> false
                APPLY_TO_ARTISTS -> false
                APPLY_TO_ARTIST_ITEMS -> false
                TITLE_KEYWORDS -> emptySet<String>()
            } as T
    }
}
