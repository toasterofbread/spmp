package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ComposableSettingsItem
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutsEditor
import com.toasterofbread.spmp.platform.AppContext

internal fun getShortcutCategoryItems(context: AppContext): List<SettingsItem> =
    listOf(
        ComposableSettingsItem(
            listOf(
                context.settings.shortcut.CONFIGURED_SHORTCUTS,
                context.settings.shortcut.NAVIGATE_SONG_WITH_NUMBERS
            ),
            composable = {
                ShortcutsEditor(it)
            }
        )
    )
