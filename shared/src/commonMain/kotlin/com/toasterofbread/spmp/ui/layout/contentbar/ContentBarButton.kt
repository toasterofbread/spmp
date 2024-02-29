package com.toasterofbread.spmp.ui.layout.contentbar

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.Crossfade
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.composekit.platform.Platform
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import LocalPlayerState
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.layout.apppage.MediaItemAppPage

enum class ContentBarButton {
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
            when (this@ContentBarButton) {
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
        if (this@ContentBarButton == RELOAD) {
            app_page.onReload()
            return
        }
        openAppPage(page!!)
    }

    companion object {
        val buttons: List<ContentBarButton?> = listOf(
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

        val current: ContentBarButton?
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

        fun getButtonShortcutButton(button: ContentBarButton?, player: PlayerState): Int? {
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

private fun PlayerState.getOwnChannel(): Artist? = context.ytapi.user_auth_state?.own_channel
