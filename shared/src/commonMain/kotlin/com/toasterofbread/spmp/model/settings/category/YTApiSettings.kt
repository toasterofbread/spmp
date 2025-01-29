package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem

class YTApiSettings(prefs: PlatformSettings): SettingsGroupImpl("YTAPI", prefs) {
    val API_TYPE: PlatformSettingsProperty<YtmApiType> by enumProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { YtmApiType.DEFAULT }
    )
    val API_URL: PlatformSettingsProperty<String> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { YtmApiType.DEFAULT.getDefaultUrl() }
    )

    @Composable
    override fun getTitle(): String = ""

    @Composable
    override fun getDescription(): String = ""

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.PlayCircle

    override fun getConfigurationItems(): List<SettingsItem> = emptyList()

    override val hidden: Boolean = true
}
