package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRows
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PinnedItemsList
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBarTemplate
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarList
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.Serializable
import LocalPlayerState

@Serializable
data class ContentBarElementContentBar(
    override val config: ContentBarElementConfig = ContentBarElementConfig(),
    val bar: ContentBarReference = ContentBarReference.ofInternalBar(InternalContentBar.PRIMARY)
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.CONTENT_BAR

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    override fun blocksIndicatorAnimation(): Boolean = true

    override fun getContainedBars(): List<ContentBarReference> = listOf(bar)

    @Composable
    override fun isDisplaying(): Boolean {
        val player: PlayerState = LocalPlayerState.current
        val content_bar: ContentBar? = remember(bar) { bar.getBar(player.context) }
        return content_bar?.isDisplaying() == true
    }

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        if (onPreviewClick != null) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconButton(onPreviewClick) {
                    Icon(Icons.Default.TableRows, null)
                }
            }
            return
        }

        if (slot == null) {
            return
        }

        val player: PlayerState = LocalPlayerState.current
        val content_bar: ContentBar? = remember(bar) { bar.getBar(player.context) }
        content_bar?.BarContent(
            slot = slot,
            background_colour = null,
            content_padding = PaddingValues(),
            distance_to_page = 0.dp,
            modifier = modifier,
            lazy = false
        )
    }

    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val content_bar: ContentBar = remember(bar) { bar.getBar(player.context)!! }
        var show_bar_selector: Boolean by remember { mutableStateOf(false) }

        val internal_bars: List<ContentBarReference> = remember {
            InternalContentBar.ALL.map { bar ->
                ContentBarReference.ofInternalBar(bar)
            }
        }

        if (show_bar_selector) {
            AlertDialog(
                { show_bar_selector = false },
                confirmButton = {
                    Button(
                        { show_bar_selector = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = player.theme.background,
                            contentColor = player.theme.on_background
                        ),
                        modifier = Modifier.appHover(true)
                    ) {
                        Text(getString("action_cancel"))
                    }
                },
                title = {
                    Text(getString("content_bar_selection"))
                },
                text = {
                    ContentBarList(
                        internal_bars,
                        getString("content_bar_selection_list_built_in")
                    ) {
                        onModification(copy(bar = internal_bars[it]))
                        show_bar_selector = false
                    }
                }
            )
        }

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("content_bar_element_content_bar_config_bar"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_bar_selector = !show_bar_selector }) {
                Icon(content_bar.getIcon(), null)
                Text(content_bar.getName())
            }
        }
    }
}
