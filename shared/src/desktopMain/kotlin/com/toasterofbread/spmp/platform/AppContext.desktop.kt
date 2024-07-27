package com.toasterofbread.spmp.platform

import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesImpl
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getAvailableLanguages
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.composekit.platform.getDesktopFilesDir
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.app_name

actual class AppContext private constructor(
    app_name: String,
    coroutine_scope: CoroutineScope,
    prefs: PlatformPreferences,
    api_type: YtmApiType,
    api_url: String,
    data_language: Language,
    available_languages: List<Language>
): PlatformContext(app_name, PlayerService::class.java, coroutine_scope) {
    companion object {
        suspend fun create(coroutine_scope: CoroutineScope): AppContext {
            val app_name: String = getString(Res.string.app_name)
            val prefs: PlatformPreferences = PlatformPreferencesImpl.getInstance { getDesktopFilesDir(app_name).resolve("preferences.json") }
            val settings: YTApiSettings = YTApiSettings(prefs)

            return AppContext(
                app_name,
                coroutine_scope,
                prefs,
                settings.API_TYPE.get(),
                settings.API_URL.get(),
                Language.getSystem(),
                getAvailableLanguages()
            )
        }
    }

    override suspend fun getIconImageData(): ByteArray? =
        Res.readBytes("drawable/ic_spmp.png")

    private val _prefs: PlatformPreferences = prefs
    actual fun getPrefs(): PlatformPreferences = _prefs

    actual val database: Database = createDatabase()
    actual val settings: Settings = Settings(this, available_languages)
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi = api_type.instantiate(this, api_url, data_language)
    actual val theme: AppThemeManager = AppThemeManager(this@AppContext)
}
