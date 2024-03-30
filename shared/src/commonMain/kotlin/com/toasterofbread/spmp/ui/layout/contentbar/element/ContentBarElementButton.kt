package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.appaction.NavigationAppAction
import com.toasterofbread.spmp.model.appaction.action.navigation.AppPageNavigationAction
import com.toasterofbread.spmp.model.appaction.OtherAppAction
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

@Serializable
data class ContentBarElementButton(
    val action: AppAction = AppAction.Type.NAVIGATION.createAction(),
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.BUTTON

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    @Composable
    override fun isSelected(): Boolean {
        if (action is NavigationAppAction && action.action is AppPageNavigationAction) {
            return action.action.page == getCurrentAppPageType()
        }
        return false
    }

    @Composable
    override fun shouldShow(): Boolean =
        when (action) {
            is NavigationAppAction ->
                if (action.action is AppPageNavigationAction)
                    action.action.page.getPage(LocalPlayerState.current, LocalPlayerState.current.app_page_state) != null
                else true
            is OtherAppAction ->
                if (action.action == OtherAppAction.Action.RELOAD_PAGE)
                    Platform.DESKTOP.isCurrent() && LocalPlayerState.current.app_page.canReload()
                else true
            else -> true
        }

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        if (action.hasCustomContent()) {
            action.CustomContent(enable_interaction, modifier)
            return
        }

        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        val colours: IconButtonColors =
            IconButtonDefaults.iconButtonColors(
                disabledContentColor = LocalContentColor.current
            )

        IconButton(
            {
                coroutine_scope.launch {
                    action.executeAction(player)
                }
            },
            modifier,
            enabled = enable_interaction,
            colors = colours
        ) {
            if (action is NavigationAppAction) {
                if (action.action is AppPageNavigationAction && action.action.page == AppPage.Type.PROFILE) {
                    val own_channel_id: String? = player.getOwnChannelId()
                    if (own_channel_id != null) {
                        ArtistRef(own_channel_id).Thumbnail(ThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
                    }
                    return@IconButton
                }
            }
            else if (action is OtherAppAction && action.action == OtherAppAction.Action.RELOAD_PAGE) {
                Crossfade(player.app_page.isReloading()) { reloading ->
                    if (reloading) {
                        SubtleLoadingIndicator()
                    }
                    else {
                        Icon(action.action.getIcon(), null)
                    }
                }
                return@IconButton
            }

            Icon(action.getIcon(), null)
        }
    }

    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            item_count = AppAction.Type.entries.size,
            selected = action.getType().ordinal,
            itemContent = {
                AppAction.Type.entries[it].Preview()
            },
            onSelected = {
                onModification(copy(action = AppAction.Type.entries[it].createAction()))
                show_type_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("content_bar_element_button_config_type"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_type_selector = !show_type_selector }) {
                action.getType().Preview()
            }
        }

        action.ConfigurationItems(item_modifier) {
            onModification(copy(action = it))
        }
    }

    companion object {
        fun ofAppPage(page_type: AppPage.Type): ContentBarElementButton =
            ContentBarElementButton(NavigationAppAction(AppPageNavigationAction(page_type)))
    }
}

@Composable
private fun getCurrentAppPageType(): AppPage.Type? =
    with (LocalPlayerState.current.app_page_state) {
        when (val page: AppPage = current_page) {
            SongFeed -> AppPage.Type.SONG_FEED
            Library -> AppPage.Type.LIBRARY
            Search -> AppPage.Type.SEARCH
            RadioBuilder -> AppPage.Type.RADIO_BUILDER
            ControlPanel -> AppPage.Type.CONTROL_PANEL
            Settings -> AppPage.Type.SETTINGS
            is ArtistAppPage ->
                if (page.item?.id == LocalPlayerState.current.getOwnChannelId()) AppPage.Type.PROFILE
                else null
            else -> null
        }
    }

private fun PlayerState.getOwnChannelId(): String? = context.ytapi.user_auth_state?.own_channel_id
