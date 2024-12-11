package com.toasterofbread.spmp.platform

import SpMp
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getAvailableLanguages
import com.toasterofbread.spmp.youtubeapi.YtmApiType
import dev.toastbits.composekit.context.ApplicationContext
import dev.toastbits.composekit.context.PlatformContext
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.context.PlatformPreferencesImpl
import dev.toastbits.composekit.util.getThemeColour
import dev.toastbits.composekit.util.launchSingle
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

actual class AppContext private constructor(
    context: Context,
    coroutine_scope: CoroutineScope,
    api_type: YtmApiType,
    api_url: String,
    data_language: Language,
    available_languages: List<Language>,
    private val prefs: PlatformSettings,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        suspend fun create(
            context: Context,
            coroutine_scope: CoroutineScope,
            application_context: ApplicationContext? = null
        ): AppContext {
            val prefs: PlatformSettings = PlatformPreferencesImpl.getInstance(context, ProjectJson.instance)
            val settings: YTApiSettings = YTApiSettings(prefs)

            return AppContext(
                context,
                coroutine_scope,
                settings.API_TYPE.get(),
                settings.API_URL.get(),
                Language.getSystem(),
                getAvailableLanguages(),
                prefs,
                application_context
            )
        }

        fun getMainActivityIntent(context: Context): Intent =
            Intent().setComponent(ComponentName(context, "com.toasterofbread.spmp.MainActivity"))
    }

    actual fun getPrefs(): PlatformSettings = prefs

    private val colorblendr_coroutine_scope = CoroutineScope(Dispatchers.Default)
    private var current_colorblendr_song: Song? = null

    fun onNotificationThumbnailLoaded(image: Bitmap?) {
        colorblendr_coroutine_scope.launchSingle {
            if (!settings.experimental.ANDROID_MONET_COLOUR_ENABLE.get()) {
                return@launchSingle
            }

            delay(500)

            val song: Song? =
                withContext(Dispatchers.Main) {
                    SpMp._player_state?.status?.song
                }

            if (current_colorblendr_song == song) {
                return@launchSingle
            }
            current_colorblendr_song = song

            val colour: Color? = image?.asImageBitmap()?.getThemeColour()

            val action: String =
                if (colour != null) "com.drdisagree.colorblendr.SET_PRIMARY_COLOR"
                else "com.drdisagree.colorblendr.RESET_PRIMARY_COLOR"

            val intent: Intent =
                Intent(action).setPackage("com.drdisagree.colorblendr")

            intent.putExtra("owner", "com.toasterofbread.spmp")

            if (colour != null) {
                intent.putExtra("set_color", colour.toArgb())
            }

            ctx.startService(intent)
        }
    }

    actual val database: Database = createDatabase()
    actual val settings: Settings = Settings(this, available_languages)
    actual val download_manager: PlayerDownloadManager = PlayerDownloadManager(this)
    actual val ytapi: YtmApi = api_type.instantiate(this, api_url, data_language)
    actual val theme: AppThemeManager = AppThemeManager(this@AppContext)
}
