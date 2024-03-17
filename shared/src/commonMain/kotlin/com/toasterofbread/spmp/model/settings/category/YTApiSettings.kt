package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.youtubeapi.YtmApiType

data object YTApiSettings: SettingsCategory("ytapi") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? = null // TODO

    enum class Key: SettingsKey {
        API_TYPE,
        API_URL;

        override val category: SettingsCategory get() = YTApiSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                API_TYPE -> YtmApiType.DEFAULT.ordinal
                API_URL -> YtmApiType.DEFAULT.getDefaultUrl()
            } as T
    }
}
