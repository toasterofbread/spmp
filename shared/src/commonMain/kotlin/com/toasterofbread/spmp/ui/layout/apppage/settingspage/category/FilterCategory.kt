package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import com.toasterofbread.spmp.platform.AppContext
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_filter_title_keywords_dialog_title

internal fun getFilterCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.Filter.ENABLE
        ),

        ToggleSettingsItem(
            context.settings.Filter.APPLY_TO_PLAYLIST_ITEMS
        ),

        ToggleSettingsItem(
            context.settings.Filter.APPLY_TO_ARTISTS
        ),

        ToggleSettingsItem(
            context.settings.Filter.APPLY_TO_ARTIST_ITEMS
        ),

        AppStringSetItem(
            context.settings.Filter.TITLE_KEYWORDS,
            Res.string.s_filter_title_keywords_dialog_title
        )
    )
}
