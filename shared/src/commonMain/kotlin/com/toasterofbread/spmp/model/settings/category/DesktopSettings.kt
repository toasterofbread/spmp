package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getDesktopCategoryItems

data object DesktopSettings: SettingsCategory("desktop") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        if (Platform.DESKTOP.isCurrent())
            SimplePage(
                getString("s_cat_desktop"),
                getString("s_cat_desc_desktop"),
                { getDesktopCategoryItems() },
                { Icons.Outlined.DesktopWindows }
            )
        else null

    enum class Key: SettingsKey {
        STARTUP_COMMAND,
        FORCE_SOFTWARE_RENDERER,
        SERVER_IP_ADDRESS,
        SERVER_PORT,
        SERVER_LOCAL_COMMAND,
        SERVER_LOCAL_START_AUTOMATICALLY,
        SERVER_KILL_CHILD_ON_EXIT;

        override val category: SettingsCategory get() = DesktopSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                STARTUP_COMMAND -> ""
                FORCE_SOFTWARE_RENDERER -> false
                SERVER_IP_ADDRESS -> "127.0.0.1"
                SERVER_PORT -> ProjectBuildConfig.SERVER_PORT ?: 3973
                SERVER_LOCAL_COMMAND -> ""
                SERVER_LOCAL_START_AUTOMATICALLY -> true
                SERVER_KILL_CHILD_ON_EXIT -> true
            } as T
    }
}
