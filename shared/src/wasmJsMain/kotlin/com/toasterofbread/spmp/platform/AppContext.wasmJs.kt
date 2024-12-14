package com.toasterofbread.spmp.platform

import dev.toastbits.composekit.context.PlatformContext
import dev.toastbits.composekit.settings.PlatformSettings
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getAvailableLanguages
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.composekit.context.InMemoryPlatformPreferences
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope

actual class AppContext private constructor(
    coroutine_scope: CoroutineScope,
    prefs: PlatformSettings,
    api_type: YtmApiType,
    api_url: String,
    data_language: Language,
    available_languages: List<Language>
): PlatformContext(coroutine_scope) {
    companion object {
        suspend fun create(coroutine_scope: CoroutineScope): AppContext {
            val prefs: PlatformSettings = InMemoryPlatformPreferences()
            val settings: YTApiSettings = YTApiSettings(prefs)

            return AppContext(
                coroutine_scope,
                prefs,
                settings.API_TYPE.get(),
                settings.API_URL.get(),
                Language.getSystem(),
                getAvailableLanguages()
            )
        }
    }

    private val _prefs: PlatformSettings = prefs
    actual fun getPrefs(): PlatformSettings = _prefs

    actual val database: Database = createDatabase()
    actual val settings: Settings = Settings(this, available_languages)
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi = api_type.instantiate(this, api_url, data_language)
    actual val theme: AppThemeManager = AppThemeManager(this@AppContext)
}
