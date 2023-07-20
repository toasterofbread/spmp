package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.settings.ui.item.SettingsGroupItem
import com.toasterofbread.settings.ui.item.SettingsItem
import com.toasterofbread.settings.ui.item.SettingsSliderItem
import com.toasterofbread.settings.ui.item.SettingsToggleItem
import com.toasterofbread.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getFeedCategory(): List<SettingsItem> {
    return listOf(
        SettingsGroupItem(getString("s_group_rec_feed")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_FILTERS.name),
            getString("s_key_feed_show_filters"), null
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_INITIAL_ROWS.name),
            getString("s_key_feed_initial_rows"),
            getString("s_sub_feed_initial_rows"),
            "1",
            "10",
            range = 1f..10f
        ),

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_FEED_SQUARE_PREVIEW_TEXT_LINES.name),
            getString("s_key_feed_square_preview_text_lines"),
            null,
            "1",
            "5",
            range = 1f..5f
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_RADIOS.name),
            getString("s_key_feed_show_radios"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_LISTEN_ROW.name),
            getString("s_key_feed_show_listen_row"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_MIX_ROW.name),
            getString("s_key_feed_show_mix_row"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_NEW_ROW.name),
            getString("s_key_feed_show_new_row"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_MOODS_ROW.name),
            getString("s_key_feed_show_moods_row"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_FEED_SHOW_CHARTS_ROW.name),
            getString("s_key_feed_show_charts_row"), null
        )
    )
}
