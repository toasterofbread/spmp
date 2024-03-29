package com.toasterofbread.spmp.platform

import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesImpl
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope
import spmp.shared.generated.resources.Res

actual class AppContext(
    app_name: String,
    val coroutine_scope: CoroutineScope
): PlatformContext(app_name, PlatformPlayerService::class.java) {
    override suspend fun getIconImageData(): ByteArray =
        Res.readBytes("drawable/ic_spmp.png")

    actual val database: Database = createDatabase()
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferencesImpl.getInstance { getFilesDir().resolve("preferences.json") }

    init {
        val prefs: PlatformPreferences = getPrefs()
        val youtubeapi_type: YtmApiType = YTApiSettings.Key.API_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, YTApiSettings.Key.API_URL.get(prefs))
    }
}
