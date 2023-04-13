import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.platform.ProjectContext
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.ApplicationTheme
import com.spectre7.spmp.ui.theme.Theme

expect fun getPlatformName(): String

private val prefs_change_listener =
    object : ProjectPreferences.Listener {
        override fun onChanged(prefs: ProjectPreferences, key: String) {
//            if (key == Settings.KEY_LANG_UI.name) {
//                updateLanguage(Settings.get(Settings.KEY_LANG_UI))
//            }
        }
    }

object SpMp {

    private lateinit var languages: Map<String, Map<String, String>>
    private lateinit var service_host: PlayerServiceHost
    private var service_started = false

    lateinit var context: ProjectContext
    val ui_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))
    val data_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_DATA))

    fun init(context: ProjectContext) {
        this.context = context

        languages = loadLanguages(context)
        Settings.prefs.addListener(prefs_change_listener)
    //    updateLanguage(Settings.get(Settings.KEY_LANG_UI))

        Cache.init(this)
        DataApi.initialise()
        MediaItem.init(Settings.prefs)

//        Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
//            error.printStackTrace()
//
//            context.startActivity(Intent(context, ErrorReportActivity::class.java).apply {
//                putExtra("message", error.message)
//                putExtra("stack_trace", error.stackTraceToString())
//            })
//        }

        service_host = PlayerServiceHost.instance ?: PlayerServiceHost()
        service_started = false
    }

    @Composable
    fun App(context: ProjectContext) {
        ApplicationTheme(context, getFontFamily(context)) {
            Theme.Update(context, MaterialTheme.colorScheme.primary)

            Surface(modifier = Modifier.fillMaxSize()) {
                if (PlayerServiceHost.service_connected) {
                    PlayerView()
                }
                else if (!service_started) {
                    service_started = true
                    service_host.startService({ service_started = false })
                }

//                MainActivity.error_manager.Indicator(Theme.current.accent_provider)
            }
        }
    }

    private fun loadLanguages(context: ProjectContext): MutableMap<String, Map<String, String>> {
        val data = context.openResourceFile("languages.json").bufferedReader()
        val ret = mutableMapOf<String, Map<String, String>>()
        for (item in Klaxon().parseJsonObject(data).entries) {
            val map = mutableMapOf<String, String>()
            for (subitem in (item.value as JsonObject).entries) {
                map[subitem.key] = subitem.value.toString()
            }
            ret[item.key] = map
        }
        data.close()
        return ret
    }

    private fun getFontFamily(context: ProjectContext): FontFamily {
        val locale = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))
        val font_dirs = context.listResourceFiles("")!!.filter { it.length > 4 && it.startsWith("font") }

        var font_dir: String? = font_dirs.firstOrNull { it.endsWith("-$locale") }
        if (font_dir == null) {
            val locale_split = locale.indexOf('-')
            if (locale_split > 0) {
                val sublocale = locale.take(locale_split)
                font_dir = font_dirs.firstOrNull { it.endsWith("-$sublocale") }
            }
        }

        val font_name = font_dir ?: "font"
        return FontFamily(context.loadFontFromFile("$font_name/regular.ttf"))
    }
}
