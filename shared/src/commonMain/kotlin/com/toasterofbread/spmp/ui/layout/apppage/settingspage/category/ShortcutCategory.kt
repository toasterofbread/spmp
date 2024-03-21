package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutsEditor
import com.toasterofbread.spmp.model.settings.category.ShortcutSettings

internal fun getShortcutCategoryItems(): List<SettingsItem> =
    listOf(
        ComposableSettingsItem(
            listOf(
                ShortcutSettings.Key.CONFIGURED_SHORTCUTS.getName(),
            ),
            composable = {
                ShortcutsEditor(it)
            }
        )
    )
