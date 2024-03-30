package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.DropdownSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.LyricsSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource

internal fun getLyricsCategoryItems(): List<SettingsItem> {
    return listOf(
        DropdownSettingsItem(
            SettingsValueState(LyricsSettings.Key.DEFAULT_SOURCE.getName()),
            getString("s_key_lyrics_default_source"), null,
            LyricsSource.SOURCE_AMOUNT
        ) { i ->
            LyricsSource.fromIdx(i).getReadable()
        },

        ToggleSettingsItem(
            SettingsValueState(LyricsSettings.Key.FOLLOW_ENABLED.getName()),
            getString("s_key_lyrics_follow_enabled"), getString("s_sub_lyrics_follow_enabled")
        ),

        AppSliderItem(
            SettingsValueState(LyricsSettings.Key.FOLLOW_OFFSET.getName()),
            getString("s_key_lyrics_follow_offset"), getString("s_sub_lyrics_follow_offset"),
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        ToggleSettingsItem(
            SettingsValueState(LyricsSettings.Key.DEFAULT_FURIGANA.getName()),
            getString("s_key_lyrics_default_furigana"), null
        ),

        DropdownSettingsItem(
            SettingsValueState(LyricsSettings.Key.TEXT_ALIGNMENT.getName()),
            getString("s_key_lyrics_text_alignment"), null, 3
        ) { i ->
            when (i) {
                0 -> getString("s_option_lyrics_text_alignment_start")
                1 -> getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_end")
            }
        },

        ToggleSettingsItem(
            SettingsValueState(LyricsSettings.Key.EXTRA_PADDING.getName()),
            getString("s_key_lyrics_extra_padding"), getString("s_sub_lyrics_extra_padding")
        ),

        ToggleSettingsItem(
            SettingsValueState(LyricsSettings.Key.ENABLE_WORD_SYNC.getName()),
            getString("s_key_lyrics_enable_word_sync"), getString("s_sub_lyrics_enable_word_sync")
        ),

        AppSliderItem(
            SettingsValueState(LyricsSettings.Key.FONT_SIZE.getName()),
            getString("s_key_lyrics_font_size"), null
        ),

        AppSliderItem(
            SettingsValueState(LyricsSettings.Key.SYNC_DELAY.getName()),
            getString("s_key_lyrics_sync_delay"), getString("s_sub_lyrics_sync_delay"),
            range = -5f .. 5f
        ),

        AppSliderItem(
            SettingsValueState(LyricsSettings.Key.SYNC_DELAY_TOPBAR.getName()),
            getString("s_key_lyrics_sync_delay_topbar"), getString("s_sub_lyrics_sync_delay_topbar"),
            range = -5f .. 5f
        ),

        AppSliderItem(
            SettingsValueState(LyricsSettings.Key.SYNC_DELAY_BLUETOOTH.getName()),
            getString("s_key_lyrics_sync_delay_bluetooth"), getString("s_sub_lyrics_sync_delay_bluetooth"),
            range = -5f .. 5f
        )
    )
}
