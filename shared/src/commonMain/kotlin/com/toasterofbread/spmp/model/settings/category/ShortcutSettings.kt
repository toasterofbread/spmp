package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.Icons
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getShortcutCategoryItems
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.model.appaction.shortcut.Shortcut
import com.toasterofbread.spmp.model.appaction.shortcut.getDefaultShortcuts
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

data object ShortcutSettings: SettingsCategory("shortcut") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_shortcut"),
            getString("s_cat_desc_shortcut"),
            { getShortcutCategoryItems() },
            { Icons.Outlined.Adjust }
        )

    enum class Key: SettingsKey {
        // List<Shortcut>
        CONFIGURED_SHORTCUTS,
        NAVIGATE_SONG_WITH_NUMBERS;

        override val category: SettingsCategory get() = ShortcutSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                CONFIGURED_SHORTCUTS -> Json.encodeToString(getDefaultShortcuts())
                NAVIGATE_SONG_WITH_NUMBERS -> true
            } as T
    }
}
