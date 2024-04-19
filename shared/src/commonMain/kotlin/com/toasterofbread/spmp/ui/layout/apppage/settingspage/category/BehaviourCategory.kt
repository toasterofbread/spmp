package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.resources.getString

internal fun getBehaviourCategoryItems(): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.OPEN_NP_ON_SONG_PLAYED.getName()),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.START_RADIO_ON_SONG_PRESS.getName()),
            getString("s_key_start_radio_on_song_press"),
            getString("s_sub_start_radio_on_song_press")
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.MULTISELECT_CANCEL_ON_ACTION.getName()),
            getString("s_key_multiselect_cancel_on_action"),
            getString("s_sub_multiselect_cancel_on_action")
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.MULTISELECT_CANCEL_ON_NONE_SELECTED.getName()),
            getString("s_key_multiselect_cancel_on_none_selected"),
            null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.TREAT_SINGLES_AS_SONG.getName()),
            getString("s_key_treat_singles_as_song"),
            getString("s_sub_treat_singles_as_song")
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE.getName()),
            getString("s_key_treat_any_single_item_playlist_as_single"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.SHOW_LIKES_PLAYLIST.getName()),
            getString("s_key_show_likes_playlist"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.SEARCH_SHOW_SUGGESTIONS.getName()),
            getString("s_key_search_show_suggestions"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.STOP_PLAYER_ON_APP_CLOSE.getName()),
            getString("s_key_stop_player_on_app_close"),
            getString("s_sub_stop_player_on_app_close")
        ),

        GroupSettingsItem(getString("s_group_long_press_menu")),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.LPM_CLOSE_ON_ACTION.getName()),
            getString("s_key_lpm_close_on_action"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.LPM_INCREMENT_PLAY_AFTER.getName()),
            getString("s_key_lpm_increment_play_after"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(BehaviourSettings.Key.DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL.getName()),
            getString("s_key_desktop_lpm_keep_on_background_scroll"), getString("s_sub_desktop_lpm_keep_on_background_scroll")
        )
    )
}
