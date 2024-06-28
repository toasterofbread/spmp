package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.platform.AppContext

internal fun getLyricsCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        DropdownSettingsItem(
            context.settings.lyrics.DEFAULT_SOURCE,
            LyricsSource.SOURCE_AMOUNT
        ) { i ->
            LyricsSource.fromIdx(i).getReadable()
        },

        ToggleSettingsItem(
            context.settings.lyrics.FOLLOW_ENABLED
        ),

        AppSliderItem(
            context.settings.lyrics.FOLLOW_OFFSET,
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        ToggleSettingsItem(
            context.settings.lyrics.ROMANISE_FURIGANA
        ),

        ToggleSettingsItem(
            context.settings.lyrics.DEFAULT_FURIGANA
        ),

        DropdownSettingsItem(
            context.settings.lyrics.TEXT_ALIGNMENT,
            3
        ) { i ->
            when (i) {
                0 -> getString("s_option_lyrics_text_alignment_start")
                1 -> getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_end")
            }
        },

        ToggleSettingsItem(
            context.settings.lyrics.EXTRA_PADDING
        ),

        ToggleSettingsItem(
            context.settings.lyrics.ENABLE_WORD_SYNC
        ),

        AppSliderItem(
            context.settings.lyrics.FONT_SIZE
        ),

        AppSliderItem(
            context.settings.lyrics.SYNC_DELAY,
            range = -5f .. 5f
        ),

        AppSliderItem(
            context.settings.lyrics.SYNC_DELAY_TOPBAR,
            range = -5f .. 5f
        ),

        AppSliderItem(
            context.settings.lyrics.SYNC_DELAY_BLUETOOTH,
            range = -5f .. 5f
        )
    )
}
