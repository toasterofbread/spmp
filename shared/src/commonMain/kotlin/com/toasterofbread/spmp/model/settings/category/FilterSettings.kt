package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFilterCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty

class FilterSettings(val context: AppContext): SettingsGroup("FILTER", context.getPrefs()) {
    val ENABLE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_filter_enable") },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val APPLY_TO_PLAYLIST_ITEMS: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_filter_apply_to_playlist_items") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTISTS: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_filter_apply_to_artists") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTIST_ITEMS: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_filter_apply_to_artist_items") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val TITLE_KEYWORDS: PreferencesProperty<Set<String>> by property(
        getName = { getString("s_key_filter_title_keywords") },
        getDescription = { getString("s_sub_filter_title_keywords") },
        getDefaultValue = { emptySet() }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_filter") },
            { getString("s_cat_desc_filter") },
            { getFilterCategoryItems(context) },
            { Icons.Outlined.FilterAlt }
        )
}
