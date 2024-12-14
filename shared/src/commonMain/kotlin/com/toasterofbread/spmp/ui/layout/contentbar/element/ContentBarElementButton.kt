package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Switch
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
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.components.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.appaction.NavigationAppAction
import com.toasterofbread.spmp.model.appaction.action.navigation.AppPageNavigationAction
import com.toasterofbread.spmp.model.appaction.OtherAppAction
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.content_bar_element_button_config_type
import spmp.shared.generated.resources.content_bar_element_button_config_become_close_while_target_open

@Serializable
data class ContentBarElementButton(
    val action: AppAction = AppAction.Type.NAVIGATION.createAction(),
    val become_close_while_target_open: Boolean = true,
    override val config: ContentBarElementConfig = ContentBarElementConfig()
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.BUTTON

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

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
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        if (action.hasCustomContent()) {
            action.CustomContent(onPreviewClick, modifier)
            return
        }

        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        val is_close: Boolean = onPreviewClick == null && become_close_while_target_open && isSelected()

        IconButton(
            {
                if (onPreviewClick != null) {
                    onPreviewClick()
                }
                else if (is_close) {
                    player.openAppPage(player.app_page_state.Default)
                    player.clearBackHistory()
                }
                else {
                    coroutine_scope.launch {
                        action.executeAction(player)
                    }
                }
            },
            modifier
        ) {
            Crossfade(is_close) {
                if (it) {
                    Icon(Icons.Default.Close, null)
                    return@Crossfade
                }
                else if (action is NavigationAppAction) {
                    if (action.action is AppPageNavigationAction && action.action.page == AppPage.Type.PROFILE) {
                        val own_channel_id: String? = player.getOwnChannelId()
                        if (own_channel_id != null) {
                            ArtistRef(own_channel_id).Thumbnail(ThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
                        }
                        return@Crossfade
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
                    return@Crossfade
                }

                Icon(action.getIcon(), null)
            }
        }
    }

    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            title = stringResource(Res.string.content_bar_element_button_config_type),
            isOpen = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            items = AppAction.Type.entries,
            selectedItem = action.getType(),
            itemContent = { action ->
                action.Preview()
            },
            onSelected = { _, action ->
                onModification(copy(action = action.createAction()))
                show_type_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.content_bar_element_button_config_type),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_type_selector = !show_type_selector }) {
                action.getType().Preview()
            }
        }

        AnimatedVisibility(action is NavigationAppAction, item_modifier) {
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(Res.string.content_bar_element_button_config_become_close_while_target_open),
                    Modifier.align(Alignment.CenterVertically),
                    softWrap = false
                )

                Switch(
                    become_close_while_target_open,
                    { onModification(copy(become_close_while_target_open = it)) }
                )
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
