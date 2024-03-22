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
import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable

@Serializable
data class ContentBarElementButton(
    val data: ButtonData = ButtonData.DEFAULT,
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.BUTTON

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    @Composable
    override fun isSelected(): Boolean = data.isSelected()

    @Composable
    override fun shouldShow(): Boolean =
        data.shouldShow(LocalPlayerState.current)

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        data.ElementContent(vertical, enable_interaction, modifier)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            item_count = ContentBarElementButton.Type.entries.size - 1 + AppPage.Type.entries.size,
            selected =
                if (data.button_type == Type.APP_PAGE) data.app_page!!.ordinal + ContentBarElementButton.Type.entries.size - 1
                else data.button_type.ordinal - 1,
            itemContent = {
                if (it < ContentBarElementButton.Type.entries.size - 1) {
                    Text(ContentBarElementButton.Type.entries[it].getName())
                }
                else {
                    Text(AppPage.Type.entries[it - ContentBarElementButton.Type.entries.size + 1].toString() + "// TODO")
                }
            },
            onSelected = {
                if (it < ContentBarElementButton.Type.entries.size - 1) {
                    onModification(copy(data = ButtonData(ContentBarElementButton.Type.entries[it])))
                }
                else {
                    val app_page: AppPage.Type = AppPage.Type.entries[it - ContentBarElementButton.Type.entries.size + 1]
                    onModification(copy(data = ButtonData(Type.APP_PAGE, app_page)))
                }

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
                Text(data.getName(), softWrap = false)
            }
        }
    }

    enum class Type {
        APP_PAGE,
        RELOAD;

        fun getName(): String =
            when (this) {
                APP_PAGE -> ""
                RELOAD -> getString("content_bar_element_button_reload")
            }
    }

    companion object {
        fun ofAppPage(page_type: AppPage.Type): ContentBarElementButton =
            ContentBarElementButton(ButtonData(Type.APP_PAGE, page_type))

        fun ofType(type: Type): ContentBarElementButton =
            ContentBarElementButton(ButtonData(type))
    }
}

@Serializable
data class ButtonData(
    val button_type: ContentBarElementButton.Type,
    val app_page: AppPage.Type? = null,
) {
    fun executeAction(player: PlayerState) {
        when (button_type) {
            ContentBarElementButton.Type.RELOAD -> player.app_page.onReload()
            ContentBarElementButton.Type.APP_PAGE -> player.openAppPage(getPage(player))
        }
    }

    fun shouldShow(player: PlayerState): Boolean =
        when (button_type) {
            ContentBarElementButton.Type.RELOAD -> Platform.DESKTOP.isCurrent() && player.app_page.canReload()
            ContentBarElementButton.Type.APP_PAGE -> getPage(player) != null
        }

    @Composable
    fun isSelected(): Boolean =
        app_page != null && app_page == getCurrentAppPageType()

    fun getName(): String =
        when (button_type) {
            ContentBarElementButton.Type.APP_PAGE -> app_page!!.getName()
            else -> button_type.getName()
        }

    fun getIcon(): ImageVector =
        when (button_type) {
            ContentBarElementButton.Type.RELOAD -> Icons.Default.Refresh
            ContentBarElementButton.Type.APP_PAGE -> app_page!!.getIcon()
        }

    private fun getPage(player: PlayerState): AppPage? =
        when (button_type) {
            ContentBarElementButton.Type.RELOAD -> null
            ContentBarElementButton.Type.APP_PAGE -> app_page!!.getPage(player, player.app_page_state)
        }

    companion object {
        val DEFAULT: ButtonData = ButtonData(ContentBarElementButton.Type.APP_PAGE, AppPage.Type.SONG_FEED)
    }
}

@Composable
private fun ButtonData.ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current

    IconButton(
        { executeAction(player) },
        modifier,
        enabled = enable_interaction
    ) {
        when (button_type) {
            ContentBarElementButton.Type.RELOAD -> {
                Crossfade(player.app_page.isReloading()) { reloading ->
                    if (reloading) {
                        SubtleLoadingIndicator()
                    }
                    else {
                        Icon(getIcon(), null)
                    }
                }
            }
            ContentBarElementButton.Type.APP_PAGE -> when (app_page!!) {
                AppPage.Type.PROFILE -> {
                    val own_channel: Artist? = player.getOwnChannel()
                    if (own_channel != null) {
                        own_channel.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(40.dp).clip(CircleShape))
                    }
                }
                else -> {
                    Icon(getIcon(), null)
                }
            }
        }
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
            is MediaItemAppPage ->
                if (page.item.item?.id == LocalPlayerState.current.getOwnChannel()?.id) AppPage.Type.PROFILE
                else null
            else -> null
        }
    }

private fun PlayerState.getOwnChannel(): Artist? = context.ytapi.user_auth_state?.own_channel
