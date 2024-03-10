package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLyricsCategoryItems

data object LyricsSettings: SettingsCategory("lyrics") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_lyrics"),
            getString("s_cat_desc_lyrics"),
            { getLyricsCategoryItems() },
            { Icons.Outlined.MusicNote }
        )

    enum class Key: SettingsKey {
        DEFAULT_SOURCE,
        FOLLOW_ENABLED,
        FOLLOW_OFFSET,
        DEFAULT_FURIGANA,
        TEXT_ALIGNMENT,
        EXTRA_PADDING,
        ENABLE_WORD_SYNC,
        FONT_SIZE,
        SYNC_DELAY,
        SYNC_DELAY_TOPBAR,
        SYNC_DELAY_BLUETOOTH;

        override val category: SettingsCategory get() = LyricsSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                FOLLOW_ENABLED -> true
                FOLLOW_OFFSET -> 0.25f
                DEFAULT_FURIGANA -> true
                TEXT_ALIGNMENT -> 0 // Left, center, right
                EXTRA_PADDING -> true
                ENABLE_WORD_SYNC -> false
                FONT_SIZE -> 0.5f
                DEFAULT_SOURCE -> 0
                SYNC_DELAY -> 0f
                SYNC_DELAY_TOPBAR -> -0.5f
                SYNC_DELAY_BLUETOOTH -> 0.3f
            } as T
    }
}
