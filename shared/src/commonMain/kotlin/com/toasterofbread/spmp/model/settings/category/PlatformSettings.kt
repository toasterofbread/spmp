package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Android
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getPlatformCategoryItems
import com.toasterofbread.spmp.platform.AppContext

class PlatformSettings(val context: AppContext): SettingsGroup("DESKTOP", context.getPrefs()) {
    val STARTUP_COMMAND: PreferencesProperty<String> by property(
        getName = { getString("s_key_startup_command") },
        getDescription = { getString("s_sub_startup_command") },
        getDefaultValue = { "" }
    )
    val FORCE_SOFTWARE_RENDERER: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_force_software_renderer") },
        getDescription = { getString("s_sub_force_software_renderer") },
        getDefaultValue = { false }
    )
    val SERVER_IP_ADDRESS: PreferencesProperty<String> by property(
        getName = { getString("s_key_server_ip") },
        getDescription = { null },
        getDefaultValue = { "127.0.0.1" }
    )
    val SERVER_PORT: PreferencesProperty<Int> by property(
        getName = { getString("s_key_server_port") },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.SERVER_PORT ?: 3973 }
    )
    val SERVER_LOCAL_COMMAND: PreferencesProperty<String> by property(
        getName = { getString("s_key_local_server_command") },
        getDescription = { getString("s_sub_local_server_command") },
        getDefaultValue = { "" }
    )
    val SERVER_LOCAL_START_AUTOMATICALLY: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_server_local_start_automatically") },
        getDescription = { getString("s_sub_server_local_start_automatically") },
        getDefaultValue = { true }
    )
    val SERVER_KILL_CHILD_ON_EXIT: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_server_kill_child_on_exit") },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val ENABLE_EXTERNAL_SERVER_MODE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_enable_external_server_mode") },
        getDescription = { getString("s_sub_enable_external_server_mode") },
        getDefaultValue = { false }
    )
    val EXTERNAL_SERVER_MODE_UI_ONLY: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_external_server_mode_ui_only") },
        getDescription = { getString("s_sub_external_server_mode_ui_only") },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        SimplePage(
            {
                when (Platform.current) {
                    Platform.ANDROID -> getString("s_cat_android")
                    Platform.DESKTOP -> getString("s_cat_desktop")
                }
            },
            {
                when (Platform.current) {
                    Platform.ANDROID -> getString("s_cat_desc_android")
                    Platform.DESKTOP -> getString("s_cat_desc_desktop")
                }
            },
            { getPlatformCategoryItems(context) },
            {
                when (Platform.current) {
                    Platform.ANDROID -> Icons.Outlined.Android
                    Platform.DESKTOP -> Icons.Outlined.DesktopWindows
                }
            }
        )
}
