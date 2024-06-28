package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLyricsCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty

class LyricsSettings(val context: AppContext): SettingsGroup("LYRICS", context.getPrefs()) {
    val FOLLOW_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_lyrics_follow_enabled") },
        getDescription = { getString("s_sub_lyrics_follow_enabled") },
        getDefaultValue = { true }
    )
    val FOLLOW_OFFSET: PreferencesProperty<Float> by property(
        getName = { getString("s_key_lyrics_follow_offset") },
        getDescription = { getString("s_sub_lyrics_follow_offset") },
        getDefaultValue = { 0.25f }
    )
    val ROMANISE_FURIGANA: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_lyrics_romanise_furigana") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val DEFAULT_FURIGANA: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_lyrics_default_furigana") },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val TEXT_ALIGNMENT: PreferencesProperty<Int> by property(
        getName = { getString("s_key_lyrics_text_alignment") },
        getDescription = { null },
        getDefaultValue = { 0 } // Left, center, right
    )
    val EXTRA_PADDING: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_lyrics_extra_padding") },
        getDescription = { getString("s_sub_lyrics_extra_padding") },
        getDefaultValue = { true }
    )
    val ENABLE_WORD_SYNC: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_lyrics_enable_word_sync") },
        getDescription = { getString("s_sub_lyrics_enable_word_sync") },
        getDefaultValue = { false }
    )
    val FONT_SIZE: PreferencesProperty<Float> by property(
        getName = { getString("s_key_lyrics_font_size") },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val DEFAULT_SOURCE: PreferencesProperty<Int> by property(
        getName = { getString("s_key_lyrics_default_source") },
        getDescription = { null },
        getDefaultValue = { 0 }
    )
    val SYNC_DELAY: PreferencesProperty<Float> by property(
        getName = { getString("s_key_lyrics_sync_delay") },
        getDescription = { getString("s_sub_lyrics_sync_delay") },
        getDefaultValue = { 0f }
    )
    val SYNC_DELAY_TOPBAR: PreferencesProperty<Float> by property(
        getName = { getString("s_key_lyrics_sync_delay_topbar") },
        getDescription = { getString("s_sub_lyrics_sync_delay_topbar") },
        getDefaultValue = { -0.5f }
    )
    val SYNC_DELAY_BLUETOOTH: PreferencesProperty<Float> by property(
        getName = { getString("s_key_lyrics_sync_delay_bluetooth") },
        getDescription = { getString("s_sub_lyrics_sync_delay_bluetooth") },
        getDefaultValue = { 0.3f }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_lyrics") },
            { getString("s_cat_desc_lyrics") },
            { getLyricsCategoryItems(context) },
            { Icons.Outlined.MusicNote }
        )
}
