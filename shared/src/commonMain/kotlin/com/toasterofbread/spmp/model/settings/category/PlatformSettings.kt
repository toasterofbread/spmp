package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getPlatformCategoryItems
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_android
import spmp.shared.generated.resources.s_cat_desc_android
import spmp.shared.generated.resources.s_cat_desc_desktop
import spmp.shared.generated.resources.s_cat_desc_web
import spmp.shared.generated.resources.s_cat_desktop
import spmp.shared.generated.resources.s_cat_web
import spmp.shared.generated.resources.s_key_enable_external_server_mode
import spmp.shared.generated.resources.s_key_external_server_mode_ui_only
import spmp.shared.generated.resources.s_key_force_software_renderer
import spmp.shared.generated.resources.s_key_local_server_command
import spmp.shared.generated.resources.s_key_server_ip
import spmp.shared.generated.resources.s_key_server_local_start_automatically
import spmp.shared.generated.resources.s_key_server_port
import spmp.shared.generated.resources.s_key_startup_command
import spmp.shared.generated.resources.s_sub_enable_external_server_mode
import spmp.shared.generated.resources.s_sub_external_server_mode_ui_only
import spmp.shared.generated.resources.s_sub_force_software_renderer
import spmp.shared.generated.resources.s_sub_local_server_command
import spmp.shared.generated.resources.s_sub_server_local_start_automatically
import spmp.shared.generated.resources.s_sub_startup_command

class PlatformSettings(val context: AppContext): SettingsGroupImpl("DESKTOP", context.getPrefs()) {
    val STARTUP_COMMAND: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_startup_command) },
        getDescription = { stringResource(Res.string.s_sub_startup_command) },
        getDefaultValue = { "" }
    )
    val FORCE_SOFTWARE_RENDERER: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_force_software_renderer) },
        getDescription = { stringResource(Res.string.s_sub_force_software_renderer) },
        getDefaultValue = { false }
    )
    val SERVER_IP_ADDRESS: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_server_ip) },
        getDescription = { null },
        getDefaultValue = { "127.0.0.1" }
    )
    val SERVER_PORT: PlatformSettingsProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_server_port) },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.SERVER_PORT ?: 3973 }
    )
    val SERVER_LOCAL_COMMAND: PlatformSettingsProperty<String> by property(
        getName = { stringResource(Res.string.s_key_local_server_command) },
        getDescription = { stringResource(Res.string.s_sub_local_server_command) },
        getDefaultValue = { "" }
    )
    val SERVER_LOCAL_START_AUTOMATICALLY: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_server_local_start_automatically) },
        getDescription = { stringResource(Res.string.s_sub_server_local_start_automatically) },
        getDefaultValue = { true }
    )
    val ENABLE_EXTERNAL_SERVER_MODE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_external_server_mode) },
        getDescription = { stringResource(Res.string.s_sub_enable_external_server_mode) },
        getDefaultValue = { false }
    )
    val EXTERNAL_SERVER_MODE_UI_ONLY: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_external_server_mode_ui_only) },
        getDescription = { stringResource(Res.string.s_sub_external_server_mode_ui_only) },
        getDefaultValue = { false }
    )

    @Composable
    override fun getTitle(): String =
        when (Platform.current) {
            Platform.ANDROID -> stringResource(Res.string.s_cat_android)
            Platform.DESKTOP -> stringResource(Res.string.s_cat_desktop)
            Platform.WEB -> stringResource(Res.string.s_cat_web)
        }

    @Composable
    override fun getDescription(): String =
        when (Platform.current) {
            Platform.ANDROID -> stringResource(Res.string.s_cat_desc_android)
            Platform.DESKTOP -> stringResource(Res.string.s_cat_desc_desktop)
            Platform.WEB -> stringResource(Res.string.s_cat_desc_web)
        }

    @Composable
    override fun getIcon(): ImageVector =
        when (Platform.current) {
            Platform.ANDROID -> Icons.Outlined.Android
            Platform.DESKTOP -> Icons.Outlined.DesktopWindows
            Platform.WEB -> Icons.Outlined.Web
        }

    override fun getConfigurationItems(): List<SettingsItem> = getPlatformCategoryItems(context)
}
