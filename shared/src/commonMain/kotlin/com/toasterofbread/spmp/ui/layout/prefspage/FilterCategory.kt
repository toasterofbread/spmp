package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import com.toasterofbread.settings.ui.item.SettingsGroupItem
import com.toasterofbread.settings.ui.item.SettingsItem
import com.toasterofbread.settings.ui.item.SettingsComposableItem
import com.toasterofbread.settings.ui.item.SettingsDropdownItem
import com.toasterofbread.settings.ui.item.SettingsSliderItem
import com.toasterofbread.settings.ui.item.SettingsToggleItem
import com.toasterofbread.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getLanguageName
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.WidthShrinkText

internal fun getFilterCategory(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FILTER_ENABLE.name),
            getString("s_key_filter_enable"), null
        ),

        SettingsStringSetItem(
            SettingsValueState(Settings.KEY_FILTER_TITLE_KEYWORDS.name),
            getString("s_key_filter_title_keywords"), getString("s_sub_filter_title_keywords")
        )
    )
}
