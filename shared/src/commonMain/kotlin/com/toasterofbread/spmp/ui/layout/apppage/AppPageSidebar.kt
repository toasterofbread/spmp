package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.spmp.ui.shortcut.*
import kotlin.math.*
import kotlinx.coroutines.delay

private fun PlayerState.getOwnChannel(): Artist? = context.ytapi.user_auth_state?.own_channel

enum class AppPageSidebarButton {
    FEED,
    LIBRARY,
    SEARCH,
    RADIOBUILDER,
    RELOAD,
    CONTROL,
    SETTINGS,
    PROFILE;

    @Composable
    fun ButtonContent() {
        val player: PlayerState = LocalPlayerState.current

        if (this == PROFILE) {
            val own_channel: Artist? = player.getOwnChannel()
            if (own_channel != null) {
                own_channel.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
            }
            return
        }

        if (this == RELOAD) {
            Crossfade(player.app_page.isReloading()) { reloading ->
                if (reloading) {
                    SubtleLoadingIndicator()
                }
                else {
                    Icon(Icons.Default.Refresh, null)
                }
            }
            return
        }

        Icon(
            when (this) {
                FEED -> Icons.Default.QueueMusic
                LIBRARY -> Icons.Default.LibraryMusic
                SEARCH -> Icons.Default.Search
                RADIOBUILDER -> Icons.Default.Radio
                CONTROL -> Icons.Default.Dns
                SETTINGS -> Icons.Default.Settings
                else -> throw IllegalStateException(name)
            },
            null
        )
    }

    @Composable
    fun shouldShow(page: AppPage?) = when (this) {
        RELOAD -> Platform.DESKTOP.isCurrent() && LocalPlayerState.current.app_page.canReload()
        else -> page != null
    }

    fun getPage(player: PlayerState): AppPage? =
        with (player.app_page_state) {
            when (this@AppPageSidebarButton) {
                FEED -> SongFeed
                LIBRARY -> Library
                SEARCH -> Search
                RADIOBUILDER -> RadioBuilder
                RELOAD -> null
                CONTROL -> ControlPanel
                SETTINGS -> Settings
                PROFILE -> player.getOwnChannel()?.let { ArtistAppPage(this, it) }
            }
        }

    fun PlayerState.onButtonClicked(page: AppPage?) {
        if (this@AppPageSidebarButton == RELOAD) {
            app_page.onReload()
            return
        }
        openAppPage(page!!)
    }

    companion object {
        val buttons: List<AppPageSidebarButton?> = listOf(
            FEED,
            LIBRARY,
            SEARCH,
            RADIOBUILDER,
            RELOAD,
            null,
            PROFILE,
            CONTROL,
            SETTINGS
        )

        val current: AppPageSidebarButton?
            @Composable get() = with (LocalPlayerState.current.app_page_state) {
                when (val page: AppPage = current_page) {
                    SongFeed -> FEED
                    Library -> LIBRARY
                    Search -> SEARCH
                    RadioBuilder -> RADIOBUILDER
                    ControlPanel -> CONTROL
                    Settings -> SETTINGS
                    is MediaItemAppPage ->
                        if (page.item.item?.id == LocalPlayerState.current.getOwnChannel()?.id) PROFILE
                        else null
                    else -> null
                }
            }

        fun getShortcutButtonPage(button_index: Int, player: PlayerState): AppPage? {
            return buttons.mapNotNull { it?.getPage(player) }.getOrNull(button_index)
        }

        fun getButtonShortcutButton(button: AppPageSidebarButton?, player: PlayerState): Int? {
            if (button == null) {
                return null
            }

            var i: Int = 0
            for (other_button in buttons) {
                if (other_button?.getPage(player) == null) {
                    continue
                }

                if (other_button == button) {
                    return i
                }

                i++
            }
            return null
        }
    }
}

