package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueueMusic
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
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import com.toasterofbread.spmp.ui.layout.apppage.songfeedpage.SongFeedAppPage
import kotlin.math.roundToInt

@Composable
private fun getOwnChannel(): Artist? = LocalPlayerState.current.context.ytapi.user_auth_state?.own_channel

private enum class SidebarButton {
    FEED,
    LIBRARY,
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

        Icon(
            when (this) {
                FEED -> Icons.Default.QueueMusic
                LIBRARY -> Icons.Default.LibraryMusic
                NOTIFICATIONS -> Icons.Default.Notifications
                SETTINGS -> Icons.Default.Settings
                else -> throw IllegalStateException(name)
            },
            null
        )
    }

    val page: AppPage?
        @Composable
        get() = with (LocalPlayerState.current.app_page_state) {
            when (this@SidebarButton) {
                FEED -> SongFeed
                LIBRARY -> Library
                NOTIFICATIONS -> Notifications
                SETTINGS -> Settings
                PROFILE -> getOwnChannel()?.let { MediaItemAppPage(this, it) }
            }
        }

    companion object {
        val buttons: List<SidebarButton?> = listOf(
            FEED,
            LIBRARY,
            null,
            NOTIFICATIONS,
            PROFILE,
            SETTINGS
        )

        val current: SidebarButton?
            @Composable get() =
                when (val page = LocalPlayerState.current.app_page) {
                    is SongFeedAppPage -> FEED
                    is LibraryAppPage -> LIBRARY
                    is NotificationsAppPage -> NOTIFICATIONS
                    is SettingsAppPage -> SETTINGS
                    is MediaItemAppPage ->
                        if (page.item.item?.id == getOwnChannel()?.id) PROFILE
                        else null
                    else -> null
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
            .width(IntrinsicSize.Min),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val icon_button_colours: IconButtonColors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = player.theme.on_background
                )

            for (button in SidebarButton.buttons) {
                if (button == null) {
                    Spacer(Modifier.fillMaxHeight().weight(1f))
                    continue
                }

                val page: AppPage = button.page ?: continue
                IconButton(
                    {
                        player.openAppPage(page)
                    },
                    Modifier.onGloballyPositioned {
                        button_positions[button] = it.positionInParent().y
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

            Spacer(Modifier.height(player.nowPlayingBottomPadding(true)))
        }
    }
}
