package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

data object YTApiSettings: SettingsCategory("ytapi") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? = null // TODO

    enum class Key: SettingsKey {
        API_TYPE,
        API_URL;

        override val category: SettingsCategory get() = YTApiSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                API_TYPE -> YoutubeApi.Type.DEFAULT.ordinal
                API_URL -> YoutubeApi.Type.DEFAULT.getDefaultUrl()
            } as T
    }
}
