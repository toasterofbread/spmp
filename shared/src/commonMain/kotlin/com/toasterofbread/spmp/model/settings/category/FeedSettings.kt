package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFeedCategoryItems

data object FeedSettings: SettingsCategory("feed") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_feed"),
            getString("s_cat_desc_feed"),
            { getFeedCategoryItems() }
        ) { Icons.Outlined.FormatListBulleted }

    enum class Key: SettingsKey {
        SHOW_SONG_DOWNLOAD_INDICATORS,
        INITIAL_ROWS,
        SQUARE_PREVIEW_TEXT_LINES,
        GRID_ROW_COUNT,
        GRID_ROW_COUNT_EXPANDED,
        LANDSCAPE_GRID_ROW_COUNT,
        LANDSCAPE_GRID_ROW_COUNT_EXPANDED,
        SHOW_RADIOS,
        HIDDEN_ROWS;

        override val category: SettingsCategory get() = FeedSettings

        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        override fun <T> getDefaultValue(): T =
            when (this) {
                SHOW_SONG_DOWNLOAD_INDICATORS -> false
                INITIAL_ROWS -> 4
                SQUARE_PREVIEW_TEXT_LINES -> if (Platform.DESKTOP.isCurrent()) 2 else 2
                GRID_ROW_COUNT -> if (Platform.DESKTOP.isCurrent()) 1 else 2
                GRID_ROW_COUNT_EXPANDED -> if (Platform.DESKTOP.isCurrent()) 1 else 2
                LANDSCAPE_GRID_ROW_COUNT -> if (Platform.DESKTOP.isCurrent()) 1 else 1
                LANDSCAPE_GRID_ROW_COUNT_EXPANDED -> if (Platform.DESKTOP.isCurrent()) 1 else 1
                SHOW_RADIOS -> false
                HIDDEN_ROWS -> emptySet<String>()
            } as T
    }
}
