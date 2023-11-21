package com.toasterofbread.spmp.model.settings.category

import com.google.gson.Gson
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.fromJson
import okhttp3.Headers.Companion.toHeaders

data object AuthSettings: SettingsCategory("auth") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? = null

    enum class Key: SettingsKey {
        YTM_AUTH,
        DISCORD_ACCOUNT_TOKEN;

        override val category: SettingsCategory get() = AuthSettings

        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
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
                DISCORD_ACCOUNT_TOKEN -> ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: ""
            } as T
    }
}
