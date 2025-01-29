package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getMiscCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_misc
import spmp.shared.generated.resources.s_cat_misc
import spmp.shared.generated.resources.s_key_add_songs_to_history
import spmp.shared.generated.resources.s_key_enable_thumbnail_cache
import spmp.shared.generated.resources.s_key_library_path
import spmp.shared.generated.resources.s_key_navbar_height_multiplier
import spmp.shared.generated.resources.s_key_persistent_queue
import spmp.shared.generated.resources.s_key_status_webhook_payload
import spmp.shared.generated.resources.s_key_status_webhook_url
import spmp.shared.generated.resources.s_sub_library_path
import spmp.shared.generated.resources.s_sub_navbar_height_multiplier
import spmp.shared.generated.resources.s_sub_persistent_queue
import spmp.shared.generated.resources.s_sub_status_webhook_payload
import spmp.shared.generated.resources.s_sub_status_webhook_url

class MiscSettings(val context: AppContext): SettingsGroupImpl("MISC", context.getPrefs()) {
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
    val NAVBAR_HEIGHT_MULTIPLIER: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_navbar_height_multiplier) },
        getDescription = { stringResource(Res.string.s_sub_navbar_height_multiplier) },
        getDefaultValue = { 1f }
    )
    val STATUS_WEBHOOK_URL: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_status_webhook_url) },
        getDescription = { stringResource(Res.string.s_sub_status_webhook_url) },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_URL ?: "" }
    )
    val STATUS_WEBHOOK_PAYLOAD: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_status_webhook_payload) },
        getDescription = { stringResource(Res.string.s_sub_status_webhook_payload) },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_PAYLOAD ?: "{}" }
    )
    val THUMB_CACHE_ENABLED: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_thumbnail_cache) },
        getDescription = { null },
        getDefaultValue = { true }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_misc)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_misc)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.MoreHoriz

    override fun getConfigurationItems(): List<SettingsItem> = getMiscCategoryItems(context)
}
