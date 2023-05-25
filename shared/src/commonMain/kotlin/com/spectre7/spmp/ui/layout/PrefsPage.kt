@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.spectre7.spmp.ui.layout

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.resources.getLanguageName
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.spmp.ui.theme.ThemeManager
import com.spectre7.utils.composable.WidthShrinkText
import com.spectre7.utils.modifier.background
import org.jetbrains.compose.resources.*
import kotlin.math.roundToInt

private enum class Page {
    ROOT,
    YOUTUBE_MUSIC_LOGIN,
    YOUTUBE_MUSIC_MANUAL_LOGIN,
    DISCORD_LOGIN,
    DISCORD_MANUAL_LOGIN
}
private enum class Category {
    GENERAL,
    FEED,
    THEME,
    LYRICS,
    DOWNLOAD,
    DISCORD_STATUS,
    OTHER;

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun getIcon(filled: Boolean = false): ImageVector = when (this) {
        GENERAL -> if (filled) Icons.Filled.Settings else Icons.Outlined.Settings
        FEED -> if (filled) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted
        THEME -> if (filled) Icons.Filled.Palette else Icons.Outlined.Palette
        LYRICS -> if (filled) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        DOWNLOAD -> if (filled) Icons.Filled.Download else Icons.Outlined.Download
        DISCORD_STATUS -> resource("drawable/ic_discord.xml").readBytesSync().toImageVector(LocalDensity.current)
        OTHER -> if (filled) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
    }

    fun getTitle(): String = when (this) {
        GENERAL -> getString("s_cat_general")
        FEED -> getString("s_cat_home_page")
        THEME -> getString("s_cat_theming")
        LYRICS -> getString("s_cat_lyrics")
        DOWNLOAD -> getString("s_cat_download")
        DISCORD_STATUS -> getString("s_cat_discord_status")
        OTHER -> getString("s_cat_other")
    }

    fun getDescription(): String = when (this) {
        GENERAL -> getString("s_cat_desc_general")
        FEED -> getString("s_cat_desc_home_page")
        THEME -> getString("s_cat_desc_theming")
        LYRICS -> getString("s_cat_desc_lyrics")
        DOWNLOAD -> getString("s_cat_desc_download")
        DISCORD_STATUS -> getString("s_cat_desc_discord_status")
        OTHER -> getString("s_cat_desc_other")
    }
}

@Composable
private fun ResetConfirmationDialog(show_state: MutableState<Boolean>, reset: suspend () -> Unit) {
    var do_reset: Boolean by remember { mutableStateOf(false) }
    LaunchedEffect(do_reset) {
        if (do_reset) {
            show_state.value = false
            reset()
        }
    }

    if (show_state.value) {
        PlatformAlertDialog(
            { show_state.value = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        do_reset = true
                    }
                ) {
                    Text(getString("action_confirm_action"))
                }
            },
            dismissButton = { TextButton({ show_state.value = false }) { Text(getString("action_deny_action")) } },
            title = { Text(getString("prompt_confirm_action")) },
            text = {
                Text(getString("prompt_confirm_settings_page_reset"))
            }
        )
    }
}

