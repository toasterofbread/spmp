package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem

class YTApiSettings(prefs: PlatformPreferences): SettingsGroup("YTAPI", prefs) {
    override fun getPage(): CategoryPage? = null

    val API_TYPE: PreferencesProperty<YtmApiType> by enumProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { YtmApiType.DEFAULT }
    )
    val API_URL: PreferencesProperty<String> by property(
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
