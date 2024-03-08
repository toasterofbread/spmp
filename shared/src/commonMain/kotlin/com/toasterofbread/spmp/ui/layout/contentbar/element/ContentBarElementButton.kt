package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.shortcut.SHORTCUT_INDICATOR_SHAPE
import kotlinx.serialization.json.*

class ContentBarElementButton(data: JsonObject?): ContentBarElement {
    private var type: Type by mutableStateOf(
        data?.get("type")?.jsonPrimitive?.int?.let { type ->
            Type.entries[type]
        } ?: Type.DEFAULT
    )

    private fun getJsonData(): JsonObject = Json.encodeToJsonElement(
        mapOf(
            "type" to type.ordinal
        )
    ).jsonObject

    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.BUTTON, data = getJsonData())

    @Composable
    override fun isSelected(): Boolean = type == Type.current

    @Composable
    override fun Element(vertical: Boolean, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current

        IconButton({
            val page: AppPage? = type.getPage(player)
            onButtonClicked(page, player)
        }) {
            ButtonContent()

            // TODO Shortcuts
            // val shortcut_index: Int? = remember(type) { getButtonShortcutButton(type, player) }
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

    @Composable
    override fun Configuration(modifier: Modifier, onModification: () -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            item_count = Type.entries.size,
            selected = type.ordinal,
            getItem = {
                Type.entries[it].getName()
            },
            onSelected = {
                type = Type.entries[it]
                show_type_selector = false
                onModification()
            }
        )

        Column {
            Button({ show_type_selector = !show_type_selector }) {
                Text(getString("content_bar_element_builtin_config_type"))
            }
        }
    }

    @Composable
    private fun ButtonContent() {
        val player: PlayerState = LocalPlayerState.current

        if (type == Type.PROFILE) {
            val own_channel: Artist? = player.getOwnChannel()
            if (own_channel != null) {
                own_channel.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
            }
            return
        }

        if (type == Type.RELOAD) {
            Crossfade(player.app_page.isReloading()) { reloading ->
                if (reloading) {
                    SubtleLoadingIndicator()
                }
                else {
                    Icon(type.getIcon(), null)
                }
            }
            return
        }

        Icon(type.getIcon(), null)
    }

    @Composable
    fun shouldShow(page: AppPage?) = when (type) {
        Type.RELOAD -> Platform.DESKTOP.isCurrent() && LocalPlayerState.current.app_page.canReload()
        else -> page != null
    }

    private fun onButtonClicked(page: AppPage?, player: PlayerState) {
        if (type == Type.RELOAD) {
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

        fun getPage(player: PlayerState): AppPage? =
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

    companion object {
        val navigation_buttons: List<Type?> = listOf(
            Type.FEED,
            Type.LIBRARY,
            Type.SEARCH,
            Type.RADIOBUILDER,
            Type.RELOAD,
            null,
            Type.PROFILE,
            Type.CONTROL,
            Type.SETTINGS
        )

        fun getShortcutButtonPage(button_index: Int, player: PlayerState): AppPage? {
            return navigation_buttons.mapNotNull { it?.getPage(player) }.getOrNull(button_index)
        }

        fun getButtonShortcutButton(button: Type?, player: PlayerState): Int? {
            if (button == null) {
                return null
            }

            var i: Int = 0
            for (other_button in navigation_buttons) {
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
