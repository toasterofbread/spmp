package com.spectre7.spmp.ui.layout.prefspage

import com.spectre7.settings.model.*
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getString

internal fun getLyricsCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
            getString("s_key_lyrics_follow_enabled"), getString("s_sub_lyrics_follow_enabled")
        ),

        SettingsItemSlider(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
            getString("s_key_lyrics_follow_offset"), getString("s_sub_lyrics_follow_offset"),
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name),
            getString("s_key_lyrics_default_furigana"), null
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name),
            getString("s_key_lyrics_text_alignment"), null, 3
        ) { i ->
            when (i) {
                0 -> getString("s_option_lyrics_text_alignment_left")
                1 -> getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_right")
            }
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_EXTRA_PADDING.name),
            getString("s_key_lyrics_extra_padding"), getString("s_sub_lyrics_extra_padding")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_ENABLE_WORD_SYNC.name),
            getString("s_key_lyrics_enable_word_sync"), getString("s_sub_lyrics_enable_word_sync")
        ),

        SettingsItemSlider(
            SettingsValueState(Settings.KEY_LYRICS_FONT_SIZE.name),
            getString("s_key_lyrics_font_size"), null
        ),

        SettingsGroup(getString("s_group_top_bar_lyrics")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.name),
            getString("s_key_top_bar_lyrics_show_furigana"), null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_LIBRARY.name),
            getString("s_key_top_bar_lyrics_show_in_library"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_RADIOBUILDER.name),
            getString("s_key_top_bar_lyrics_show_in_radiobuilder"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_SETTINGS.name),
            getString("s_key_top_bar_lyrics_show_in_settings"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_LOGIN.name),
            getString("s_key_top_bar_lyrics_show_in_login"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_PLAYLIST.name),
            getString("s_key_top_bar_lyrics_show_in_playlist"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_ARTIST.name),
            getString("s_key_top_bar_lyrics_show_in_artist"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_VIEWMORE.name),
            getString("s_key_top_bar_lyrics_show_in_viewmore"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_SEARCH.name),
            getString("s_key_top_bar_lyrics_show_in_search"), null
        )
    )
}
