package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.remember
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.serialise
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.UiString

internal fun getFeedCategoryItems(): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            SettingsValueState(FeedSettings.Key.SHOW_ARTISTS_ROW.getName()),
            getString("s_key_feed_show_artists_row"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(FeedSettings.Key.SHOW_SONG_DOWNLOAD_INDICATORS.getName()),
            getString("s_key_feed_show_song_download_indicators"), null
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.INITIAL_ROWS.getName()),
            getString("s_key_feed_initial_rows"),
            getString("s_sub_feed_initial_rows"),
            "1",
            "10",
            range = 1f..10f,
            steps = 10
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.SQUARE_PREVIEW_TEXT_LINES.getName()),
            getString("s_key_feed_square_preview_text_lines"),
            null,
            "1",
            "5",
            range = 1f..5f
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.GRID_ROW_COUNT.getName()),
            getString("s_key_feed_grid_row_count"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.GRID_ROW_COUNT_EXPANDED.getName()),
            getString("s_key_feed_grid_row_count_expanded"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT.getName()),
            getString("s_key_feed_alt_grid_row_count"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            SettingsValueState<Int>(FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT_EXPANDED.getName()),
            getString("s_key_feed_alt_grid_row_count_expanded"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        ToggleSettingsItem(
            SettingsValueState(FeedSettings.Key.SHOW_RADIOS.getName()),
            getString("s_key_feed_show_radios"), null
        ),

        AppStringSetItem(
            SettingsValueState(FeedSettings.Key.HIDDEN_ROWS.getName()),
            getString("s_key_hidden_feed_rows"), getString("s_sub_hidden_feed_rows"),
            getString("s_hidden_feed_rows_dialog_title"),
            itemToText = {
                val player = LocalPlayerState.current
                remember(it) {
                    UiString.deserialise(it).getString(player.context)
                }
            },
            textToItem = {
                RawUiString(it).serialise()
            }
        )
    )
}
