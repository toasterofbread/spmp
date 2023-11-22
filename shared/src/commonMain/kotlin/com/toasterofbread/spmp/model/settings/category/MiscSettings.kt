package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getMiscCategoryItems

data object MiscSettings: SettingsCategory("misc") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_misc"),
            getString("s_cat_desc_misc"),
            { getMiscCategoryItems() }
        ) { Icons.Outlined.MoreHoriz }

    enum class Key: SettingsKey {
        NAVBAR_HEIGHT_MULTIPLIER,
        STATUS_WEBHOOK_URL,
        STATUS_WEBHOOK_PAYLOAD,
        THUMB_CACHE_ENABLED; // TODO Max size, management

        override val category: SettingsCategory get() = MiscSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                NAVBAR_HEIGHT_MULTIPLIER -> 1f
                STATUS_WEBHOOK_URL -> ProjectBuildConfig.STATUS_WEBHOOK_URL ?: ""
                STATUS_WEBHOOK_PAYLOAD -> ProjectBuildConfig.STATUS_WEBHOOK_PAYLOAD ?: "{}"
                THUMB_CACHE_ENABLED -> true
            } as T
    }
}
