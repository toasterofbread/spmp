package com.toasterofbread.spmp.platform

import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

actual class AppContext(app_name: String): PlatformContext(app_name, PlatformPlayerService::class.java) {
    actual val database: Database = createDatabase()
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YoutubeApi
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferences.getInstance { getFilesDir().resolve("preferences.json") }

    init {
        val prefs = getPrefs()
        val youtubeapi_type: YoutubeApi.Type = YTApiSettings.Key.YOUTUBEAPI_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, YTApiSettings.Key.YOUTUBEAPI_URL.get(prefs))
    }

    suspend fun init(): AppContext {
        ytapi.init()
        return this
    }
}
