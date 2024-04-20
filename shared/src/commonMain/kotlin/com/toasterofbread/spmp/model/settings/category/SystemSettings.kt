package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSystemCategoryItems
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.*
import org.jetbrains.compose.resources.FontResource
import dev.toastbits.composekit.platform.PreferencesProperty

class SystemSettings(val context: AppContext): SettingsGroup("SYSTEM", context.getPrefs()) {
    val LANG_UI: PreferencesProperty<String> by property(
        getName = { getString("s_key_interface_lang") },
        getDescription = { getString("s_sub_interface_lang") },
        getDefaultValue = { "" }
    )
    val LANG_DATA: PreferencesProperty<String> by property(
        getName = { getString("s_key_data_lang") },
        getDescription = { getString("s_sub_data_lang") },
        getDefaultValue = { "" }
    )
    val FONT: PreferencesProperty<FontMode> by enumProperty(
        getName = { getString("s_key_font") },
        getDescription = { null },
        getDefaultValue = { FontMode.DEFAULT }
    )
    val UI_SCALE: PreferencesProperty<Float> by property(
        getName = { getString("s_key_ui_scale") },
        getDescription = { null },
        getDefaultValue = { 1f }
    )
    val LIBRARY_PATH: PreferencesProperty<String> by property(
        getName = { getString("s_key_library_path") },
        getDescription = { getString("s_sub_library_path") },
        getDefaultValue = { "" }
    )
    val PERSISTENT_QUEUE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_persistent_queue") },
        getDescription = { getString("s_sub_persistent_queue") },
        getDefaultValue = { true }
    )
    val ADD_SONGS_TO_HISTORY: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_add_songs_to_history") },
        getDescription = { getString("s_key_add_songs_to_history") },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_general") },
            { getString("s_cat_desc_general") },
            { getSystemCategoryItems(context) },
            { Icons.Outlined.Tune }
        )
}

enum class FontMode {
    DEFAULT, SYSTEM, HC_MARU_GOTHIC;

    fun getFontResource(language: String): FontResource? =
        when (this) {
            DEFAULT -> getDefaultFont(language).getFontResource(language)
            SYSTEM -> null
            HC_MARU_GOTHIC -> Res.font.hc_maru_gothic
        }

    fun getReadable(language: String): String =
        when (this) {
            DEFAULT -> {
                val default_font: String = getDefaultFont(language).getReadable(language)
                getString("font_option_default_\$x").replace("\$x", default_font)
            }
            SYSTEM -> getString("font_option_system")
            HC_MARU_GOTHIC -> getString("font_option_hc_maru_gothic")
        }

    companion object {
        fun getDefaultFont(language: String): FontMode =
            when (language) {
                "ja-JP" -> HC_MARU_GOTHIC
                else -> SYSTEM
            }
    }
}
