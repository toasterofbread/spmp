package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.appaction.shortcut.Shortcut
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getShortcutCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_shortcut
import spmp.shared.generated.resources.s_cat_shortcut
import spmp.shared.generated.resources.s_key_configured_shortcuts
import spmp.shared.generated.resources.s_key_navigate_song_with_numbers

class ShortcutSettings(val context: AppContext): SettingsGroupImpl("SHORTCUT", context.getPrefs()) {
    val CONFIGURED_SHORTCUTS: PlatformSettingsProperty<List<Shortcut>?> by nullableSerialisableProperty(
        getName = { stringResource(Res.string.s_key_configured_shortcuts) },
        getDescription = { null },
        getDefaultValue = { null }
    )
    val NAVIGATE_SONG_WITH_NUMBERS: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_navigate_song_with_numbers) },
        getDescription = { null },
        getDefaultValue = { true }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_shortcut)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_shortcut)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Adjust

    override fun getConfigurationItems(): List<SettingsItem> = getShortcutCategoryItems(context)
}
