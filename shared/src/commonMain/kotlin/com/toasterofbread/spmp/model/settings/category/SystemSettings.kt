package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSystemCategoryItems
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.*
import org.jetbrains.compose.resources.FontResource

data object SystemSettings: SettingsCategory("system") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_general"),
            getString("s_cat_desc_general"),
            { getSystemCategoryItems(it) },
            { Icons.Outlined.Tune }
        )

    enum class Key: SettingsKey {
        LANG_UI,
        LANG_DATA,
        FONT,
        UI_SCALE,
        LIBRARY_PATH,
        PERSISTENT_QUEUE,
        ADD_SONGS_TO_HISTORY;

        override val category: SettingsCategory get() = SystemSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                LANG_UI, LANG_DATA -> ""
                FONT -> FontMode.DEFAULT.ordinal
                UI_SCALE -> 1f
                LIBRARY_PATH -> ""
                PERSISTENT_QUEUE -> true
                ADD_SONGS_TO_HISTORY -> false
            } as T
    }
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
