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
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.Permissions
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast

enum class Page { ROOT, ACCESSIBILITY_SERVICE }

@Composable
fun PrefsPage(setOverlayPage: (page: OverlayPage) -> Unit) {

    val interface_lang = remember {
        SettingsValueState<Int>(
            Settings.KEY_LANG_UI.name,
            Settings.prefs,
            Settings.getDefaultProvider()
        )
    }
    var language_data by remember { mutableStateOf(MainActivity.languages.values.elementAt(interface_lang.value)) }

    OnChangedEffect(interface_lang.value) {
        language_data = MainActivity.languages.values.elementAt(interface_lang.value)
    }

    PlayerAccessibilityService.isEnabled(MainActivity.context)

    val settings_interface: SettingsInterface = remember { SettingsInterface(MainActivity.theme, Page.ROOT.ordinal, {
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
                    SettingsValueState(Settings.KEY_LANG_DATA.name, Settings.prefs, Settings.getDefaultProvider()),
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
                    SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_accent_source), null,
                    2, false
                ) { choice ->
                    when (choice) {
                        0 -> {
                            getString(R.string.s_option_accent_thumbnail)
                        }
                        else ->  {
                            getString(R.string.s_option_accent_system)
                        }
                    }
                },

                SettingsItemMultipleChoice(
                    SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_np_theme_mode), null,
                    3, false
                ) { choice ->
                    when (choice) {
                        0 -> {
                            getString(R.string.s_option_np_accent_background)
                        }
                        1 -> {
                            getString(R.string.s_option_np_accent_elements)
                        }
                        else -> {
                            getString(R.string.s_option_np_accent_none)
                        }
                    }
                },

                SettingsGroup(getString(R.string.s_group_lyrics)),

                SettingsItemToggle(
                    SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_lyrics_follow_enabled), getString(R.string.s_sub_lyrics_follow_enabled)
                ),

                SettingsItemSlider(
                    SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_lyrics_follow_offset), getString(R.string.s_sub_lyrics_follow_offset),
                    getString(R.string.s_option_lyrics_follow_offset_top), getString(R.string.s_option_lyrics_follow_offset_bottom), steps = 5
                ),

                SettingsItemToggle(
                    SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_lyrics_default_furigana), null
                ),

                SettingsItemDropdown(
                    SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_lyrics_text_alignment), null, 3
                ) { i ->
                    when (i) {
                        0 -> getString(R.string.s_option_lyrics_text_alignment_left)
                        1 -> getString(R.string.s_option_lyrics_text_alignment_center)
                        else -> getString(R.string.s_option_lyrics_text_alignment_right)
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
                    SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_MODE.name, Settings.prefs, Settings.getDefaultProvider()),
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
                    SettingsValueState(Settings.KEY_ACC_SCREEN_OFF.name, Settings.prefs, Settings.getDefaultProvider()),
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
                    SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name, Settings.prefs, Settings.getDefaultProvider()),
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
    }, MainActivity.context, {
        setOverlayPage(OverlayPage.NONE)
    }) }

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
