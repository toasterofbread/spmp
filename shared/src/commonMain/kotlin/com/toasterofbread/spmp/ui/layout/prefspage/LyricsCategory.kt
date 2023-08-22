package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.composesettings.ui.item.SettingsDropdownItem
import com.toasterofbread.composesettings.ui.item.SettingsGroupItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsSliderItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource

internal fun getLyricsCategory(): List<SettingsItem> {
    return listOf(
        SettingsDropdownItem(
            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_SOURCE.name),
            getString("s_key_lyrics_default_source"), null,
            LyricsSource.SOURCE_AMOUNT
        ) { i ->
            LyricsSource.fromIdx(i).getReadable()
        },

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
            getString("s_key_lyrics_follow_enabled"), getString("s_sub_lyrics_follow_enabled")
        ),

        SettingsSliderItem(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
            getString("s_key_lyrics_follow_offset"), getString("s_sub_lyrics_follow_offset"),
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name),
            getString("s_key_lyrics_default_furigana"), null
        ),

        SettingsDropdownItem(
            SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name),
            getString("s_key_lyrics_text_alignment"), null, 3
        ) { i ->
            when (i) {
                0 -> getString("s_option_lyrics_text_alignment_left")
                1 -> getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_right")
            }
        },

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_EXTRA_PADDING.name),
            getString("s_key_lyrics_extra_padding"), getString("s_sub_lyrics_extra_padding")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_ENABLE_WORD_SYNC.name),
            getString("s_key_lyrics_enable_word_sync"), getString("s_sub_lyrics_enable_word_sync")
        ),

        SettingsSliderItem(
            SettingsValueState(Settings.KEY_LYRICS_FONT_SIZE.name),
            getString("s_key_lyrics_font_size"), null
        ),

        SettingsGroupItem(getString("s_group_top_bar_lyrics")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.name),
            getString("s_key_top_bar_lyrics_show_furigana"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_LIBRARY.name),
            getString("s_key_top_bar_lyrics_show_in_library"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_RADIOBUILDER.name),
            getString("s_key_top_bar_lyrics_show_in_radiobuilder"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_SETTINGS.name),
            getString("s_key_top_bar_lyrics_show_in_settings"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_LOGIN.name),
            getString("s_key_top_bar_lyrics_show_in_login"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_PLAYLIST.name),
            getString("s_key_top_bar_lyrics_show_in_playlist"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_ARTIST.name),
            getString("s_key_top_bar_lyrics_show_in_artist"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_VIEWMORE.name),
            getString("s_key_top_bar_lyrics_show_in_viewmore"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LYRICS_SHOW_IN_SEARCH.name),
            getString("s_key_top_bar_lyrics_show_in_search"), null
        )
    )
}
