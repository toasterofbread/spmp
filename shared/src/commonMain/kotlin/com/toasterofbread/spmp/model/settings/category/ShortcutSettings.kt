package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.Icons
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getShortcutCategoryItems
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString

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
        CONFIGURED_SHORTCUTS;

        override val category: SettingsCategory get() = ShortcutSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                CONFIGURED_SHORTCUTS -> "[]"
            } as T
    }
}
