package com.toasterofbread.spmp.platform

import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesImpl
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

actual class AppContext(app_name: String): PlatformContext(app_name, PlatformPlayerService::class.java) {
    actual val database: Database = createDatabase()
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YoutubeApi
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferencesImpl.getInstance { getFilesDir().resolve("preferences.json") }

    init {
        val prefs = getPrefs()
        val youtubeapi_type: YoutubeApi.Type = YTApiSettings.Key.API_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, YTApiSettings.Key.API_URL.get(prefs))
    }

    suspend fun init(): AppContext {
        ytapi.init()
        return this
    }
}
