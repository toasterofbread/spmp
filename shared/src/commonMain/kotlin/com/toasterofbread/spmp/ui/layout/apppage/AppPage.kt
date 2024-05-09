package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import dev.toastbits.composekit.platform.composable.ScrollBarLazyRow
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.resources.getString
import dev.toastbits.composekit.utils.composable.ScrollBarLazyRowOrColumn

abstract class AppPageWithItem : AppPage() {
    abstract val item: MediaItemHolder
}

abstract class AppPage {
    abstract val state: AppPageState

    @Composable
    abstract fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    )

    @Composable
    open fun shouldShowPrimaryBarContent(): Boolean = false

    @Composable
    open fun PrimaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean = false

    @Composable
    open fun shouldShowSecondaryBarContent(): Boolean = false

    @Composable
    open fun SecondaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean = false

    open fun onOpened(from_item: MediaItemHolder? = null) {}
    open fun onReopened() {}
    open fun onClosed(next_page: AppPage?) {}
    open fun onBackNavigation(): Boolean = false

    open fun canReload(): Boolean = false
    open fun onReload() {}

    @Composable
    open fun isReloading(): Boolean = false

    enum class Type {
        SONG_FEED,
        LIBRARY,
        SEARCH,
        RADIO_BUILDER,
        CONTROL_PANEL,
        SETTINGS,
        PROFILE;

        companion object {
            val DEFAULT: Type = SONG_FEED
        }

        fun getName(): String =
            when (this) {
                SONG_FEED -> getString("app_page_song_feed")
                LIBRARY -> getString("app_page_library")
                SEARCH -> getString("app_page_search")
                RADIO_BUILDER -> getString("app_page_radio_builder")
                CONTROL_PANEL -> getString("app_page_control_panel")
                SETTINGS -> getString("app_page_settings")
                PROFILE -> getString("app_page_profile")
            }

        fun getIcon(): ImageVector =
            when (this) {
                SONG_FEED -> Icons.Default.QueueMusic
                LIBRARY -> Icons.Default.LibraryMusic
                SEARCH -> Icons.Default.Search
                RADIO_BUILDER -> Icons.Default.Radio
                CONTROL_PANEL -> Icons.Default.Dns
                SETTINGS -> Icons.Default.Settings
                PROFILE -> Icons.Default.Person
            }

        fun getPage(player: PlayerState, state: AppPageState): AppPage? =
            when (this) {
                SONG_FEED -> state.SongFeed
                LIBRARY -> state.Library
                SEARCH -> state.Search
                RADIO_BUILDER -> state.RadioBuilder
                CONTROL_PANEL -> state.ControlPanel
                SETTINGS -> state.Settings
                PROFILE -> player.getOwnChannel()?.let { ArtistAppPage(state, it) }
            }
    }
}

private fun PlayerState.getOwnChannel(): Artist? = context.ytapi.user_auth_state?.own_channel_id?.let { ArtistRef(it) }
