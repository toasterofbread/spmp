package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.youtubeapi.YtmApiType
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty

class YTApiSettings(val context: AppContext): SettingsGroup("YTAPI", context.getPrefs()) {
    override val page: CategoryPage? = null // TODO

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
}
