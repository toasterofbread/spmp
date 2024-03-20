package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.shortcut.SHORTCUT_INDICATOR_SHAPE
import com.toasterofbread.spmp.ui.shortcut.trigger.ShortcutTrigger
import com.toasterofbread.spmp.ui.shortcut.ShortcutSelector
import com.toasterofbread.spmp.ui.shortcut.Shortcut
import com.toasterofbread.spmp.ui.shortcut.action.ContentBarButtonShortcutAction
import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class ContentBarElementButton(data: ContentBarElementData): ContentBarElement(data) {
    private var button_type: Type by mutableStateOf(
        data.data?.get("button_type")?.jsonPrimitive?.int?.let {
            Type.entries[it]
        } ?: Type.DEFAULT
    )

    private var shortcut_trigger: ShortcutTrigger? by mutableStateOf(
        data.data?.get("shortcut_trigger")?.let { Json.decodeFromJsonElement(it) }
    )

    constructor(button_type: Type): this(
        ContentBarElementData(
            ContentBarElement.Type.BUTTON,
            data = buildJsonObject {
                put("button_type", button_type.ordinal)
            }
        )
    )

    override fun getSubData(): JsonObject = buildJsonObject {
        put("button_type", button_type.ordinal)
        put("shortcut_trigger", Json.encodeToJsonElement(shortcut_trigger))
    }

    override fun getShortcut(): Shortcut? =
        shortcut_trigger?.let { trigger ->
            Shortcut(trigger, ContentBarButtonShortcutAction(button_type))
        }

    @Composable
    override fun isSelected(): Boolean = button_type == Type.current

    @Composable
    override fun shouldShow(): Boolean =
        button_type.shouldShow(LocalPlayerState.current)

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current

        IconButton(
            {
                button_type.executeAction(player)
            },
            modifier
        ) {
            if (button_type == Type.PROFILE) {
                val own_channel: Artist? = player.getOwnChannel()
                if (own_channel != null) {
                    own_channel.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
                }
                return@IconButton
            }

            if (button_type == Type.RELOAD) {
                Crossfade(player.app_page.isReloading()) { reloading ->
                    if (reloading) {
                        SubtleLoadingIndicator()
                    }
                    else {
                        Icon(button_type.getIcon(), null)
                    }
                }
                return@IconButton
            }

            Icon(button_type.getIcon(), null)

            // TODO Shortcuts
            // val shortcut_index: Int? = remember(button_type) { getButtonShortcutButton(button_type, player) }
            // if (shortcut_index == null) {
            //     return@IconButton
            // }

            // val show_shortcut_indicator: Boolean by remember(shortcut_index) { derivedStateOf {
            //     showing_shortcut_indices > shortcut_index
            // } }

            // AnimatedVisibility(
            //     show_shortcut_indicator,
            //     Modifier.offset(17.dp, 17.dp).zIndex(1f),
            //     enter = fadeIn(),
            //     exit = fadeOut()
            // ) {
            //     val indicator_colour: Color =
            //         if (button == current_button) player.theme.on_accent
            //         else player.theme.accent

            //     Box(
            //         Modifier.size(20.dp).background(indicator_colour, SHORTCUT_INDICATOR_SHAPE),
            //         contentAlignment = Alignment.Center
            //     ) {
            //         Text(
            //             (shortcut_index + 1).toString(),
            //             Modifier.offset(y = (-5).dp),
            //             fontSize = 10.sp,
            //             color = indicator_colour.getContrasted()
            //         )
            //     }
            // }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: () -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            item_count = Type.entries.size,
            selected = button_type.ordinal,
            itemContent = {
                Text(Type.entries[it].getName())
            },
            onSelected = {
                button_type = Type.entries[it]
                show_type_selector = false
                onModification()
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
                Text(button_type.getName(), softWrap = false)
            }
        }

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("content_bar_element_button_config_shortcut"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            ShortcutSelector(shortcut_trigger) { new_shortcut ->
                shortcut_trigger = new_shortcut
                onModification()
            }
        }

        shortcut_trigger?.ConfigurationItems(item_modifier.padding(start = 50.dp)) { new_shortcut ->
            shortcut_trigger = new_shortcut
            onModification()
        }
    }

    private fun onButtonClicked(page: AppPage?, player: PlayerState) {
        if (button_type == Type.RELOAD) {
            player.app_page.onReload()
            return
        }
        player.openAppPage(page!!)
    }

    enum class Type {
        FEED,
        LIBRARY,
        SEARCH,
        RADIOBUILDER,
        RELOAD,
        CONTROL,
        SETTINGS,
        PROFILE;

        fun executeAction(player: PlayerState) {
            if (this == Type.RELOAD) {
                player.app_page.onReload()
                return
            }
            player.openAppPage(getPage(player))
        }

        fun shouldShow(player: PlayerState): Boolean =
            when (this) {
                RELOAD -> Platform.DESKTOP.isCurrent() && player.app_page.canReload()
                else -> getPage(player) != null
            }

        fun getName(): String =
            when (this) {
                FEED -> getString("content_bar_element_button_feed")
                LIBRARY -> getString("content_bar_element_button_library")
                SEARCH -> getString("content_bar_element_button_search")
                RADIOBUILDER -> getString("content_bar_element_button_radiobuilder")
                RELOAD -> getString("content_bar_element_button_reload")
                CONTROL -> getString("content_bar_element_button_control")
                SETTINGS -> getString("content_bar_element_button_settings")
                PROFILE -> getString("content_bar_element_button_profile")
            }

        fun getIcon(): ImageVector =
            when (this) {
                Type.FEED -> Icons.Default.QueueMusic
                Type.LIBRARY -> Icons.Default.LibraryMusic
                Type.SEARCH -> Icons.Default.Search
                Type.RADIOBUILDER -> Icons.Default.Radio
                Type.RELOAD -> Icons.Default.Refresh
                Type.CONTROL -> Icons.Default.Dns
                Type.SETTINGS -> Icons.Default.Settings
                Type.PROFILE -> Icons.Default.Person
            }

        private fun getPage(player: PlayerState): AppPage? =
            with (player.app_page_state) {
                when (this@Type) {
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

        companion object {
            val DEFAULT: Type = SETTINGS

            val current: Type?
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
        }
    }
}

private fun PlayerState.getOwnChannel(): Artist? = context.ytapi.user_auth_state?.own_channel
