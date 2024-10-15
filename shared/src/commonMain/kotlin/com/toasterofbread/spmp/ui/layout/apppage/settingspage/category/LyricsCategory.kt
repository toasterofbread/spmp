package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.utils.common.toCustomResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_option_lyrics_follow_offset_top
import spmp.shared.generated.resources.s_option_lyrics_follow_offset_bottom
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_start
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_center
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_end

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
            Res.string.s_option_lyrics_follow_offset_top.toCustomResource(),
            Res.string.s_option_lyrics_follow_offset_bottom.toCustomResource(),
            steps = 5,
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
                0 -> stringResource(Res.string.s_option_lyrics_text_alignment_start)
                1 -> stringResource(Res.string.s_option_lyrics_text_alignment_center)
                else -> stringResource(Res.string.s_option_lyrics_text_alignment_end)
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
