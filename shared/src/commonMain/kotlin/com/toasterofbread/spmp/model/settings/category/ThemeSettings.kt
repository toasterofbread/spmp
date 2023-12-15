package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getThemeCategoryItems

data object ThemeSettings: SettingsCategory("theme") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_theme"),
            getString("s_cat_desc_theme"),
            { getThemeCategoryItems(it) }
        ) { Icons.Outlined.Palette }

    enum class Key: SettingsKey {
        CURRENT_THEME,
        THEMES,
        ACCENT_COLOUR_SOURCE,
        NOWPLAYING_THEME_MODE,
        NOWPLAYING_DEFAULT_GRADIENT_DEPTH;

        override val category: SettingsCategory get() = ThemeSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                ACCENT_COLOUR_SOURCE -> AccentColourSource.THUMBNAIL.ordinal
                CURRENT_THEME -> 0
                THEMES -> "[]"
                NOWPLAYING_THEME_MODE -> 0
                NOWPLAYING_DEFAULT_GRADIENT_DEPTH -> 1f
            } as T
    }
}

enum class AccentColourSource {
    THEME, THUMBNAIL
}
