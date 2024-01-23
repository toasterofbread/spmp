package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.composable.SidebarButtonSelector
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getThumbShape
import com.toasterofbread.spmp.ui.component.mediaitempreview.loadIfLocalPlaylist
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage

@Composable
private fun getOwnChannel(): Artist? = LocalPlayerState.current.context.ytapi.user_auth_state?.own_channel

private enum class SidebarButton {
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
        if (this == PROFILE) {
            val own_channel: Artist? = getOwnChannel()
            if (own_channel != null) {
                own_channel.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
            }
            return
        }

        if (this == RELOAD) {
            val player: PlayerState = LocalPlayerState.current
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

    val page: AppPage?
        @Composable
        get() = with (LocalPlayerState.current.app_page_state) {
            when (this@SidebarButton) {
                FEED -> SongFeed
                LIBRARY -> Library
                SEARCH -> Search
                RADIOBUILDER -> RadioBuilder
                RELOAD -> null
                CONTROL -> ControlPanel
                SETTINGS -> Settings
                PROFILE -> getOwnChannel()?.let { ArtistAppPage(this, it) }
            }
        }

    fun PlayerState.onButtonClicked(page: AppPage?) {
        if (this@SidebarButton == RELOAD) {
            app_page.onReload()
            return
        }
        openAppPage(page!!)
    }

    companion object {
        val buttons: List<SidebarButton?> = listOf(
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

        val current: SidebarButton?
            @Composable get() = with (LocalPlayerState.current.app_page_state) {
                when (val page: AppPage = current_page) {
                    SongFeed -> FEED
                    Library -> LIBRARY
                    Search -> SEARCH
                    RadioBuilder -> RADIOBUILDER
                    ControlPanel -> CONTROL
                    Settings -> SETTINGS
                    is MediaItemAppPage ->
                        if (page.item.item?.id == getOwnChannel()?.id) PROFILE
                        else null
                    else -> null
                }
            }
    }
}

@Composable
fun AppPageSidebar(
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current

    val current_button: SidebarButton? = SidebarButton.current
    val pages: List<AppPage?> = SidebarButton.entries.map { it.page }

    SidebarButtonSelector(
        modifier = modifier
            .background(player.theme.background.amplify(0.05f))
            .padding(content_padding)
            .width(50.dp),
        selected_button = current_button,
        buttons = SidebarButton.buttons,
        indicator_colour = player.theme.vibrant_accent,
        onButtonSelected = { button ->
            if (button == null) {
                return@SidebarButtonSelector
            }

            val page: AppPage? = pages[button.ordinal]
            with (button) {
                player.onButtonClicked(page)
            }
        },
        isSpacing = {
            it == null
        },
        bottom_padding = player.nowPlayingBottomPadding(true),
        vertical_arrangement = Arrangement.spacedBy(1.dp),
        showButton = { button ->
            if (button == null) {
                return@SidebarButtonSelector false
            }

            val page: AppPage? = pages[button.ordinal]
            return@SidebarButtonSelector button.shouldShow(page)
        },
        extraContent = { button ->
            if (button == null) {
                Column(Modifier.fillMaxHeight().weight(1f).padding(vertical = 10.dp)) {
                    Spacer(Modifier.fillMaxHeight().weight(1f))
                    PinnedItems(multiselect_context = multiselect_context)
                }
            }
        }
    ) { button ->
        val colour: Color =
            if (button == current_button) player.theme.on_accent
            else player.theme.on_background

        CompositionLocalProvider(LocalContentColor provides colour) {
            button?.ButtonContent()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedItems(modifier: Modifier = Modifier, multiselect_context: MediaItemMultiSelectContext? = null) {
    val pinned_items: List<MediaItem> = rememberPinnedItems() ?: emptyList()

    Column(modifier) {
        multiselect_context?.CollectionToggleButton(pinned_items, enter = expandVertically(), exit = shrinkVertically())

        LazyColumn(
            modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
        ) {
            items(pinned_items.reversed()) { item ->
                val long_press_menu_data: LongPressMenuData = remember(item) {
                    item.getLongPressMenuData(multiselect_context)
                }

                val loaded_item: MediaItem? = item.loadIfLocalPlaylist()
                if (loaded_item == null) {
                    return@items
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(item.getType().getThumbShape())
                        .animateItemPlacement()
                ) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        Modifier
                            .mediaItemPreviewInteraction(loaded_item, long_press_menu_data)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )

                    multiselect_context?.also { ctx ->
                        ctx.SelectableItemOverlay(
                            loaded_item,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            key = long_press_menu_data.multiselect_key
                        )
                    }
                }
            }
        }
    }
}
