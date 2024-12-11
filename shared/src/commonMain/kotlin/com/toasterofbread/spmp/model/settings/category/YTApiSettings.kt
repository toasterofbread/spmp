package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem

class YTApiSettings(prefs: PlatformSettings): SettingsGroupImpl("YTAPI", prefs) {
    override fun getPage(): CategoryPage? = null

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
}
