package com.spectre7.spmp.ui.layout

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.spectre7.composesettings.ui.SettingsInterface
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.composesettings.ui.SettingsPageWithItems
import com.spectre7.settings.model.*
import com.spectre7.settings.ui.SettingsItemThemeSelector
import com.spectre7.spmp.PlayerAccessibilityService
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.DiscordStatus
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.resources.getLanguageName
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.spmp.ui.theme.ThemeManager
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.ShapedIconButton
import com.spectre7.utils.composable.WidthShrinkText
import com.spectre7.utils.getContrasted
import com.spectre7.utils.modifier.background

private enum class Page { ROOT, YOUTUBE_MUSIC_LOGIN, DISCORD_LOGIN }
private enum class Category {
    GENERAL,
    FEED,
    THEME,
    LYRICS,
    DOWNLOAD,
    OTHER;

    fun getIcon(filled: Boolean = false): ImageVector = when (this) {
        GENERAL -> if (filled) Icons.Filled.Settings else Icons.Outlined.Settings
        FEED -> if (filled) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted
        THEME -> if (filled) Icons.Filled.Palette else Icons.Outlined.Palette
        LYRICS -> if (filled) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        DOWNLOAD -> if (filled) Icons.Filled.Download else Icons.Outlined.Download
        OTHER -> if (filled) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
    }

    fun getTitle(): String = when (this) {
        GENERAL -> getString("s_cat_general")
        FEED -> getString("s_cat_home_page")
        THEME -> getString("s_cat_theming")
        LYRICS -> getString("s_cat_lyrics")
        DOWNLOAD -> getString("s_cat_download")
        OTHER -> getString("s_cat_other")
    }
}

@Composable
fun PrefsPage(pill_menu: PillMenu, playerProvider: () -> PlayerViewContext, close: () -> Unit) {
    var current_category: Category by remember { mutableStateOf(Category.GENERAL) }

    val ytm_auth = remember {
        SettingsValueState(
            Settings.KEY_YTM_AUTH.name,
            converter = { set ->
                set?.let { YoutubeMusicAuthInfo(it as Set<String>) } ?: YoutubeMusicAuthInfo()
            }
        ).init(Settings.prefs, Settings.Companion::provideDefault)
    }

    val discord_auth = remember {
        SettingsValueState<String>(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name).init(Settings.prefs, Settings.Companion::provideDefault)
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
        PlatformAlertDialog(
            { show_reset_confirmation = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        reset = !reset
                        show_reset_confirmation = false
                    }
                ) {
                    Text(getString("action_confirm_action"))
                }
            },
            dismissButton = { TextButton( { show_reset_confirmation = false } ) { Text(getString("action_deny_action")) } },
            title = { Text(getString("prompt_confirm_action")) },
            text = {
                Text(getString("prompt_confirm_settings_page_reset"))
            }
        )
    }

    val extra_action: @Composable PillMenu.Action.(Int) -> Unit = {
        if (it == 1) {
            ActionButton(
                Icons.Filled.Refresh
            ) {
                show_reset_confirmation = true
            }
        }
    }
    val alongside_action: @Composable PillMenu.Action.() -> Unit = {
        Row(fill_modifier
            .border(1.dp, background_colour, CircleShape)
            .background(CircleShape, Theme.current.background_provider)
            .padding(horizontal = 5.dp)
        ) {
            for (category in Category.values()) {
                Box(
                    Modifier.fillMaxWidth(1f / (Category.values().size - category.ordinal).toFloat()),
                    contentAlignment = Alignment.Center
                ) {

                    Crossfade(category == current_category) { current ->
                        val button_colour = if (current) background_colour else Color.Transparent
                        ShapedIconButton(
                            { current_category = category },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = button_colour,
                                contentColor = button_colour.getContrasted()
                            )
                        ) {
                            Icon(category.getIcon(current), null)
                        }
                    }
                }
            }
        }
    }

    settings_interface = remember {
        SettingsInterface(
            { Theme.current },
            Page.ROOT.ordinal,
            SpMp.context,
            Settings.prefs,
            Settings.Companion::provideDefault,
            pill_menu,
            {
                when (Page.values()[it]) {
                    Page.ROOT -> SettingsPageWithItems(
                        { current_category.getTitle() },
                        {
                            when (current_category) {
                                Category.GENERAL -> getGeneralCategory(ytm_auth, playerProvider)
                                Category.FEED -> getFeedCategory()
                                Category.THEME -> getThemeCategory(Theme.manager)
                                Category.LYRICS -> getLyricsCategory()
                                Category.DOWNLOAD -> getDownloadCategory()
                                Category.OTHER -> getOtherCategory(discord_auth)
                            }
                        }
                    )
                    Page.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth)
                    Page.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth)
                }
            },
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

    DisposableEffect(settings_interface.current_page) {
        if (settings_interface.current_page.id == Page.ROOT.ordinal) {
            pill_menu.addExtraAction(action = extra_action)
            pill_menu.addAlongsideAction(alongside_action)
        }
        else {
            pill_menu.removeExtraAction(extra_action)
            pill_menu.removeAlongsideAction(alongside_action)
        }

        onDispose {
            pill_menu.removeExtraAction(extra_action)
            pill_menu.removeAlongsideAction(alongside_action)
        }
    }

    BoxWithConstraints(
        Modifier
            .background(Theme.current.background_provider)
            .pointerInput(Unit) {}
    ) {
        settings_interface.Interface(
            SpMp.context.getScreenHeight() - SpMp.context.getStatusBarHeight(),
            content_padding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp * 2f)
        )
    }
}

