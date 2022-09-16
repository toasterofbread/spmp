package com.spectre7.spmp.ui.layout

import androidx.compose.runtime.Composable
import com.spectre7.spmp.ui.layout.OverlayPage
import com.spectre7.spmp.R
import com.spectre7.spmp.MainActivity.getString
import com.alorma.compose.settings.ui.*
import com.alorma.compose.settings.storage.preferences.rememberPreferenceIntSettingState
import com.alorma.compose.settings.storage.preferences.rememberPreferenceFloatSettingState

@Composable
fun SettingsPage(set_overlay_page: (page: OverlayPage) -> Unit) {
    Column(Modifier.fillMaxHeight()) {

        // App language
        val lang_app_state = rememberPreferenceIntSettingState(key = "lang_app")
        SettingsListDropdown(
            state = lang_app_state,
            title = { Text(getString(R.string.setting_t_lang_app)) },
            subtitle = { Text(getString(R.string.setting_d_lang_app)) },
            items = listOf(getString(R.string.lang_system), "EN", "JA"),
        )

        // Song data language
        val lang_data_app = rememberPreferenceIntSettingState(key = "lang_data")
        SettingsListDropdown(
            state = lang_data_app,
            title = { Text(getString(R.string.setting_t_lang_data)) },
            subtitle = { Text(getString(R.string.setting_d_lang_data)) },
            items = listOf(getString(R.string.lang_system), getString(R.string.lang_native), "EN", "JA"),
        )

        // Now playing theme mode
        val theme_mode_state = rememberPreferenceIntSettingState(key = "theme_mode")
        SettingsList(
            state = theme_mode_state,
            title = { Text(getString(R.string.setting_t_theme_mode)) },
            subtitle = { Text(getString(R.string.setting_d_theme_mode)) },
            items = listOf(getString(R.string.theme_mode_background), getString(R.string.theme_mode_elements))
            // action = {
            //     IconButton(onClick = { singleChoiceState.reset() }) {
            //         Icon(
            //             imageVector = Icons.Default.Clear,
            //             contentDescription = "Clear",
            //         )
            //     }
            // }
        )

        // WiFi audio quality
        val quality_wifi_state = rememberPreferenceIntSettingState(key = "audio_quality_wifi")
        SettingsList(
            state = quality_wifi_state,
            title = { Text(getString(R.string.setting_t_audio_quality_wifi)) },
            subtitle = { Text(getString(R.string.setting_d_audio_quality_wifi)) },
            items = listOf(getString(R.string.audio_quality_139), getString(R.string.audio_quality_140))
        )

        // Mobile data audio quality
        val quality_mobile_state = rememberPreferenceIntSettingState(key = "audio_quality_mobile")
        SettingsList(
            state = quality_mobile_state,
            title = { Text(getString(R.string.setting_t_audio_quality_mobile)) },
            subtitle = { Text(getString(R.string.setting_d_audio_quality_mobile)) },
            items = listOf(getString(R.string.audio_quality_139), getString(R.string.audio_quality_140))
        )

        // Auto download views threshold
        val auto_download_threshold_state = rememberPreferenceFloatSettingState(key = "auto_download_threshold")
        SettingsSlider(
            state = auto_download_threshold_state,
            icon = {
                Icon(imageVector = Icons.Default.Download, contentDescription = "")
            },
            title = { Text(getString(R.string.setting_t_auto_download_threshold)) },
            // TODO
            // subtitle = { Text(getString(R.string.setting_d_auto_download_threshold)) },
            steps = 100,
            valueRange = 0f...100f
        )

    }
}