package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.runtime.remember
import com.toasterofbread.composesettings.ui.item.SettingsGroupItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsSliderItem
import com.toasterofbread.composesettings.ui.item.SettingsStringSetItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.RawLocalisedString

internal fun getFeedCategory(): List<SettingsItem> {
    return listOf(
        SettingsGroupItem(getString("s_group_rec_feed")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_FILTER_BAR.name),
            getString("s_key_feed_show_filter_bar"), getString("s_sub_feed_show_filter_bar")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_SONG_DOWNLOAD_INDICATORS.name),
            getString("s_key_feed_show_song_download_indicators"), null
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_INITIAL_ROWS.name),
            getString("s_key_feed_initial_rows"),
            getString("s_sub_feed_initial_rows"),
            "1",
            "10",
            range = 1f..10f,
            steps = 10
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_SQUARE_PREVIEW_TEXT_LINES.name),
            getString("s_key_feed_square_preview_text_lines"),
            null,
            "1",
            "5",
            range = 1f..5f
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_GRID_ROW_COUNT.name),
            getString("s_key_feed_grid_row_count"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_GRID_ROW_COUNT_EXPANDED.name),
            getString("s_key_feed_grid_row_count_expanded"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_ALT_GRID_ROW_COUNT.name),
            getString("s_key_feed_alt_grid_row_count"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_ALT_GRID_ROW_COUNT_EXPANDED.name),
            getString("s_key_feed_alt_grid_row_count_expanded"),
            null,
            "1",
            "10",
            range = 1f..10f
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_RADIOS.name),
            getString("s_key_feed_show_radios"), null
        ),

        SettingsStringSetItem(
            SettingsValueState(Settings.KEY_FEED_HIDDEN_ROWS.name),
            getString("s_key_hidden_feed_rows"), getString("s_sub_hidden_feed_rows"),
            getString("s_hidden_feed_rows_dialog_title"),
            itemToText = {
                val player = LocalPlayerState.current
                remember(it) {
                    LocalisedString.deserialise(it).getString(player.context)
                }
            },
            textToItem = {
                RawLocalisedString(it).serialise()
            }
        )
    )
}
