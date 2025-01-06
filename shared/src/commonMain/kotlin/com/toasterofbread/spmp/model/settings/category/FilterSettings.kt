package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFilterCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_filter
import spmp.shared.generated.resources.s_cat_filter
import spmp.shared.generated.resources.s_key_filter_apply_to_artist_items
import spmp.shared.generated.resources.s_key_filter_apply_to_artists
import spmp.shared.generated.resources.s_key_filter_apply_to_playlist_items
import spmp.shared.generated.resources.s_key_filter_enable
import spmp.shared.generated.resources.s_key_filter_title_keywords
import spmp.shared.generated.resources.s_sub_filter_title_keywords

class FilterSettings(val context: AppContext): SettingsGroupImpl("FILTER", context.getPrefs()) {
    val ENABLE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_enable) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val APPLY_TO_PLAYLIST_ITEMS: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_playlist_items) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTISTS: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_artists) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val APPLY_TO_ARTIST_ITEMS: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_filter_apply_to_artist_items) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val TITLE_KEYWORDS: PlatformSettingsProperty<Set<String>> by property(
        getName = { stringResource(Res.string.s_key_filter_title_keywords) },
        getDescription = { stringResource(Res.string.s_sub_filter_title_keywords) },
        getDefaultValue = { emptySet() }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_filter)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_filter)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.FilterAlt

    override fun getConfigurationItems(): List<SettingsItem> = getFilterCategoryItems(context)
}
