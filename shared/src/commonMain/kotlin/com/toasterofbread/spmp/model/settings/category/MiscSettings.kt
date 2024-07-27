package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getMiscCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_navbar_height_multiplier
import spmp.shared.generated.resources.s_sub_navbar_height_multiplier
import spmp.shared.generated.resources.s_key_status_webhook_url
import spmp.shared.generated.resources.s_sub_status_webhook_url
import spmp.shared.generated.resources.s_key_status_webhook_payload
import spmp.shared.generated.resources.s_sub_status_webhook_payload
import spmp.shared.generated.resources.s_key_enable_thumbnail_cache
import spmp.shared.generated.resources.s_cat_misc
import spmp.shared.generated.resources.s_cat_desc_misc

class MiscSettings(val context: AppContext): SettingsGroup("MISC", context.getPrefs()) {
    val NAVBAR_HEIGHT_MULTIPLIER: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_navbar_height_multiplier) },
        getDescription = { stringResource(Res.string.s_sub_navbar_height_multiplier) },
        getDefaultValue = { 1f }
    )
    val STATUS_WEBHOOK_URL: PreferencesProperty<String> by property(
        getName = { stringResource(Res.string.s_key_status_webhook_url) },
        getDescription = { stringResource(Res.string.s_sub_status_webhook_url) },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_URL ?: "" }
    )
    val STATUS_WEBHOOK_PAYLOAD: PreferencesProperty<String> by property(
        getName = { stringResource(Res.string.s_key_status_webhook_payload) },
        getDescription = { stringResource(Res.string.s_sub_status_webhook_payload) },
        getDefaultValue = { ProjectBuildConfig.STATUS_WEBHOOK_PAYLOAD ?: "{}" }
    )
    val THUMB_CACHE_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_thumbnail_cache) },
        getDescription = { null },
        getDefaultValue = { true }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_misc) },
            { stringResource(Res.string.s_cat_desc_misc) },
            { getMiscCategoryItems(context) },
            { Icons.Outlined.MoreHoriz }
        )
}
