package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ComposableSettingsItem
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutsEditor
import com.toasterofbread.spmp.platform.AppContext

internal fun getShortcutCategoryItems(context: AppContext): List<SettingsItem> =
    listOf(
        ComposableSettingsItem(
            listOf(
                context.settings.Shortcut.CONFIGURED_SHORTCUTS,
                context.settings.Shortcut.NAVIGATE_SONG_WITH_NUMBERS
            ),
            resetComposeUiState = {}
        ) { modifier ->
            ShortcutsEditor(modifier)
        }
    )
