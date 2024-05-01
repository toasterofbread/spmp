package com.toasterofbread.spmp.platform

import android.app.Activity
import android.content.Context
import dev.toastbits.composekit.platform.ApplicationContext
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesImpl
import dev.toastbits.composekit.settings.ui.Theme
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope

actual class AppContext(
    context: Context,
    coroutine_scope: CoroutineScope,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        lateinit var main_activity: Class<out Activity>
    }

    actual val database: Database = createDatabase()
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi by lazy {
        settings.ytapi.API_TYPE.get().instantiate(this, settings.ytapi.API_URL.get())
    }
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }
    actual val settings: Settings by lazy { Settings(this) }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferencesImpl.getInstance(ctx)
}
