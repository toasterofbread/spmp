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
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getAvailableLanguages
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope

actual class AppContext private constructor(
    context: Context,
    coroutine_scope: CoroutineScope,
    api_type: YtmApiType,
    api_url: String,
    available_languages: List<Language>,
    private val prefs: PlatformPreferences,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        lateinit var main_activity: Class<out Activity>

        suspend fun create(
            context: Context,
            coroutine_scope: CoroutineScope,
            application_context: ApplicationContext? = null
        ): AppContext {
            val prefs: PlatformPreferences = PlatformPreferencesImpl.getInstance(context)
            val settings: YTApiSettings = YTApiSettings(prefs)

            return AppContext(
                context,
                coroutine_scope,
                settings.API_TYPE.get(),
                settings.API_URL.get(),
                getAvailableLanguages(),
                prefs,
                application_context
            )
        }
    }

    actual val database: Database = createDatabase()
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi = api_type.instantiate(this, api_url)
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }
    actual val settings: Settings by lazy { Settings(this, available_languages) }

    actual fun getPrefs(): PlatformPreferences = prefs
}
