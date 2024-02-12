package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerCategoryItems
import spms.socketapi.shared.SPMS_DEFAULT_PORT

data object ServerSettings: SettingsCategory("server") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_server"),
            getString("s_cat_desc_server"),
            { getServerCategoryItems() }
        ) { Icons.Outlined.DesktopWindows }

    enum class Key: SettingsKey {
        ENABLE_EXTERNAL_SERVER_MODE,
        EXTERNAL_SERVER_IP_ADDRESS,
        SERVER_PORT,
        KILL_CHILD_SERVER_ON_EXIT;

        override val category: SettingsCategory get() = ServerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                ENABLE_EXTERNAL_SERVER_MODE -> false
                EXTERNAL_SERVER_IP_ADDRESS -> "127.0.0.1"
                SERVER_PORT -> ProjectBuildConfig.SERVER_PORT ?: SPMS_DEFAULT_PORT
                KILL_CHILD_SERVER_ON_EXIT -> true
            } as T
    }
}
