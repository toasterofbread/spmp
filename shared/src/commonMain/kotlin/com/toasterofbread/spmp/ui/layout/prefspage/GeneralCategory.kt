package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.composesettings.ui.item.SettingsComposableItem
import com.toasterofbread.composesettings.ui.item.SettingsDropdownItem
import com.toasterofbread.composesettings.ui.item.SettingsGroupItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsSliderItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.FontMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.Languages
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.WidthShrinkText

// TODO Allow setting to any language
fun getLanguageDropdownItem(
    key: Settings,
    available_languages: List<Languages.LanguageInfo>,
    title: String,
    subtitle: String?
): SettingsItem {
    return SettingsDropdownItem(
        SettingsValueState(
            key.name,
            getValueConverter = {
                val language_code = it as String
                if (language_code.isBlank()) {
                    return@SettingsValueState 0
                }

                val index = available_languages.indexOfFirst { it.code == language_code }
                if (index == -1) {
                    key.set(null)
                    return@SettingsValueState 0
                }
                else {
                    return@SettingsValueState index + 1
                }
            },
            setValueConverter = { index ->
                if (index == 0) {
                    ""
                }
                else {
                    available_languages[index - 1].code
                }
            }
        ),
        title, subtitle,
        available_languages.size + 1,
        { i ->
            if (i == 0) {
                getString("system_language")
            }
            else {
                available_languages[i - 1].readable_name
            }
        }
    ) { i ->
        if (i == 0) {
            getString("system_language")
        }
        else {
            val lang = available_languages[i - 1]
            "${lang.code} / ${lang.readable_name}"
        }
    }
}

internal fun getGeneralCategory(language: String, available_languages: List<Languages.LanguageInfo>): List<SettingsItem> {
    return listOf(
        SettingsComposableItem {
            WidthShrinkText(getString("language_change_restart_notice"))
        },

        getLanguageDropdownItem(
            Settings.KEY_LANG_UI,
            available_languages,
            getString("s_key_interface_lang"), getString("s_sub_interface_lang")
        ),

        getLanguageDropdownItem(
            Settings.KEY_LANG_DATA,
            available_languages,
            getString("s_key_data_lang"), getString("s_sub_data_lang")
        ),

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

        SettingsDropdownItem(
            SettingsValueState(Settings.KEY_FONT.name),
            getString("s_key_font"),
            null,
            FontMode.values().size,
        ) { index ->
            FontMode.values()[index].getReadable(language)
        },

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
