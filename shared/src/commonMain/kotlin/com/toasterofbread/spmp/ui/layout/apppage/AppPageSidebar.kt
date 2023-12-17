package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.toFloat
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getThumbShape
import com.toasterofbread.spmp.ui.component.mediaitempreview.loadIfLocalPlaylist
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
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

    val button_positions: MutableMap<SidebarButton, Float> = remember { mutableStateMapOf() }

    val button_indicator_alpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    val button_indicator_position: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

    val current_button: SidebarButton? = SidebarButton.current
    var previous_button: SidebarButton? by remember { mutableStateOf(null) }

    var running: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(current_button) {
        val button_position: Float? = button_positions[current_button]
        if (button_position == null) {
            button_indicator_alpha.animateTo(0f)
            previous_button = null
            running = false
            return@LaunchedEffect
        }

        running = true

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

        running = false
    }

    BoxWithConstraints(
        modifier
            .background(player.theme.background.amplify(0.05f))
            .padding(content_padding)
            .width(50.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.verticalScroll(rememberScrollState())) {
            CurrentButtonIndicator(
                Modifier
                    .offset {
                        IntOffset(
                            0,
                            button_indicator_position.value.roundToInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = if (!running) 0f else button_indicator_alpha.value
                    }
            )

            Column(
                Modifier.heightIn(min = this@BoxWithConstraints.maxHeight),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val icon_button_colours: IconButtonColors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = player.theme.on_background
                    )

                for (button in SidebarButton.buttons) {
                    if (button == null) {
                        Column(Modifier.fillMaxHeight().weight(1f).padding(vertical = 10.dp)) {
                            Spacer(Modifier.fillMaxHeight().weight(1f))
                            PinnedItems(multiselect_context = multiselect_context)
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
                        Box(Modifier.requiredSize(0.dp)) {
                            CurrentButtonIndicator(
                                Modifier
                                    .offset(25.dp, 25.dp)
                                    .graphicsLayer { alpha = (button == previous_button && !running).toFloat() }
                            )
                        }

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

@Composable
private fun CurrentButtonIndicator(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    Box(
        modifier
            .background(
                player.theme.vibrant_accent,
                CircleShape
            )
            .requiredSize(50.dp)
    )
}