@Composable
private fun rememberSettingsInterface(pill_menu: PillMenu, getCategory: () -> Category?, close: () -> Unit): SettingsInterface {
    return remember {
        lateinit var settings_interface: SettingsInterface
        val pill_menu_action_overrider: @Composable PillMenu.Action.(i: Int) -> Boolean = { i ->
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
        }

        val discord_auth =
            SettingsValueState<String>(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name).init(Settings.prefs, Settings.Companion::provideDefault)

        settings_interface = SettingsInterface(
            { Theme.current },
            Page.ROOT.ordinal,
            SpMp.context,
            Settings.prefs,
            Settings.Companion::provideDefault,
            pill_menu,
            {
                when (Page.values()[it]) {
                    Page.ROOT -> SettingsPageWithItems(
                        { getCategory()?.getTitle() },
                        {
                            when (getCategory()) {
                                Category.GENERAL -> getGeneralCategory(ytm_auth)
                                Category.FEED -> getFeedCategory()
                                Category.THEME -> getThemeCategory(Theme.manager)
                                Category.LYRICS -> getLyricsCategory()
                                Category.DOWNLOAD -> getDownloadCategory()
                                Category.DISCORD_STATUS -> getDiscordStatusGroup(discord_auth)
                                Category.OTHER -> getOtherCategory()
                                null -> emptyList()
                            }
                        },
                        getIcon = {
                            val icon = getCategory()?.getIcon()
                            var current_icon by remember { mutableStateOf(icon) }

                            LaunchedEffect(icon) {
                                if (icon != null) {
                                    current_icon = icon
                                }
                            }

                            return@SettingsPageWithItems current_icon
                        }
                    )
                    Page.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth)
                    Page.YOUTUBE_MUSIC_MANUAL_LOGIN -> getYoutubeMusicLoginPage(ytm_auth, manual = true)
                    Page.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth)
                    Page.DISCORD_MANUAL_LOGIN -> getDiscordLoginPage(discord_auth, manual = true)
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
            close
        )

        return@remember settings_interface
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun PrefsPage(pill_menu: PillMenu, close: () -> Unit) {
    var current_category: Category? by remember { mutableStateOf(null) }
    val category_open by remember { derivedStateOf { current_category != null } }
    val settings_interface: SettingsInterface =
        rememberSettingsInterface(pill_menu, { current_category }, { current_category = null })
    val show_reset_confirmation = remember { mutableStateOf(false) }

    val ytm_auth = remember {
        SettingsValueState(
            Settings.KEY_YTM_AUTH.name,
            converter = { set ->
                set?.let { YoutubeMusicAuthInfo(it as Set<String>) } ?: YoutubeMusicAuthInfo()
            }
        ).init(Settings.prefs, Settings.Companion::provideDefault)
    }

    ResetConfirmationDialog(
        show_reset_confirmation,
        { 
            if (category_open) {
                settings_interface.current_page.resetKeys()
            }
            else {
                TODO("Reset keys in all categories (w/ different confirmation text)")
            }
        }
    )

    BackHandler(category_open) {
        current_category = null
    }

    val extra_action: @Composable PillMenu.Action.(action_count: Int) -> Unit = remember {{
        if (it == 1) {
            ActionButton(
                Icons.Filled.Refresh
            ) {
                show_reset_confirmation.value = true
            }
        }
    }}

    DisposableEffect(settings_interface.current_page) {
        if (settings_interface.current_page.id == Page.ROOT.ordinal) {
            pill_menu.addExtraAction(action = extra_action)
        }
        else {
            pill_menu.removeExtraAction(extra_action)
        }

        onDispose {
            pill_menu.removeExtraAction(extra_action)
        }
    }

    Crossfade(category_open || settings_interface.current_page.id!! != Page.ROOT) { open ->
        if (!open) {
            LazyColumn(
                contentPadding = PaddingValues(
                    bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp,
                    top = 20.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "設定",
                            style = MaterialTheme.typography.displaySmall
                        )

                        if (SpMp.context.canOpenUrl()) {
                            IconButton({ SpMp.context.openUrl(getString("project_url")) }) {
                                Icon(painterResource("drawable/ic_github.xml"), null)
                            }
                        }
                    }
                }

                item {
                    var own_channel = remember { mutableStateOf(ytm_auth.value.getOwnChannelOrNull()) }
                    val item = remember { 
                        getYtmAuthItem(ytm_auth, own_channel).apply { 
                            initialise(SpMp.context, Settings.prefs, Settings.Companion::provideDefault) 
                        } 
                    }
                    item.GetItem(
                        Theme.current,
                        settings_interface::openPageById,
                        settings_interface::openPage
                    )
                }

                items(Category.values()) { category ->
                    ElevatedCard(
                        onClick = { current_category = category },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            Icon(category.getIcon(), null)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(category.getTitle(), style = MaterialTheme.typography.titleMedium)
                                Text(category.getDescription(), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        else {
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
    }
}

private fun getMusicTopBarGroup(): List<SettingsItem> {
    fun MusicTopBarMode.getString(): String = when (this) {
        MusicTopBarMode.VISUALISER -> getString("s_option_topbar_mode_visualiser")
        MusicTopBarMode.LYRICS -> getString("s_option_topbar_mode_lyrics")
        MusicTopBarMode.NONE -> getString("s_option_topbar_mode_none")
    }

    return listOf(
        SettingsGroup(getString("s_group_topbar")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_LINGER.name),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        SettingsItemSlider(
            SettingsValueState<Float>(Settings.KEY_TOPBAR_VISUALISER_WIDTH.name),
            getStringTODO("Visualiser width"), null,
            getValueText = { value ->
                "${(value * 100f).roundToInt()}%"
            }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_HOME.name),
            getString("s_key_topbar_default_mode_home"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        },
        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_NOWPLAYING.name),
            getString("s_key_topbar_default_mode_nowplaying"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_IN_QUEUE.name),
            getString("s_key_topbar_show_in_queue"), null
        ),
        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_QUEUE.name),
            getString("s_key_topbar_default_mode_queue"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        }
    )
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
            warningDialog = { dismiss, openPage ->
                DiscordLoginConfirmation { proceed ->
                    dismiss()
                    if (proceed) {
                        openPage(Page.DISCORD_LOGIN.ordinal)
                    }
                }
            },
            infoDialog = { dismiss, _ ->
                DiscordLoginConfirmation(true) {
                    dismiss()
                }
            }
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

private fun getOtherCategory(): List<SettingsItem> {
    return getCachingGroup() + getAccessibilityServiceGroup() + getMusicTopBarGroup()
}

private fun getCachingGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString("s_group_caching")),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_THUMB_CACHE_ENABLED.name),
            getString("s_key_enable_thumbnail_cache"), null
        )
    )
}

private fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, manual: Boolean = false): SettingsPage {
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
            YoutubeMusicLogin(Modifier.fillMaxSize(), manual = manual) { auth_info ->
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

private fun getDiscordLoginPage(discord_auth: SettingsValueState<String>, manual: Boolean = false): SettingsPage {
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
            DiscordLogin(Modifier.fillMaxSize(), manual = manual) { auth_info ->
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

private fun getYtmAuthItem(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, own_channel: MutableState<Artist?>): SettingsItem =
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
                own_channel.value = it
            }

            own_channel.value?.PreviewLong(
                MediaItem.PreviewParams(
                    modifier
                )
            )
        },
        disabled_text = getStringTODO("Not signed in"),
        enable_button = getStringTODO("Sign in"),
        disable_button = getStringTODO("Sign out"),
        warningDialog = { dismiss, openPage ->
            YoutubeMusicLoginConfirmation { manual ->
                dismiss()
                if (manual == true) {
                    openPage(Page.YOUTUBE_MUSIC_MANUAL_LOGIN.ordinal)
                }
                else if (manual == false) {
                    openPage(Page.YOUTUBE_MUSIC_LOGIN.ordinal)
                }
            }
        },
        infoDialog = { dismiss, _ ->
            YoutubeMusicLoginConfirmation(true) {
                dismiss()
            }
        }
    ) { target, setEnabled, _, openPage ->
        if (target) {
            openPage(Page.YOUTUBE_MUSIC_LOGIN.ordinal)
        }
        else {
            setEnabled(false)
        }
    }

private fun getGeneralCategory(): List<SettingsItem> {
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
            range = 0f .. 100f
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
        )
    )
}

private fun getFeedCategory(): List<SettingsItem> {
    return listOf(
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
            { theme_manager.addTheme(Theme.current.theme_data.copy(name = getString("theme_title_new")), it) },
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
