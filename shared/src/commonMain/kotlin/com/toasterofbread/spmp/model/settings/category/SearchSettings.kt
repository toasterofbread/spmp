package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSearchCategoryItems
import com.toasterofbread.spmp.platform.AppContext
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.Icons
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_search_search_for_non_music
import spmp.shared.generated.resources.s_sub_search_search_for_non_music

class SearchSettings(val context: AppContext): SettingsGroup("SEARCH", context.getPrefs()) {
    val SEARCH_FOR_NON_MUSIC: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_search_search_for_non_music) },
        getDescription = { stringResource(Res.string.s_sub_search_search_for_non_music) },
        getDefaultValue = { false }
    )

    fun getItems(): List<SettingsItem> = getSearchCategoryItems(context)

    override val page: CategoryPage? = null
}
