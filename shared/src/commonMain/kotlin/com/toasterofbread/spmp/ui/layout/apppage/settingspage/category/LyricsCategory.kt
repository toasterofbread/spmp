package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.util.toCustomResource
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
            context.settings.Lyrics.DEFAULT_SOURCE,
            LyricsSource.SOURCE_AMOUNT
        ) { i ->
            LyricsSource.fromIdx(i).getReadable()
        },

        ToggleSettingsItem(
            context.settings.Lyrics.FOLLOW_ENABLED
        ),

        AppSliderItem(
            context.settings.Lyrics.FOLLOW_OFFSET,
            Res.string.s_option_lyrics_follow_offset_top.toCustomResource(),
            Res.string.s_option_lyrics_follow_offset_bottom.toCustomResource(),
            steps = 5,
            getValueText = null
        ),

        ToggleSettingsItem(
            context.settings.Lyrics.ROMANISE_FURIGANA
        ),

        ToggleSettingsItem(
            context.settings.Lyrics.DEFAULT_FURIGANA
        ),

        DropdownSettingsItem(
            context.settings.Lyrics.TEXT_ALIGNMENT,
            3
        ) { i ->
            when (i) {
                0 -> stringResource(Res.string.s_option_lyrics_text_alignment_start)
                1 -> stringResource(Res.string.s_option_lyrics_text_alignment_center)
                else -> stringResource(Res.string.s_option_lyrics_text_alignment_end)
            }
        },

        ToggleSettingsItem(
            context.settings.Lyrics.EXTRA_PADDING
        ),

        ToggleSettingsItem(
            context.settings.Lyrics.ENABLE_WORD_SYNC
        ),

        AppSliderItem(
            context.settings.Lyrics.FONT_SIZE
        ),

        AppSliderItem(
            context.settings.Lyrics.SYNC_DELAY,
            range = -5f .. 5f
        ),

        AppSliderItem(
            context.settings.Lyrics.SYNC_DELAY_TOPBAR,
            range = -5f .. 5f
        ),

        AppSliderItem(
            context.settings.Lyrics.SYNC_DELAY_BLUETOOTH,
            range = -5f .. 5f
        )
    )
}