@Composable
fun AppPageSidebar(
    slot: LayoutSlot,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current

    val current_button: AppPageSidebarButton? = AppPageSidebarButton.current

    val pressed_shortcut_modifiers: List<ShortcutModifier> = LocalPressedShortcutModifiers.current.modifiers
    val has_all_shortcut_modifiers: Boolean = pressed_shortcut_modifiers.containsAll(ShortcutGroup.SIDEBAR_NAVIGATION.modifiers)
    var showing_shortcut_indices: Int by remember { mutableStateOf(0) }

    LaunchedEffect(has_all_shortcut_modifiers) {
        if (has_all_shortcut_modifiers && showing_shortcut_indices == 0) {
            delay(SHORTCUT_INDICATOR_SHOW_DELAY_MS)
        }

        val period: Long = ((1f / AppPageSidebarButton.entries.size) * SHORTCUT_INDICATOR_GROUP_ANIM_DURATION_MS).roundToLong()
        val target: Int = if (has_all_shortcut_modifiers) AppPageSidebarButton.entries.size else 0

        for (i in 0 until (showing_shortcut_indices - target).absoluteValue) {
            if (has_all_shortcut_modifiers) showing_shortcut_indices++
            else showing_shortcut_indices--

            delay(period)
        }
    }

    SidebarButtonSelector(
        vertical = slot.is_vertical,
        modifier = modifier
            .padding(content_padding)
            .then(
                if (slot.is_vertical) Modifier.width(50.dp)
                else Modifier.height(50.dp)
            ),
        selected_button = AppPageSidebarButton.buttons.indexOf(current_button).takeIf { it != -1 },
        buttons = AppPageSidebarButton.buttons,
        indicator_colour = player.theme.vibrant_accent,
        isSpacing = {
            it == null
        },
        arrangement = Arrangement.spacedBy(1.dp),
        showButton = { button ->
            if (button == null) {
                return@SidebarButtonSelector false
            }

            val page: AppPage? = AppPageSidebarButton.entries[button.ordinal].getPage(player)
            return@SidebarButtonSelector button.shouldShow(page)
        },
        extraContent = { _, button ->
            if (button == null) {
                val fill_modifier: Modifier =
                    if (slot.is_vertical) Modifier.fillMaxHeight()
                    else Modifier.fillMaxWidth()

                Column(
                    fill_modifier
                        .weight(1f)
                        .then(
                            if (slot.is_vertical) Modifier.padding(vertical = 10.dp)
                            else Modifier.padding(horizontal = 10.dp)
                        )
                ) {
                    Spacer(fill_modifier.weight(1f))
                    PinnedItems(slot.is_vertical, multiselect_context = multiselect_context)
                }
            }
        }
    ) { _, button ->
        val colour: Color =
            if (button == current_button) player.theme.on_accent
            else LocalContentColor.current

        CompositionLocalProvider(LocalContentColor provides colour) {
            IconButton({
                if (button == null) {
                    return@IconButton
                }

                val page: AppPage? = AppPageSidebarButton.entries[button.ordinal].getPage(player)
                with (button) {
                    player.onButtonClicked(page)
                }
            }) {
                button?.ButtonContent()

                val shortcut_index: Int? = remember(button, player) { AppPageSidebarButton.getButtonShortcutButton(button, player) }
                if (shortcut_index == null) {
                    return@IconButton
                }

                val show_shortcut_indicator: Boolean by remember(shortcut_index) { derivedStateOf { showing_shortcut_indices > shortcut_index } }

                AnimatedVisibility(
                    show_shortcut_indicator,
                    Modifier.offset(17.dp, 17.dp).zIndex(1f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val indicator_colour: Color =
                        if (button == current_button) player.theme.on_accent
                        else player.theme.accent

                    Box(
                        Modifier.size(20.dp).background(indicator_colour, SHORTCUT_INDICATOR_SHAPE),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (shortcut_index + 1).toString(),
                            Modifier.offset(y = (-5).dp),
                            fontSize = 10.sp,
                            color = indicator_colour.getContrasted()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedItems(
    vertical: Boolean,
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val pinned_items: List<MediaItem> = rememberPinnedItems() ?: emptyList()

    RowOrColumn(!vertical, modifier) {
        multiselect_context?.CollectionToggleButton(pinned_items, enter = expandVertically(), exit = shrinkVertically())

        ScrollBarLazyRowOrColumn(
            !vertical,
            modifier,
            arrangement = Arrangement.spacedBy(10.dp),
            alignment = -1,
            show_scrollbar = false
        ) {
            items(pinned_items.reversed()) { item ->
                val long_press_menu_data: LongPressMenuData = remember(item) {
                    item.getLongPressMenuData(multiselect_context)
                }

                val loaded_item: MediaItem? = item.loadIfLocalPlaylist()
                if (loaded_item == null) {
                    return@items
                }

                val fill_modifier: Modifier =
                    Modifier
                        .then(
                            if (vertical) Modifier.fillMaxWidth()
                            else Modifier.fillMaxHeight()
                        )
                        .aspectRatio(1f)

                Box(
                    fill_modifier
                        .clip(item.getType().getThumbShape())
                        .animateItemPlacement()
                ) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        fill_modifier.mediaItemPreviewInteraction(loaded_item, long_press_menu_data)
                    )

                    multiselect_context?.also { ctx ->
                        ctx.SelectableItemOverlay(
                            loaded_item,
                            fill_modifier,
                            key = long_press_menu_data.multiselect_key
                        )
                    }
                }
            }
        }
    }
}
