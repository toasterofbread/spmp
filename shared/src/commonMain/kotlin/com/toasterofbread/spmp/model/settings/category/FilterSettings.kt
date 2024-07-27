package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFilterCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_filter_enable
import spmp.shared.generated.resources.s_key_filter_apply_to_playlist_items
import spmp.shared.generated.resources.s_key_filter_apply_to_artists
import spmp.shared.generated.resources.s_key_filter_apply_to_artist_items
import spmp.shared.generated.resources.s_key_filter_title_keywords
import spmp.shared.generated.resources.s_sub_filter_title_keywords
import spmp.shared.generated.resources.s_cat_filter
import spmp.shared.generated.resources.s_cat_desc_filter

class FilterSettings(val context: AppContext): SettingsGroup("FILTER", context.getPrefs()) {
    val ENABLE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_enable) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val APPLY_TO_PLAYLIST_ITEMS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_playlist_items) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTISTS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_artists) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTIST_ITEMS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_artist_items) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val TITLE_KEYWORDS: PreferencesProperty<Set<String>> by property(
        getName = { stringResource(Res.string.s_key_filter_title_keywords) },
        getDescription = { stringResource(Res.string.s_sub_filter_title_keywords) },
        getDefaultValue = { emptySet() }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_filter) },
            { stringResource(Res.string.s_cat_desc_filter) },
            { getFilterCategoryItems(context) },
            { Icons.Outlined.FilterAlt }
        )
}
