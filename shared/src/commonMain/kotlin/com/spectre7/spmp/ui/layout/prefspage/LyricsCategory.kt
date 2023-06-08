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

        SettingsGroup(getString("s_group_top_bar_lyrics")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.name),
            getString("s_key_top_bar_lyrics_show_furigana"), null
        )
    )
}
