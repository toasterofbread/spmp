package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsGroupItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.resources.getString

internal fun getBehaviourCategoryItems(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.OPEN_NP_ON_SONG_PLAYED.getName()),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.START_RADIO_ON_SONG_PRESS.getName()),
            getString("s_key_start_radio_on_song_press"),
            getString("s_sub_start_radio_on_song_press")
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.MULTISELECT_CANCEL_ON_ACTION.getName()),
            getString("s_key_multiselect_cancel_on_action"),
            getString("s_sub_multiselect_cancel_on_action")
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.MULTISELECT_CANCEL_ON_NONE_SELECTED.getName()),
            getString("s_key_multiselect_cancel_on_none_selected"),
            null
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.TREAT_SINGLES_AS_SONG.getName()),
            getString("s_key_treat_singles_as_song"),
            getString("s_sub_treat_singles_as_song")
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.SHOW_LIKES_PLAYLIST.getName()),
            getString("s_key_show_likes_playlist"), null
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.SEARCH_SHOW_SUGGESTIONS.getName()),
            getString("s_key_search_show_suggestions"), null
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.STOP_PLAYER_ON_APP_CLOSE.getName()),
            getString("s_key_stop_player_on_app_close"),
            getString("s_sub_stop_player_on_app_close")
        ),

        SettingsGroupItem(getString("s_group_long_press_menu")),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.LPM_CLOSE_ON_ACTION.getName()),
            getString("s_key_lpm_close_on_action"), null
        ),

        SettingsToggleItem(
            SettingsValueState(BehaviourSettings.Key.LPM_INCREMENT_PLAY_AFTER.getName()),
            getString("s_key_lpm_increment_play_after"), null
        )
    )
}
