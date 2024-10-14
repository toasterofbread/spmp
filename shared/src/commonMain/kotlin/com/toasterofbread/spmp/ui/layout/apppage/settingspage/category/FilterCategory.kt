package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import com.toasterofbread.spmp.platform.AppContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_filter_title_keywords_dialog_title

internal fun getFilterCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.filter.ENABLE
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_PLAYLIST_ITEMS
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_ARTISTS
        ),

        ToggleSettingsItem(
            context.settings.filter.APPLY_TO_ARTIST_ITEMS
        ),

        AppStringSetItem(
            context.settings.filter.TITLE_KEYWORDS,
            Res.string.s_filter_title_keywords_dialog_title
        )
    )
}
