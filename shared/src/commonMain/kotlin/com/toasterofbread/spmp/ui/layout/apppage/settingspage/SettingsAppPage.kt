@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.settings.ui.SettingsInterface
import com.toasterofbread.composekit.settings.ui.item.*
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.*
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.modifier.getHorizontal
import org.jetbrains.compose.resources.*

private const val PREFS_PAGE_EXTRA_PADDING_DP: Float = 10f

internal enum class PrefsPageScreen {
    ROOT,
    YOUTUBE_MUSIC_LOGIN,
    DISCORD_LOGIN,
    UI_DEBUG_INFO
}
enum class PrefsPageCategory {
    GENERAL,
    FILTER,
    FEED,
    PLAYER,
    LIBRARY,
    THEME,
    LYRICS,
    DOWNLOAD,
    DISCORD_STATUS,
    OTHER,
    DEVELOPMENT;

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun getIcon(filled: Boolean = false): ImageVector = when (this) {
        GENERAL -> if (filled) Icons.Filled.Settings else Icons.Outlined.Settings
        FILTER -> if (filled) Icons.Filled.FilterAlt else Icons.Outlined.FilterAlt
        FEED -> if (filled) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted
        PLAYER -> if (filled) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow
        LIBRARY -> if (filled) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic
        THEME -> if (filled) Icons.Filled.Palette else Icons.Outlined.Palette
        LYRICS -> if (filled) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        DOWNLOAD -> if (filled) Icons.Filled.Download else Icons.Outlined.Download
        DISCORD_STATUS -> resource("drawable/ic_discord.xml").readBytesSync().toImageVector(LocalDensity.current)
        OTHER -> if (filled) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
        DEVELOPMENT -> if (filled) Icons.Filled.Code else Icons.Outlined.Code
    }

    fun getTitle(): String = when (this) {
        GENERAL -> getString("s_cat_general")
        FILTER -> getString("s_cat_filter")
        FEED -> getString("s_cat_home_page")
        PLAYER -> getString("s_cat_player")
        LIBRARY -> getString("s_cat_library")
        THEME -> getString("s_cat_theming")
        LYRICS -> getString("s_cat_lyrics")
        DOWNLOAD -> getString("s_cat_download")
        DISCORD_STATUS -> getString("s_cat_discord_status")
        OTHER -> getString("s_cat_other")
        DEVELOPMENT -> getString("s_cat_development")
    }

    fun getDescription(): String = when (this) {
        GENERAL -> getString("s_cat_desc_general")
        FILTER -> getString("s_cat_desc_filter")
        FEED -> getString("s_cat_desc_home_page")
        PLAYER -> getString("s_cat_desc_player")
        LIBRARY -> getString("s_cat_desc_library")
        THEME -> getString("s_cat_desc_theming")
        LYRICS -> getString("s_cat_desc_lyrics")
        DOWNLOAD -> getString("s_cat_desc_download")
        DISCORD_STATUS -> getString("s_cat_desc_discord_status")
        OTHER -> getString("s_cat_desc_other")
        DEVELOPMENT -> ""
    }
}

class SettingsAppPage(override val state: AppPageState, footer_modifier: Modifier): AppPage() {
    private var current_category: PrefsPageCategory? by mutableStateOf(null)
    private val pill_menu: PillMenu = PillMenu(follow_player = true)
    private val ytm_auth: SettingsValueState<Set<String>> =
        SettingsValueState<Set<String>>(
            Settings.KEY_YTM_AUTH.name
        ).init(Settings.prefs, Settings.Companion::provideDefault)
    private val settings_interface: SettingsInterface =
        getPrefsPageSettingsInterface(
            state,
            pill_menu,
            ytm_auth,
            footer_modifier,
            { current_category },
            { current_category = null }
        )

    override fun onBackNavigation(): Boolean {
        if (current_category != null) {
            settings_interface.goBack()
            return true
        }
        return false
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
    @Composable
    override fun ColumnScope.SFFPage(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        val player = LocalPlayerState.current
        val category_open by remember { derivedStateOf { current_category != null } }
        val show_reset_confirmation = remember { mutableStateOf(false) }

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
            if (settings_interface.current_page.id == PrefsPageScreen.ROOT.ordinal) {
                pill_menu.addExtraAction(action = extra_action)
            }
            else {
                pill_menu.removeExtraAction(extra_action)
            }

            onDispose {
                pill_menu.removeExtraAction(extra_action)
            }
        }

        Box(modifier) {
            pill_menu.PillMenu()

            Column(Modifier.fillMaxSize().padding(content_padding.getHorizontal(PREFS_PAGE_EXTRA_PADDING_DP.dp))) {
                val top_padding: Dp = player.top_bar.MusicTopBar(
                    Settings.KEY_LYRICS_SHOW_IN_SETTINGS,
                    Modifier.fillMaxWidth().zIndex(10f),
                    getBottomBorderColour = if (current_category == null) player.theme.background_provider else null,
                    padding = PaddingValues(top = content_padding.calculateTopPadding())
                ).top_padding

                Crossfade(category_open || settings_interface.current_page.id!! != PrefsPageScreen.ROOT.ordinal) { open ->
                    if (!open) {
                        LazyColumn(
                            contentPadding = PaddingValues(top = top_padding, bottom = content_padding.calculateBottomPadding() + PREFS_PAGE_EXTRA_PADDING_DP.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        getString("s_page_preferences"),
                                        style = MaterialTheme.typography.displaySmall
                                    )

                                    val clipboard = LocalClipboardManager.current
                                    fun copyProjectUrl() {
                                        clipboard.setText(AnnotatedString(getString("project_url")))
                                        player.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", getString("project_url_name")))
                                    }

                                    Icon(
                                        painterResource("drawable/ic_github.xml"),
                                        null,
                                        Modifier.platformClickable(
                                            onClick = {
                                                if (player.context.canOpenUrl()) {
                                                    player.context.openUrl(getString("project_url"))
                                                }
                                                else {
                                                    copyProjectUrl()
                                                }
                                            },
                                            onAltClick = {
                                                if (player.context.canOpenUrl()) {
                                                    copyProjectUrl()
                                                    player.context.vibrateShort()
                                                }
                                            }
                                        )
                                    )
                                }
                            }

                            item {
                                val item = rememberYtmAuthItem(ytm_auth, true)
                                item.Item(
                                    settings_interface,
                                    settings_interface::openPageById,
                                    settings_interface::openPage
                                )
                            }

                            items(PrefsPageCategory.values()) { category ->
                                ElevatedCard(
                                    onClick = { current_category = category },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                                        contentColor = player.theme.on_background
                                    )
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

                            item {
                                val version_string: String = "v${getString("version_string")}"
                                val on_release_commit: Boolean = ProjectBuildConfig.GIT_TAG == version_string

                                Text(
                                    if (on_release_commit) {
                                        getString("info_using_release_\$x")
                                            .replace("\$x", version_string)
                                    }
                                    else {
                                        getString("info_using_non_release_commit_\$x")
                                            .replace("\$x", ProjectBuildConfig.GIT_COMMIT_HASH?.take(7).toString())
                                    },
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .alpha(0.5f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else {
                        BoxWithConstraints(
                            Modifier.pointerInput(Unit) {}
                        ) {
                            CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                                settings_interface.Interface(
                                    Modifier.fillMaxSize(),
                                    content_padding = PaddingValues(top = top_padding, bottom = content_padding.calculateBottomPadding()),
                                    titleFooter = {
                                        WaveBorder()
                                    },
                                    page_top_padding = WAVE_BORDER_HEIGHT_DP.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
