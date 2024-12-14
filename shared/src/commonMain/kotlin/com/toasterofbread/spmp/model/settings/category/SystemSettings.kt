package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSystemCategoryItems
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_general
import spmp.shared.generated.resources.s_cat_general
import spmp.shared.generated.resources.s_key_add_songs_to_history
import spmp.shared.generated.resources.s_key_library_path
import spmp.shared.generated.resources.s_key_persistent_queue
import spmp.shared.generated.resources.s_sub_library_path
import spmp.shared.generated.resources.s_sub_persistent_queue

class SystemSettings(
    val context: AppContext,
    private val available_languages: List<Language>
): SettingsGroupImpl("SYSTEM", context.getPrefs()) {
    val LIBRARY_PATH: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_library_path) },
        getDescription = { stringResource(Res.string.s_sub_library_path) },
        getDefaultValue = { "" }
    )
    val PERSISTENT_QUEUE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_persistent_queue) },
        getDescription = { stringResource(Res.string.s_sub_persistent_queue) },
        getDefaultValue = { true }
    )
    val ADD_SONGS_TO_HISTORY: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_add_songs_to_history) },
        getDescription = { stringResource(Res.string.s_key_add_songs_to_history) },
        getDefaultValue = { false }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_general)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_general)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Tune

    override fun getConfigurationItems(): List<SettingsItem> = getSystemCategoryItems(context, available_languages)
}
