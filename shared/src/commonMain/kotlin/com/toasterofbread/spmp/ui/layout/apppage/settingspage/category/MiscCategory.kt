package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.TextFieldSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.platform.AppContext

internal fun getMiscCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        AppSliderItem(
            context.settings.misc.NAVBAR_HEIGHT_MULTIPLIER
        ),

        TextFieldSettingsItem(
            context.settings.misc.STATUS_WEBHOOK_URL,
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.misc.STATUS_WEBHOOK_PAYLOAD,
            getFieldModifier = { Modifier.appTextField() }
        )
    ) + getCachingGroup(context)
}

private fun getCachingGroup(context: AppContext): List<SettingsItem> {
    return listOf(
        GroupSettingsItem(getString("s_group_caching")),
        ToggleSettingsItem(
            context.settings.misc.THUMB_CACHE_ENABLED
        )
    )
}
