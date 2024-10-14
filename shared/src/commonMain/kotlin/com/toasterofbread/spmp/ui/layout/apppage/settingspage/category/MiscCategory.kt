package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.TextFieldSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.platform.AppContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_caching

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
        GroupSettingsItem(Res.string.s_group_caching),
        ToggleSettingsItem(
            context.settings.misc.THUMB_CACHE_ENABLED
        )
    )
}
