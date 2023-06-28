package com.spectre7.spmp.ui.layout.prefspage

import SpMp
import com.spectre7.settings.model.*
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getLanguageName
import com.spectre7.spmp.resources.getString
import com.spectre7.utils.composable.WidthShrinkText

internal fun getGeneralCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemComposable {
            WidthShrinkText(getString("language_change_restart_notice"))
        },

        SettingsItemDropdown(
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

        SettingsItemDropdown(
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

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_VOLUME_STEPS.name),
            getString("s_key_vol_steps"),
            getString("s_sub_vol_steps"),
            "0",
            "100",
            range = 0f..100f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_OPEN_NP_ON_SONG_PLAYED.name),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_MULTISELECT_CANCEL_ON_ACTION.name),
            getString("s_key_multiselect_cancel_on_action"),
            getString("s_sub_multiselect_cancel_on_action")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_PERSISTENT_QUEUE.name),
            getString("s_key_persistent_queue"),
            getString("s_sub_persistent_queue")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ADD_SONGS_TO_HISTORY.name),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TREAT_SINGLES_AS_SONG.name),
            getString("s_key_treat_singles_as_song"),
            getString("s_sub_treat_singles_as_song")
        ),

        SettingsGroup(getString("s_group_long_press_menu")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LPM_CLOSE_ON_ACTION.name),
            getString("s_key_lpm_close_on_action"), null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LPM_INCREMENT_PLAY_AFTER.name),
            getString("s_key_lpm_increment_play_after"), null
        )
    )
}
