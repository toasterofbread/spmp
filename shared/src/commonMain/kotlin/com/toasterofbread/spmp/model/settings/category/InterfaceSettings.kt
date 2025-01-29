package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.commonsettings.impl.group.impl.ComposeKitSettingsGroupInterfaceImpl
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.LocaleSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.composekit.util.model.LocaleList
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.language_name
import spmp.shared.generated.resources.s_key_data_lang
import spmp.shared.generated.resources.s_sub_data_lang

class InterfaceSettings(
    context: AppContext
): ComposeKitSettingsGroupInterfaceImpl(
    "INTERFACE",
    context.getPrefs(),
    Res.string.language_name
), SettingsGroup {
    val DATA_LOCALE: PlatformSettingsProperty<Locale?> by nullableSerialisableProperty(
        getName = { stringResource(Res.string.s_key_data_lang) },
        getDescription = { stringResource(Res.string.s_sub_data_lang) },
        getDefaultValue = { null }
    )

    override fun getConfigurationItems(): List<SettingsItem> {
        val items: MutableList<SettingsItem> = super.getConfigurationItems().toMutableList()

        val uiLocaleIndex: Int =
            items.indexOfFirst { (it as? LocaleSettingsItem)?.state == UI_LOCALE }
        check(uiLocaleIndex != -1) { items.toList() }

        val dataLocaleItem: SettingsItem =
            LocaleSettingsItem(
                DATA_LOCALE,
                localeList = LocaleList.Localised(localeNameResource),
                allowCustomLocale = true
            )

        items.add(uiLocaleIndex + 1, dataLocaleItem)
        return items
    }
}