private fun getAccessibilityServiceGroup(): List<SettingsItem> {
    if (!PlayerAccessibilityService.isSupported()) {
        return emptyList()
    }

    return listOf(
        SettingsGroup(getString("s_group_acc_service")),

        SettingsItemAccessibilityService(
            getString("s_acc_service_enabled"),
            getString("s_acc_service_disabled"),
            getString("s_acc_service_enable"),
            getString("s_acc_service_disable"),
            object : SettingsItemAccessibilityService.AccessibilityServiceBridge {
                override fun addEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.addEnabledListener(listener, context)
                }

                override fun removeEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.removeEnabledListener(listener, context)
                }

                override fun isEnabled(context: PlatformContext): Boolean {
                    return PlayerAccessibilityService.isEnabled(context)
                }

                override fun setEnabled(enabled: Boolean) {
                    if (!enabled) {
                        PlayerAccessibilityService.disable()
                        return
                    }

                    val context = SpMp.context
                    if (PlayerAccessibilityService.isSettingsPermissionGranted(context)) {
                        PlayerAccessibilityService.enable(context, true)
                        return
                    }

                    TODO()
//                    val dialog = AlertDialog.Builder(MainActivity.context)
//                    dialog.setCancelable(true)
//                    dialog.setTitle(getString("acc_ser_enable_dialog_title"))
//                    dialog.setMessage(getString("acc_ser_enable_dialog_body"))
//                    dialog.setPositiveButton(getString("acc_ser_enable_dialog_btn_root")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, true)
//                    }
//                    dialog.setNeutralButton(getString("acc_ser_enable_dialog_btn_manual")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, false)
//                    }
//                    dialog.setNegativeButton(getString("action_cancel")) { _, _ -> }
//                    dialog.create().show()
                }
            }
        ),

//        SettingsItemMultipleChoice(
//            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_MODE.name),
//            getString("s_key_vol_intercept_mode"),
//            getString("s_sub_vol_intercept_mode"),
//            PlayerAccessibilityService.PlayerAccessibilityServiceVolumeInterceptMode.values().size,
//            false
//        ) { mode ->
//            when (mode) {
//                0 -> getString("s_option_vol_intercept_mode_always")
//                1 -> getString("s_option_vol_intercept_mode_app")
//                else -> getString("s_option_vol_intercept_mode_never")
//            }
//        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ACC_SCREEN_OFF.name),
            getString("s_key_acc_screen_off"),
            getString("s_sub_acc_screen_off")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsItemToggle
            }

            PlayerAccessibilityService.requestRootPermission(allowChange)
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name),
            getString("s_key_vol_intercept_notification"),
            getString("s_key_vol_intercept_notification")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsItemToggle
            }

            if (!PlayerAccessibilityService.isOverlayPermissionGranted(SpMp.context)) {
                PlayerAccessibilityService.requestOverlayPermission(SpMp.context) { success ->
                    allowChange(success)
                }
                return@SettingsItemToggle
            }

            allowChange(true)
        }
    )
}

