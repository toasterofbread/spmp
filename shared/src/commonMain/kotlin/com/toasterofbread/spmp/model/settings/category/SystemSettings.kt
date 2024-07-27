package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSystemCategoryItems
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.Language
import dev.toastbits.composekit.platform.PlatformPreferences
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.*
import org.jetbrains.compose.resources.FontResource
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.s_key_interface_lang
import spmp.shared.generated.resources.s_sub_interface_lang
import spmp.shared.generated.resources.s_key_data_lang
import spmp.shared.generated.resources.s_sub_data_lang
import spmp.shared.generated.resources.s_key_font
import spmp.shared.generated.resources.s_key_ui_scale
import spmp.shared.generated.resources.s_key_library_path
import spmp.shared.generated.resources.s_sub_library_path
import spmp.shared.generated.resources.s_key_persistent_queue
import spmp.shared.generated.resources.s_sub_persistent_queue
import spmp.shared.generated.resources.s_key_add_songs_to_history
import spmp.shared.generated.resources.s_cat_general
import spmp.shared.generated.resources.s_cat_desc_general
import spmp.shared.generated.resources.font_option_system
import spmp.shared.generated.resources.font_option_hc_maru_gothic

class SystemSettings(
    val context: AppContext,
    private val available_languages: List<Language>
): SettingsGroup("SYSTEM", context.getPrefs()) {
    val LANG_UI: PreferencesProperty<String> by property(
        getName = { stringResource(Res.string.s_key_interface_lang) },
        getDescription = { stringResource(Res.string.s_sub_interface_lang) },
        getDefaultValue = { "" }
    )
    val LANG_DATA: PreferencesProperty<String> by property(
        getName = { stringResource(Res.string.s_key_data_lang) },
        getDescription = { stringResource(Res.string.s_sub_data_lang) },
        getDefaultValue = { "" }
    )
    val FONT: PreferencesProperty<FontMode> by enumProperty(
        getName = { stringResource(Res.string.s_key_font) },
        getDescription = { null },
        getDefaultValue = { FontMode.DEFAULT }
    )
    val UI_SCALE: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_ui_scale) },
        getDescription = { null },
        getDefaultValue = { 1f }
    )
    val LIBRARY_PATH: PreferencesProperty<String> by property(
        getName = { stringResource(Res.string.s_key_library_path) },
        getDescription = { stringResource(Res.string.s_sub_library_path) },
        getDefaultValue = { "" }
    )
    val PERSISTENT_QUEUE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_persistent_queue) },
        getDescription = { stringResource(Res.string.s_sub_persistent_queue) },
        getDefaultValue = { true }
    )
    val ADD_SONGS_TO_HISTORY: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_add_songs_to_history) },
        getDescription = { stringResource(Res.string.s_key_add_songs_to_history) },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_general) },
            { stringResource(Res.string.s_cat_desc_general) },
            { getSystemCategoryItems(context, available_languages) },
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

    @Composable
    fun getReadable(language: String): String =
        when (this) {
            DEFAULT -> {
                val default_font: String = getDefaultFont(language).getReadable(language)
                stringResource(Res.string.`font_option_default_$x`).replace("\$x", default_font)
            }
            SYSTEM -> stringResource(Res.string.font_option_system)
            HC_MARU_GOTHIC -> stringResource(Res.string.font_option_hc_maru_gothic)
        }

    companion object {
        fun getDefaultFont(language: String): FontMode =
            when (language) {
                "ja-JP" -> HC_MARU_GOTHIC
                else -> SYSTEM
            }
    }
}
