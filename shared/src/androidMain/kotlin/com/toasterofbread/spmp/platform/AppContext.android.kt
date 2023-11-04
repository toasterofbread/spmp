package com.toasterofbread.spmp.platform

import android.app.Activity
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.toasterofbread.db.Database
import com.toasterofbread.toastercomposetools.platform.ApplicationContext
import com.toasterofbread.toastercomposetools.platform.PlatformContext
import com.toasterofbread.toastercomposetools.platform.PlatformPreferences
import com.toasterofbread.toastercomposetools.settings.ui.StaticThemeData
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.settings.ui.ThemeData
import com.toasterofbread.spmp.model.AccentColourSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual class AppContext(
    context: Context,
    private val coroutine_scope: CoroutineScope,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        lateinit var main_activity: Class<out Activity>
    }

    actual val database: Database = createDatabase()
    actual val download_manager = PlayerDownloadManager(this)
    actual val ytapi: YoutubeApi
    actual val theme: Theme by lazy { ThemeImpl(this@AppContext) }

    init {
        val prefs = getPrefs()
        val youtubeapi_type: YoutubeApi.Type = Settings.KEY_YOUTUBEAPI_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, Settings.KEY_YOUTUBEAPI_URL.get(prefs))
    }

    fun init(): AppContext {
        coroutine_scope.launch {
            ytapi.init()
        }
        return this
    }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferences.getInstance(ctx)
}
