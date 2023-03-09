package com.spectre7.spmp.ui.layout

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.spectre7.composesettings.ui.SettingsInterface
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.composesettings.ui.SettingsPageWithItems
import com.spectre7.settings.model.*
import com.spectre7.settings.ui.SettingsItemThemeSelector
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerAccessibilityService
import com.spectre7.spmp.R
import com.spectre7.spmp.api.YoutubeMusicAuthInfo
import com.spectre7.spmp.api.YoutubeMusicLogin
import com.spectre7.spmp.model.AccentColourSource
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.spmp.ui.theme.ThemeManager
import com.spectre7.utils.*

enum class Page { ROOT, ACCESSIBILITY_SERVICE, YOUTUBE_MUSIC_LOGIN }

@Composable
fun PrefsPage(pill_menu: PillMenu, close: () -> Unit) {

    val interface_lang = remember { SettingsValueState<Int>(Settings.KEY_LANG_UI.name).init(Settings.prefs, Settings.Companion::provideDefault) }
    var language_data by remember { mutableStateOf(MainActivity.languages.values.elementAt(interface_lang.value)) }
    OnChangedEffect(interface_lang.value) {
        language_data = MainActivity.languages.values.elementAt(interface_lang.value)
    }

    val ytm_auth = remember {
        SettingsValueState<YoutubeMusicAuthInfo>(
            Settings.KEY_YTM_AUTH.name,
            converter = { set ->
                set?.let { YoutubeMusicAuthInfo(it as Set<String>) }
            }
        ).init(Settings.prefs, Settings.Companion::provideDefault)
    }

    lateinit var settings_interface: SettingsInterface

    val pill_menu_action_overrider: @Composable PillMenu.Action.(i: Int) -> Boolean = remember { { i ->
        if (i == 0) {
            var go_back by remember { mutableStateOf(false) }
            LaunchedEffect(go_back) {
                if (go_back) {
                    settings_interface.goBack()
                }
            }

            ActionButton(
                Icons.Filled.ArrowBack
            ) {
                go_back = true
            }
            true
        }
        else {
            false
        }
    } }

    var show_reset_confirmation by remember { mutableStateOf(false) }

    var reset by remember { mutableStateOf(false) }
    OnChangedEffect(reset) {
        settings_interface.current_page.resetKeys()
    }

    if (show_reset_confirmation) {
        AlertDialog(
            { show_reset_confirmation = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        reset = !reset
                        show_reset_confirmation = false
                    }
                ) {
                    Text(getString(R.string.action_confirm_action))
                }
            },
            dismissButton = { TextButton( { show_reset_confirmation = false } ) { Text(getString(R.string.action_deny_action)) } },
            title = { Text(getString(R.string.prompt_confirm_action)) },
            text = {
                Text(getString(R.string.prompt_confirm_settings_page_reset))
            }
        )
    }

    LaunchedEffect(Unit) {
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Refresh
                ) {
                    show_reset_confirmation = true
                }
            }
        }
    }

    settings_interface = remember {
        SettingsInterface(
            { Theme.current },
            Page.ROOT.ordinal,
            MainActivity.context,
            Settings.prefs,
            Settings.Companion::provideDefault,
            {
                when (Page.values()[it]) {
                    Page.ROOT -> getRootPage(interface_lang, language_data, ytm_auth)
                    Page.ACCESSIBILITY_SERVICE -> getAccessibilityServicePage()
                    Page.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth)
                }
            },
            pill_menu,
            { page: Int? ->
                if (page == Page.ROOT.ordinal) {
                    pill_menu.removeActionOverrider(pill_menu_action_overrider)
                }
                else {
                    pill_menu.addActionOverrider(pill_menu_action_overrider)
                }
            },
            {
                close()
            }
        ) 
    }

    BoxWithConstraints(
        Modifier
            .background(Theme.current.background)
            .pointerInput(Unit) {}
    ) {
        settings_interface.Interface(
            LocalConfiguration.current.screenHeightDp.dp,
            content_padding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp + 70.dp)
        )
    }
}

private fun getRootPage(
    interface_lang: SettingsValueState<Int>,
    language_data: Map<String, String>,
    ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>
): SettingsPage {
    return SettingsPageWithItems(
        getString(R.string.s_page_preferences),
        groupAuth(ytm_auth)
            + groupGeneral(interface_lang, language_data)
            + groupHomeFeed()
            + groupTheming(Theme.manager)
            + groupLyrics()
            + groupDownloads()
            + groupAudioVideo()
            + groupOther(),
        Modifier.fillMaxSize()
    )
}

private fun getAccessibilityServicePage(): SettingsPage {
    return SettingsPageWithItems(getString(R.string.s_page_acc_service), listOf(

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
                    dialog.setTitle(getString(R.string.acc_ser_enable_dialog_title))
                    dialog.setMessage(getString(R.string.acc_ser_enable_dialog_body))
                    dialog.setPositiveButton(getString(R.string.acc_ser_enable_dialog_btn_root)) { _, _ ->
                        PlayerAccessibilityService.enable(MainActivity.context, true)
                    }
                    dialog.setNeutralButton(getString(R.string.acc_ser_enable_dialog_btn_manual)) { _, _ ->
                        PlayerAccessibilityService.enable(MainActivity.context, false)
                    }
                    dialog.setNegativeButton(getString(R.string.action_cancel)) { _, _ -> }
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
        }

    ), Modifier.fillMaxSize())
}

private fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>): SettingsPage {
    return object : SettingsPage(null) {
        override val disable_padding: Boolean = true
        override val scrolling: Boolean = false

        @Composable
        override fun PageView(
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            YoutubeMusicLogin(Modifier.fillMaxSize()) { auth_info ->
                auth_info.fold({
                    ytm_auth.value = it
                }, {
                    TODO(it.toString())
                })
                goBack()
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}

private fun groupAuth(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>): List<SettingsItem> {
    return listOf(
        object : SettingsItem() {
            override fun initialiseValueStates(
                prefs: SharedPreferences,
                default_provider: (String) -> Any,
            ) {}

            override fun resetValues() {
                ytm_auth.reset()
            }

            @Composable
            override fun GetItem(
                theme: Theme,
                openPage: (Int) -> Unit,
                openCustomPage: (SettingsPage) -> Unit,
            ) {
                Box(Modifier
                    .background(Theme.current.vibrant_accent, SETTINGS_ITEM_ROUNDED_SHAPE)
                    .padding(horizontal = 10.dp)
                ) {
                    Crossfade(ytm_auth.value) { auth ->
                        Column() {
                            WidthShrinkText(
                                getString(
                                    if (auth.initialised) "Signed in to YouTube Music"
                                    else "Not signed in to YouTube Music"
                                ),
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {

                                auth.own_channel?.also {
                                    it.PreviewLong(
                                        content_colour = Theme.current.on_background_provider,
                                        playerProvider = { TODO() },
                                        enable_long_press_menu = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Button({
                                    if (auth.initialised) {
                                        resetValues()
                                    }
                                    else {
                                        openPage(Page.YOUTUBE_MUSIC_LOGIN.ordinal)
                                    }
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = Theme.current.background,
                                    contentColor = Theme.current.on_background
                                )) {
                                    Text(getString(if (auth.initialised) "Sign out" else "Sign in"))
                                }

                                ShapedIconButton(
                                    {

                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Theme.current.background,
                                        contentColor = Theme.current.on_background
                                    ),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Filled.Info, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

private fun groupGeneral(interface_lang: SettingsValueState<Int>, language_data: Map<String, String>): List<SettingsItem> {
    return listOf(
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

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_VOLUME_STEPS.name),
            getString(R.string.s_key_vol_steps),
            getString(R.string.s_sub_vol_steps),
            "0",
            "100",
            range = 0f .. 100f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_OPEN_NP_ON_SONG_PLAYED.name),
            getString(R.string.s_key_open_np_on_song_played),
            getString(R.string.s_sub_open_np_on_song_played)
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_PERSISTENT_QUEUE.name),
            getString(R.string.s_key_persistent_queue),
            getString(R.string.s_sub_persistent_queue)
        )
    )
}

private fun groupHomeFeed(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString(R.string.s_group_home_feed)),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_FEED_INITIAL_ROWS.name),
            getString(R.string.s_key_feed_initial_rows),
            getString(R.string.s_sub_feed_initial_rows),
            "1",
            "10",
            range = 1f .. 10f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_ENABLE_LISTEN_ROW.name),
            getString(R.string.s_key_feed_enable_listen_row), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_ENABLE_MIX_ROW.name),
            getString(R.string.s_key_feed_enable_mix_row), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_ENABLE_NEW_ROW.name),
            getString(R.string.s_key_feed_enable_new_row), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_ENABLE_MOODS_ROW.name),
            getString(R.string.s_key_feed_enable_moods_row), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_ENABLE_CHARTS_ROW.name),
            getString(R.string.s_key_feed_enable_charts_row), null
        )
    )
}

private fun groupTheming(theme_manager: ThemeManager): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString(R.string.s_group_theming)),

        SettingsItemThemeSelector (
            SettingsValueState(Settings.KEY_CURRENT_THEME.name),
            getString(R.string.s_key_current_theme), null,
            getString(R.string.s_theme_editor_title),
            {
                check(theme_manager.themes.isNotEmpty())
                theme_manager.themes.size
            },
            { theme_manager.themes[it] },
            { index: Int, edited_theme: ThemeData ->
                theme_manager.updateTheme(index, edited_theme)
            },
            { theme_manager.addTheme(Theme.default.copy(name = getString(R.string.theme_title_new))) },
            { theme_manager.removeTheme(it) }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
            getString(R.string.s_key_accent_source), null,
            3, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME     -> getString(R.string.s_option_accent_theme)
                AccentColourSource.THUMBNAIL -> getString(R.string.s_option_accent_thumbnail)
                AccentColourSource.SYSTEM    -> getString(R.string.s_option_accent_system)
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
        }
    )
}

private fun groupLyrics(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString(R.string.s_group_lyrics)),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
            getString(R.string.s_key_lyrics_follow_enabled), getString(R.string.s_sub_lyrics_follow_enabled)
        ),

        SettingsItemSlider(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
            getString(R.string.s_key_lyrics_follow_offset), getString(R.string.s_sub_lyrics_follow_offset),
            getString(R.string.s_option_lyrics_follow_offset_top), getString(R.string.s_option_lyrics_follow_offset_bottom), steps = 5,
            getValueText = null
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
        )
    )
}

private fun groupDownloads(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString(R.string.s_group_download)),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.name),
            getString(R.string.s_key_auto_download_threshold), getString(R.string.s_sub_auto_download_threshold),
            range = 1f .. 10f,
            min_label = "1",
            max_label = "10"
        )
    )
}

private fun groupAudioVideo(): List<SettingsItem> {
    return listOf(
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
        }
    )
}

private fun groupOther(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString(R.string.s_group_other)),

        SettingsItemSubpage(
            getString(R.string.s_page_acc_service),
            null,
            Page.ACCESSIBILITY_SERVICE.ordinal
        )
    )
}