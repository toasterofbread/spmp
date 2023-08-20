@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.prefspage

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.item.*
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.blendWith
import com.toasterofbread.utils.modifier.background
import org.jetbrains.compose.resources.*

internal enum class PrefsPageScreen {
    ROOT,
    YOUTUBE_MUSIC_LOGIN,
    YOUTUBE_MUSIC_MANUAL_LOGIN,
    DISCORD_LOGIN,
    DISCORD_MANUAL_LOGIN
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun PrefsPage(
    bottom_padding: Dp,
    category_state: MutableState<PrefsPageCategory?>,
    pill_menu: PillMenu,
    settings_interface: SettingsInterface,
    ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>,
    modifier: Modifier = Modifier,
    close: () -> Unit,
) {
    var current_category by category_state
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
        Column(Modifier.fillMaxSize()) {
            MusicTopBar(
                Settings.KEY_LYRICS_SHOW_IN_SETTINGS,
                Modifier.fillMaxWidth().zIndex(10f),
                getBottomBorderColour = if (current_category == null) Theme.background_provider else null
            )
    
            Crossfade(category_open || settings_interface.current_page.id!! != PrefsPageScreen.ROOT.ordinal) { open ->
                if (!open) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = bottom_padding,
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
                                    getString("s_page_preferences"),
                                    style = MaterialTheme.typography.displaySmall
                                )
    
                                val clipboard = LocalClipboardManager.current
                                fun copyProjectUrl() {
                                    clipboard.setText(AnnotatedString(getString("project_url")))
                                    SpMp.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", getString("project_url_name")))
                                }
    
                                Icon(
                                    painterResource("drawable/ic_github.xml"), 
                                    null,
                                    Modifier.platformClickable(
                                        onClick = {
                                            if (SpMp.context.canOpenUrl()) {
                                                SpMp.context.openUrl(getString("project_url"))
                                            }
                                            else {
                                                copyProjectUrl()
                                            }
                                        },
                                        onAltClick = {
                                            if (SpMp.context.canOpenUrl()) {
                                                copyProjectUrl()
                                                SpMp.context.vibrateShort()
                                            }
                                        }
                                    )
                                )
                            }
                        }
    
                        item {
                            val item = rememberYtmAuthItem(ytm_auth, true)
                            item.GetItem(
                                Theme,
                                settings_interface::openPageById,
                                settings_interface::openPage
                            )
                        }
    
                        items(PrefsPageCategory.values()) { category ->
                            ElevatedCard(
                                onClick = { current_category = category },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Theme.accent.blendWith(Theme.background, 0.05f),
                                    contentColor = Theme.on_background
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
                    }
                }
                else {
                    BoxWithConstraints(
                        Modifier
                            .background(Theme.background_provider)
                            .pointerInput(Unit) {}
                    ) {
                        CompositionLocalProvider(LocalContentColor provides Theme.on_background) {
                            settings_interface.Interface(
                                Modifier.fillMaxSize(),
                                content_padding = PaddingValues(bottom = bottom_padding)
                            )
                        }
                    }
                }
            }
        }
    }
}
