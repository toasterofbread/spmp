package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import com.toasterofbread.settings.ui.item.SettingsGroupItem
import com.toasterofbread.settings.ui.item.SettingsItem
import com.toasterofbread.settings.ui.item.SettingsComposableItem
import com.toasterofbread.settings.ui.item.SettingsDropdownItem
import com.toasterofbread.settings.ui.item.SettingsSliderItem
import com.toasterofbread.settings.ui.item.SettingsToggleItem
import com.toasterofbread.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getLanguageName
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.WidthShrinkText

internal fun getGeneralCategory(): List<SettingsItem> {
    return listOf(
        SettingsComposableItem {
            WidthShrinkText(getString("language_change_restart_notice"))
        },

        SettingsDropdownItem(
            SettingsValueState(Settings.KEY_LANG_UI.name),
            getString("s_key_interface_lang"), getString("s_sub_interface_lang"),
            SpMp.getLanguageCount(),
            { i ->
                getLanguageName(i)
            }
        ) { i ->
            val code = SpMp.getLanguageCode(i)
            val name = getLanguageName(i)
            "$code / $name"
        },

        SettingsDropdownItem(
            SettingsValueState(Settings.KEY_LANG_DATA.name),
            getString("s_key_data_lang"), getString("s_sub_data_lang"),
            SpMp.getLanguageCount(),
            { i ->
                getLanguageName(i)
            }
        ) { i ->
            val code = SpMp.getLanguageCode(i)
            val name = getLanguageName(i)
            "$code / $name"
        },

        SettingsSliderItem(
            SettingsValueState<Int>(Settings.KEY_VOLUME_STEPS.name),
            getString("s_key_vol_steps"),
            getString("s_sub_vol_steps"),
            "0",
            "100",
            range = 0f..100f
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_OPEN_NP_ON_SONG_PLAYED.name),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_MULTISELECT_CANCEL_ON_ACTION.name),
            getString("s_key_multiselect_cancel_on_action"),
            getString("s_sub_multiselect_cancel_on_action")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_PERSISTENT_QUEUE.name),
            getString("s_key_persistent_queue"),
            getString("s_sub_persistent_queue")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_ADD_SONGS_TO_HISTORY.name),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TREAT_SINGLES_AS_SONG.name),
            getString("s_key_treat_singles_as_song"),
            getString("s_sub_treat_singles_as_song")
        ),

        SettingsGroupItem(getString("s_group_long_press_menu")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LPM_CLOSE_ON_ACTION.name),
            getString("s_key_lpm_close_on_action"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_LPM_INCREMENT_PLAY_AFTER.name),
            getString("s_key_lpm_increment_play_after"), null
        )
    )
}
