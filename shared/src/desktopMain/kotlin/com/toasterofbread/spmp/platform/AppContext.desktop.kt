package com.toasterofbread.spmp.platform

import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesImpl
import dev.toastbits.composekit.settings.ui.Theme
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
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
    actual val ytapi: YtmApi by lazy {
        settings.ytapi.API_TYPE.get().instantiate(this, settings.ytapi.API_URL.get())
    }
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }
    actual val settings: Settings by lazy { Settings(this) }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferencesImpl.getInstance { getFilesDir().resolve("preferences.json") }
}