private fun getDiscordStatusGroup(discord_auth: SettingsValueState<String>): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    var account_token by mutableStateOf(discord_auth.value)

    return listOf(
        SettingsGroup(getString("s_group_discord_status")),

        SettingsItemLargeToggle(
            object : BasicSettingsValueState<Boolean> {
                override var value: Boolean
                    get() = discord_auth.value.isNotEmpty()
                    set(value) {
                        if (!value) {
                            discord_auth.value = ""
                        }
                    }
                override fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun reset() = discord_auth.reset()
                override fun save() = discord_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean = (defaultProvider(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name) as String).isNotEmpty()
            },
            enabled_content = { modifier ->
                if (discord_auth.value.isNotEmpty()) {
                    account_token = discord_auth.value
                }
                if (account_token.isNotEmpty()) {
                    DiscordAccountPreview(account_token, modifier)
                }
            },
            disabled_text = "Not signed in",
            enable_button = "Sign in",
            disable_button = "Sign out",
            warning_text = getString("warning_discord_login"),
            info_text = getString("info_discord_login")
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(Page.DISCORD_LOGIN.ordinal)
            }
            else {
                setEnabled(false)
            }
        },

        SettingsItemInfoText(getString("s_discord_status_text_info")),

        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_NAME.name),
            getString("s_key_discord_status_name"), getString("s_sub_discord_status_name")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_A.name),
            getString("s_key_discord_status_text_a"), getString("s_sub_discord_status_text_a")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_B.name),
            getString("s_key_discord_status_text_b"), getString("s_sub_discord_status_text_b")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_C.name),
            getString("s_key_discord_status_text_c"), getString("s_sub_discord_status_text_c")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_SONG.name),
            getString("s_key_discord_status_show_button_song"), getString("s_sub_discord_status_show_button_song")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_SONG_TEXT.name),
            getString("s_key_discord_status_button_song_text"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_PROJECT.name),
            getString("s_key_discord_status_show_button_project"), getString("s_sub_discord_status_show_button_project")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_PROJECT_TEXT.name),
            getString("s_key_discord_status_button_project_text"), null
        )
    )
}

private fun getOtherCategory(discord_auth: SettingsValueState<String>): List<SettingsItem> {
    return getAccessibilityServiceGroup() + getDiscordStatusGroup(discord_auth)
}

private fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean = true
        override val scrolling: Boolean = false

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            YoutubeMusicLogin(Modifier.fillMaxSize()) { auth_info ->
                auth_info?.fold({
                    ytm_auth.value = it
                }, {
                    throw RuntimeException(it)
                })
                goBack()
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}

private fun getDiscordLoginPage(discord_auth: SettingsValueState<String>): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean = true
        override val scrolling: Boolean = false

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            DiscordLogin(Modifier.fillMaxSize()) { auth_info ->
                auth_info?.fold({
                    discord_auth.value = it ?: ""
                }, {
                    throw RuntimeException(it)
                })
                goBack()
            }
        }

        override suspend fun resetKeys() {
            discord_auth.reset()
        }
    }
}

private fun getGeneralCategory(
    ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>,
    playerProvider: () -> PlayerViewContext
): List<SettingsItem> {
    var own_channel by mutableStateOf(ytm_auth.value.getOwnChannelOrNull())

    return listOf(
        SettingsItemLargeToggle(
            object : BasicSettingsValueState<Boolean> {
                override var value: Boolean
                    get() = ytm_auth.value.initialised
                    set(value) {
                        if (!value) {
                            ytm_auth.value = YoutubeMusicAuthInfo()
                        }
                    }
                override fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun reset() = ytm_auth.reset()
                override fun save() = ytm_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    defaultProvider(Settings.KEY_YTM_AUTH.name) is YoutubeMusicAuthInfo
            },
            enabled_content = { modifier ->
                ytm_auth.value.getOwnChannelOrNull()?.also {
                    own_channel = it
                }

                own_channel?.PreviewLong(
                    MediaItem.PreviewParams(
                        playerProvider,
                        modifier
                    )
                )
            },
            disabled_text = getStringTODO("Not signed in"),
            enable_button = getStringTODO("Sign in"),
            disable_button = getStringTODO("Sign out"),
            warning_text = getString("warning_ytm_login"),
            info_text = getString("info_ytm_login")
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(Page.YOUTUBE_MUSIC_LOGIN.ordinal)
            }
            else {
                setEnabled(false)
            }
        },

        SettingsItemSpacer(5.dp),

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
            range = 0f .. 100f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_OPEN_NP_ON_SONG_PLAYED.name),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
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
        )
    )
}

