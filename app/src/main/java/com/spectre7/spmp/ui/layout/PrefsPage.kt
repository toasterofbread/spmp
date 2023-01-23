package com.spectre7.spmp.ui.layout

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.spectre7.composesettings.model.*
import com.spectre7.composesettings.ui.SettingsInterface
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerAccessibilityService
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.Permissions
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import com.spectre7.spmp.ui.component.PillMenuActionGetter

enum class Page { ROOT, ACCESSIBILITY_SERVICE }

@Composable
fun PrefsPage(setPillAction: ((@Composable PillMenuActionGetter.() -> Unit)?) -> Unit, setOverlayPage: (page: OverlayPage) -> Unit) {

    val interface_lang = remember { SettingsValueState<Int>(Settings.KEY_LANG_UI.name).init(Settings.prefs, Settings.getDefaultProvider()) }
    var language_data by remember { mutableStateOf(MainActivity.languages.values.elementAt(interface_lang.value)) }

    OnChangedEffect(interface_lang.value) {
        language_data = MainActivity.languages.values.elementAt(interface_lang.value)
    }

    PlayerAccessibilityService.isEnabled(MainActivity.context)

    lateinit var settings_interface: SettingsInterface
    settings_interface = remember {
        SettingsInterface(
            MainActivity.theme, 
            Page.ROOT.ordinal,
            MainActivity.context,
            Settings.prefs, 
            Settings.getDefaultProvider(),
            {
                when (Page.values()[it]) {

                    Page.ROOT -> SettingsPage(getString(R.string.s_page_preferences), listOf(

                        SettingsGroup(getString(R.string.s_group_general)),

                        SettingsItemDropdown(
                            interface_lang,
                            getString(R.string.s_key_interface_lang), getString(R.string.s_sub_interface_lang),
                            MainActivity.languages.values.first().size,
                            { i ->
                                language_data.entries.elementAt(i).key
                            }
                        ) { i ->
                            val language = language_data.entries.elementAt(i)
                            "${language.key} / ${language.value}"
                        },

                        SettingsItemDropdown(
                            SettingsValueState(Settings.KEY_LANG_DATA.name),
                            getString(R.string.s_key_data_lang), getString(R.string.s_sub_data_lang),
                            MainActivity.languages.values.first().size,
                            { i ->
                                language_data.entries.elementAt(i).key
                            }
                        ) { i ->
                            val language = language_data.entries.elementAt(i)
                            "${language.key} / ${language.value}"
                        },

                        SettingsGroup(getString(R.string.s_group_theming)),

                        SettingsItemMultipleChoice(
                            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
                            getString(R.string.s_key_accent_source), null,
                            2, false
                        ) { choice ->
                            when (choice) {
                                0 ->    getString(R.string.s_option_accent_thumbnail)
                                else -> getString(R.string.s_option_accent_system)
                            }
                        },

                        SettingsItemMultipleChoice(
                            SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name),
                            getString(R.string.s_key_np_theme_mode), null,
                            3, false
                        ) { choice ->
                            when (choice) {
                                0 ->    getString(R.string.s_option_np_accent_background)
                                1 ->    getString(R.string.s_option_np_accent_elements)
                                else -> getString(R.string.s_option_np_accent_none)
                            }
                        },

                        SettingsGroup(getString(R.string.s_group_lyrics)),

                        SettingsItemToggle(
                            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
                            getString(R.string.s_key_lyrics_follow_enabled), getString(R.string.s_sub_lyrics_follow_enabled)
                        ),

                        SettingsItemSlider(
                            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
                            getString(R.string.s_key_lyrics_follow_offset), getString(R.string.s_sub_lyrics_follow_offset),
                            getString(R.string.s_option_lyrics_follow_offset_top), getString(R.string.s_option_lyrics_follow_offset_bottom), steps = 5
                        ),

                        SettingsItemToggle(
                            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name),
                            getString(R.string.s_key_lyrics_default_furigana), null
                        ),

                        SettingsItemDropdown(
                            SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name),
                            getString(R.string.s_key_lyrics_text_alignment), null, 3
                        ) { i ->
                            when (i) {
                                0 ->    getString(R.string.s_option_lyrics_text_alignment_left)
                                1 ->    getString(R.string.s_option_lyrics_text_alignment_center)
                                else -> getString(R.string.s_option_lyrics_text_alignment_right)
                            }
                        },

                        SettingsItemToggle(
                            SettingsValueState(Settings.KEY_LYRICS_EXTRA_PADDING.name),
                            getString(R.string.s_key_lyrics_extra_padding), getString(R.string.s_sub_lyrics_extra_padding)
                        ),

                        SettingsGroup(getString(R.string.s_group_audio_video)),

                        SettingsItemDropdown(
                            SettingsValueState(Settings.KEY_STREAM_AUDIO_QUALITY.name),
                            getString(R.string.s_key_stream_audio_quality), getString(R.string.s_sub_stream_audio_quality), 3
                        ) { i ->
                            when (i) {
                                Song.AudioQuality.HIGH.ordinal ->   getString(R.string.s_option_audio_quality_high)
                                Song.AudioQuality.MEDIUM.ordinal -> getString(R.string.s_option_audio_quality_medium)
                                else ->                             getString(R.string.s_option_audio_quality_low)
                            }
                        },

                        SettingsItemDropdown(
                            SettingsValueState(Settings.KEY_DOWNLOAD_AUDIO_QUALITY.name),
                            getString(R.string.s_key_download_audio_quality), getString(R.string.s_sub_download_audio_quality), 3
                        ) { i ->
                            when (i) {
                                Song.AudioQuality.HIGH.ordinal ->   getString(R.string.s_option_audio_quality_high)
                                Song.AudioQuality.MEDIUM.ordinal -> getString(R.string.s_option_audio_quality_medium)
                                else ->                             getString(R.string.s_option_audio_quality_low)
                            }
                        },

                        SettingsGroup(getString(R.string.s_group_other)),

                        SettingsItemSubpage(
                            getString(R.string.s_page_acc_service),
                            null,
                            Page.ACCESSIBILITY_SERVICE.ordinal
                        )

                    ), Modifier.fillMaxSize())

                    Page.ACCESSIBILITY_SERVICE -> SettingsPage(getString(R.string.s_page_acc_service), listOf(

                        SettingsItemAccessibilityService(
                            getString(R.string.s_acc_service_enabled),
                            getString(R.string.s_acc_service_disabled),
                            getString(R.string.s_acc_service_enable),
                            getString(R.string.s_acc_service_disable),
                            object : SettingsItemAccessibilityService.AccessibilityServiceBridge {
                                override fun addEnabledListener(
                                    listener: (Boolean) -> Unit,
                                    context: Context
                                ) {
                                    PlayerAccessibilityService.addEnabledListener(listener, context)
                                }

                                override fun removeEnabledListener(
                                    listener: (Boolean) -> Unit,
                                    context: Context
                                ) {
                                    PlayerAccessibilityService.removeEnabledListener(listener, context)
                                }

                                override fun isEnabled(context: Context): Boolean {
                                    return PlayerAccessibilityService.isEnabled(context)
                                }

                                override fun setEnabled(enabled: Boolean) {
                                    if (!enabled) {
                                        PlayerAccessibilityService.disable()
                                        return
                                    }

                                    if (Permissions.hasPermission(Manifest.permission.WRITE_SECURE_SETTINGS, MainActivity.context)) {
                                        PlayerAccessibilityService.enable(MainActivity.context, true)
                                        return
                                    }

                                    val dialog = AlertDialog.Builder(MainActivity.context)
                                    dialog.setCancelable(true)
                                    dialog.setTitle("Enabling accessibility service")
                                    dialog.setMessage("Service can be enabled automatically by granting write secure settings permission using root")
                                    dialog.setPositiveButton("Use root") { _, _ ->
                                        PlayerAccessibilityService.enable(MainActivity.context, true)
                                    }
                                    dialog.setNeutralButton("Enable manually") { _, _ ->
                                        PlayerAccessibilityService.enable(MainActivity.context, false)
                                    }
                                    dialog.setNegativeButton("Cancel") { _, _ -> }
                                    dialog.create().show()
                                }
                            }
                        ),

                        SettingsItemMultipleChoice(
                            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_MODE.name),
                            getString(R.string.s_key_vol_intercept_mode),
                            getString(R.string.s_sub_vol_intercept_mode),
                            PlayerAccessibilityService.VOLUME_INTERCEPT_MODE.values().size,
                            false
                        ) { mode ->
                            when (mode) {
                                0 -> getString(R.string.s_option_vol_intercept_mode_always)
                                1 -> getString(R.string.s_option_vol_intercept_mode_app)
                                else -> getString(R.string.s_option_vol_intercept_mode_never)
                            }
                        },

                        SettingsItemToggle(
                            SettingsValueState(Settings.KEY_ACC_SCREEN_OFF.name),
                            getString(R.string.s_key_acc_screen_off),
                            getString(R.string.s_sub_acc_screen_off)
                        ) { checked, allowChange ->
                            if (!checked) {
                                allowChange(true)
                                return@SettingsItemToggle
                            }

                            Permissions.requestRootPermission(allowChange)
                        },

                        SettingsItemToggle(
                            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name),
                            getString(R.string.s_key_vol_intercept_notification),
                            getString(R.string.s_key_vol_intercept_notification)
                        ) { checked, allowChange ->
                            if (!checked) {
                                allowChange(true)
                                return@SettingsItemToggle
                            }

                            if (!android.provider.Settings.canDrawOverlays(MainActivity.context)) {
                                Permissions.requestPermission(Manifest.permission.SYSTEM_ALERT_WINDOW, MainActivity.context) { result, error ->
                                    allowChange(result == Permissions.GrantError.OK)
                                }
                                return@SettingsItemToggle
                            }

                            allowChange(true)
                        },

                    ), Modifier.fillMaxSize())
                }
            }, 
            { page: Int ->
                if (page == Page.ROOT.ordinal) {
                    setPillAction(null)
                }
                else {
                    setPillAction {
                        ActionButton(
                            Icons.Filled.ArrowBack
                        ) {
                            settings_interface.goBack()
                        }
                    }
                }
            },
            {
                setOverlayPage(OverlayPage.NONE)
            }
        ) 
    }

    BoxWithConstraints(
        Modifier
            .background(
                MainActivity
                    .theme
                    .getBackground(false)
            )
            .pointerInput(Unit) {}) {
        settings_interface.Interface(Modifier.requiredHeight(LocalConfiguration.current.screenHeightDp.dp - MINIMISED_NOW_PLAYING_HEIGHT.dp))
    }
}
