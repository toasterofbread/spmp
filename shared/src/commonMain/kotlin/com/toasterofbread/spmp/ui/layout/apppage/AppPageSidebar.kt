package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getThumbShape
import com.toasterofbread.spmp.ui.component.mediaitempreview.loadIfLocalPlaylist
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import kotlin.math.roundToInt

@Composable
private fun getOwnChannel(): Artist? = LocalPlayerState.current.context.ytapi.user_auth_state?.own_channel

private enum class SidebarButton {
    FEED,
    LIBRARY,
    SEARCH,
    RADIOBUILDER,
    RELOAD,
    NOTIFICATIONS,
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
            val player = LocalPlayerState.current
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
                NOTIFICATIONS -> Icons.Default.Notifications
                SETTINGS -> Icons.Default.Settings
                else -> throw IllegalStateException(name)
            },
            null
        )
    }

    @Composable
    fun shouldShow(page: AppPage?) = when (this) {
        RELOAD -> LocalPlayerState.current.app_page.canReload()
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
                NOTIFICATIONS -> Notifications
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
            NOTIFICATIONS,
            PROFILE,
            SETTINGS
        )

        val current: SidebarButton?
            @Composable get() = with (LocalPlayerState.current.app_page_state) {
                when (val page: AppPage = current_page) {
                    SongFeed -> FEED
                    Library -> LIBRARY
                    Search -> SEARCH
                    RadioBuilder -> RADIOBUILDER
                    Notifications -> NOTIFICATIONS
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
fun AppPageSidebar(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues()) {
    val player: PlayerState = LocalPlayerState.current
    val button_positions: MutableMap<SidebarButton, Float> = remember { mutableStateMapOf() }

    val button_indicator_alpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    val button_indicator_position: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

    val current_button: SidebarButton? = SidebarButton.current
    var previous_button: SidebarButton? by remember { mutableStateOf(null) }

    LaunchedEffect(current_button) {
        val button_position: Float? = button_positions[current_button]
        if (button_position == null) {
            button_indicator_alpha.animateTo(0f)
            previous_button = null
            return@LaunchedEffect
        }

        if (previous_button == null) {
            button_indicator_position.snapTo(button_position)
            button_indicator_alpha.animateTo(1f)
        }
        else {
            var jump: Boolean = false

            var in_range: Boolean = false
            for (button in SidebarButton.buttons) {
                if (button == current_button || button == previous_button) {
                    if (in_range) {
                        break
                    }
                    in_range = true
                }
                else if (in_range && button == null) {
                    jump = true
                    break
                }
            }

            if (jump) {
                button_indicator_alpha.animateTo(0f)
                button_indicator_position.snapTo(button_position)
                button_indicator_alpha.animateTo(1f)
            }
            else {
                button_indicator_position.animateTo(button_position)
            }
        }

        previous_button = current_button
    }

    Box(
        modifier
            .background(player.theme.background.amplify(0.05f))
            .padding(content_padding)
            .width(50.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .offset {
                    IntOffset(
                        0,
                        button_indicator_position.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    alpha = button_indicator_alpha.value
                }
                .background(
                    player.theme.vibrant_accent,
                    CircleShape
                )
                .size(50.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val icon_button_colours: IconButtonColors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = player.theme.on_background
                )

            for (button in SidebarButton.buttons) {
                if (button == null) {

                    Column(Modifier.fillMaxHeight().weight(1f)) {
                        Spacer(Modifier.fillMaxHeight().weight(1f))

                        if (DesktopSettings.Key.SHOW_PINNED_IN_SIDEBAR.get()) {
                            PinnedItems()
                        }
                    }

                    continue
                }

                val page: AppPage? = button.page
                AnimatedVisibility(
                    button.shouldShow(page),
                    Modifier.onGloballyPositioned {
                        button_positions[button] = it.positionInParent().y
                    }
                ) {
                    IconButton(
                        {
                            with (button) {
                                player.onButtonClicked(page)
                            }
                        },
                        colors =
                            if (button == current_button) IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = player.theme.on_accent
                            )
                            else icon_button_colours
                    ) {
                        button.ButtonContent()
                    }
                }
            }

            Spacer(Modifier.height(player.nowPlayingBottomPadding(true)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedItems(modifier: Modifier = Modifier) {
    val pinned_items: List<MediaItem> = rememberPinnedItems() ?: emptyList()

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
    ) {
        items(pinned_items) { item ->
            val long_press_menu_data: LongPressMenuData = remember(item) {
                item.getLongPressMenuData()
            }

            val loaded_item: MediaItem? = item.loadIfLocalPlaylist()
            if (loaded_item == null) {
                return@items
            }

            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .mediaItemPreviewInteraction(loaded_item, long_press_menu_data)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(item.getType().getThumbShape()),
                container_modifier = Modifier.animateItemPlacement()
            )
        }
    }
}
