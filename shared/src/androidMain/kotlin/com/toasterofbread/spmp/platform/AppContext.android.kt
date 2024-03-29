package com.toasterofbread.spmp.platform

import android.app.Activity
import android.content.Context
import com.toasterofbread.composekit.platform.ApplicationContext
import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesImpl
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual class AppContext(
    context: Context,
    val coroutine_scope: CoroutineScope,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        lateinit var main_activity: Class<out Activity>
    }

    actual val database: Database = createDatabase()
    actual val download_manager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }

    init {
        val prefs = getPrefs()
        val youtubeapi_type: YtmApiType = YTApiSettings.Key.API_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, YTApiSettings.Key.API_URL.get(prefs))
    }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferencesImpl.getInstance(ctx)
}
