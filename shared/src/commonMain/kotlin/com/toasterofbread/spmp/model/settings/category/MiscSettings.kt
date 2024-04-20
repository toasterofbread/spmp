package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getMiscCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty

class MiscSettings(val context: AppContext): SettingsGroup("MISC", context.getPrefs()) {
    val NAVBAR_HEIGHT_MULTIPLIER: PreferencesProperty<Float> by property(
        getName = { getString("s_key_navbar_height_multiplier") },
        getDescription = { getString("s_sub_navbar_height_multiplier") },
        getDefaultValue = { 1f }
    )
    val STATUS_WEBHOOK_URL: PreferencesProperty<String> by property(
        getName = { getString("s_key_status_webhook_url") },
        getDescription = { getString("s_sub_status_webhook_url") },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_URL ?: "" }
    )
    val STATUS_WEBHOOK_PAYLOAD: PreferencesProperty<String> by property(
        getName = { getString("s_key_status_webhook_payload") },
        getDescription = { getString("s_sub_status_webhook_payload") },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_PAYLOAD ?: "{}" }
    )
    val THUMB_CACHE_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_enable_thumbnail_cache") },
        getDescription = { null },
        getDefaultValue = { true }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_misc") },
            { getString("s_cat_desc_misc") },
            { getMiscCategoryItems(context) },
            { Icons.Outlined.MoreHoriz }
        )
}
