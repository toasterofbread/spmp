package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSearchCategoryItems
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.platform.AppContext
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.Icons
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty

class SearchSettings(val context: AppContext): SettingsGroup("SEARCH", context.getPrefs()) {
    val SEARCH_FOR_NON_MUSIC: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_search_search_for_non_music") },
        getDescription = { getString("s_sub_search_search_for_non_music") },
        getDefaultValue = { false }
    )

    fun getItems(): List<SettingsItem> = getSearchCategoryItems(context)

    override val page: CategoryPage? = null
}
