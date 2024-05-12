package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.Icons
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getShortcutCategoryItems
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.model.appaction.shortcut.Shortcut
import com.toasterofbread.spmp.model.appaction.shortcut.getDefaultShortcuts
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import dev.toastbits.composekit.platform.PreferencesProperty

class ShortcutSettings(val context: AppContext): SettingsGroup("SHORTCUT", context.getPrefs()) {
    val CONFIGURED_SHORTCUTS: PreferencesProperty<List<Shortcut>?> by nullableSerialisableProperty(
        getName = { getString("s_key_configured_shortcuts") },
        getDescription = { null },
        getDefaultValue = { null }
    )
    val NAVIGATE_SONG_WITH_NUMBERS: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_navigate_song_with_numbers") },
        getDescription = { null },
        getDefaultValue = { true }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_shortcut") },
            { getString("s_cat_desc_shortcut") },
            { getShortcutCategoryItems(context) },
            { Icons.Outlined.Adjust }
        )
}