private fun getFeedCategory(): List<SettingsItem> {
    return listOf(

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_HP_SHOW_VISUALISER.name),
            getString("s_key_hp_show_visualiser"),
            getString("s_sub_hp_show_visualiser")
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_HP_SHOW_TIMED_LYRICS.name),
            getString("s_key_hp_show_timed_lyrics"),
            getString("s_sub_hp_show_timed_lyrics")
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_HP_TOP_BAR_ACTION.name),
            getString("s_key_hp_top_bar_action"), null,
            PlayerViewTopBarAction.values().size,
            false
        ) {
            getString(when (PlayerViewTopBarAction.values()[it]) {
                PlayerViewTopBarAction.TOGGLE_LYRICS -> "s_option_hp_top_bar_action_toggle_lyrics"
                PlayerViewTopBarAction.OPEN_LYRICS -> "s_option_hp_top_bar_action_open_lyrics"
                PlayerViewTopBarAction.NONE -> "s_option_hp_top_bar_action_none"
            })
        },

        SettingsGroup(getString("s_group_rec_feed")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_FILTERS.name),
            getString("s_key_feed_show_filters"), null
        ),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_FEED_INITIAL_ROWS.name),
            getString("s_key_feed_initial_rows"),
            getString("s_sub_feed_initial_rows"),
            "1",
            "10",
            range = 1f .. 10f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_RADIOS.name),
            getString("s_key_feed_show_radios"), null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_LISTEN_ROW.name),
            getString("s_key_feed_show_listen_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_MIX_ROW.name),
            getString("s_key_feed_show_mix_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_NEW_ROW.name),
            getString("s_key_feed_show_new_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_MOODS_ROW.name),
            getString("s_key_feed_show_moods_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_CHARTS_ROW.name),
            getString("s_key_feed_show_charts_row"), null
        )
    )
}

private fun getThemeCategory(theme_manager: ThemeManager): List<SettingsItem> {
    return listOf(
        SettingsItemThemeSelector (
            SettingsValueState(Settings.KEY_CURRENT_THEME.name),
            getString("s_key_current_theme"), null,
            getString("s_theme_editor_title"),
            {
                check(theme_manager.themes.isNotEmpty())
                theme_manager.themes.size
            },
            { theme_manager.themes[it] },
            { index: Int, edited_theme: ThemeData ->
                theme_manager.updateTheme(index, edited_theme)
            },
            { theme_manager.addTheme(Theme.default.copy(name = getString("theme_title_new"))) },
            { theme_manager.removeTheme(it) }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
            getString("s_key_accent_source"), null,
            3, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME     -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
                AccentColourSource.SYSTEM    -> getString("s_option_accent_system")
            }
        },

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name),
            getString("s_key_np_theme_mode"), null,
            3, false
        ) { choice ->
            when (choice) {
                0 ->    getString("s_option_np_accent_background")
                1 ->    getString("s_option_np_accent_elements")
                else -> getString("s_option_np_accent_none")
            }
        }
    )
}

private fun getLyricsCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
            getString("s_key_lyrics_follow_enabled"), getString("s_sub_lyrics_follow_enabled")
        ),

        SettingsItemSlider(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
            getString("s_key_lyrics_follow_offset"), getString("s_sub_lyrics_follow_offset"),
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name),
            getString("s_key_lyrics_default_furigana"), null
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name),
            getString("s_key_lyrics_text_alignment"), null, 3
        ) { i ->
            when (i) {
                0 ->    getString("s_option_lyrics_text_alignment_left")
                1 ->    getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_right")
            }
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_EXTRA_PADDING.name),
            getString("s_key_lyrics_extra_padding"), getString("s_sub_lyrics_extra_padding")
        )
    )
}

private fun getDownloadCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_AUTO_DOWNLOAD_ENABLED.name),
            getString("s_key_auto_download_enabled"), null
        ),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.name),
            getString("s_key_auto_download_threshold"), getString("s_sub_auto_download_threshold"),
            range = 1f .. 10f,
            min_label = "1",
            max_label = "10"
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_STREAM_AUDIO_QUALITY.name),
            getString("s_key_stream_audio_quality"), getString("s_sub_stream_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal ->   getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else ->                             getString("s_option_audio_quality_low")
            }
        },

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_DOWNLOAD_AUDIO_QUALITY.name),
            getString("s_key_download_audio_quality"), getString("s_sub_download_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal ->   getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else ->                             getString("s_option_audio_quality_low")
            }
        }
    )
}
