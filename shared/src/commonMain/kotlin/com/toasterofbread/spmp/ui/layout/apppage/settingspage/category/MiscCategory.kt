package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.GroupSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.TextFieldSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.MiscSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem

internal fun getMiscCategoryItems(): List<SettingsItem> {
    return listOf(
        AppSliderItem(
            SettingsValueState(MiscSettings.Key.NAVBAR_HEIGHT_MULTIPLIER.getName()),
            getString("s_key_navbar_height_multiplier"),
            getString("s_sub_navbar_height_multiplier")
        ),

        TextFieldSettingsItem(
            SettingsValueState(MiscSettings.Key.STATUS_WEBHOOK_URL.getName()),
            getString("s_key_status_webhook_url"),
            getString("s_sub_status_webhook_url")
        ),

        TextFieldSettingsItem(
            SettingsValueState(MiscSettings.Key.STATUS_WEBHOOK_PAYLOAD.getName()),
            getString("s_key_status_webhook_payload"),
            getString("s_sub_status_webhook_payload")
        )
    ) + getCachingGroup()
}

private fun getCachingGroup(): List<SettingsItem> {
    return listOf(
        GroupSettingsItem(getString("s_group_caching")),
        ToggleSettingsItem(
            SettingsValueState(MiscSettings.Key.THUMB_CACHE_ENABLED.getName()),
            getString("s_key_enable_thumbnail_cache"), null
        )
    )
}
