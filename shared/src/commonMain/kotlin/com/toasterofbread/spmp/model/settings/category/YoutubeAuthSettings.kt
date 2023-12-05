package com.toasterofbread.spmp.model.settings.category

import com.google.gson.Gson
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getYtmAuthItem
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.fromJson
import okhttp3.Headers.Companion.toHeaders

data object YoutubeAuthSettings: SettingsCategory("ytauth") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? =
        object : Page(
            this,
            getString("s_cat_youtube_auth")
        ) {
            override fun getTitleItem(context: AppContext): SettingsItem? =
                getYtmAuthItem(
                    context,
                    SettingsValueState<Set<String>>(Key.YTM_AUTH.getName()).init(context.getPrefs(), Settings::provideDefault),
                    true
                )
        }

    enum class Key: SettingsKey {
        YTM_AUTH;

        override val category: SettingsCategory get() = YoutubeAuthSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                YTM_AUTH -> {
                    with(ProjectBuildConfig) {
                        if (YTM_CHANNEL_ID != null && YTM_HEADERS != null)
                            YoutubeApi.UserAuthState.packSetData(
                                ArtistRef(YTM_CHANNEL_ID),
                                Gson().fromJson<Map<String, String>>(YTM_HEADERS.reader()).toHeaders()
                            )
                        else emptySet()
                    }
                }
            } as T
    }
}
