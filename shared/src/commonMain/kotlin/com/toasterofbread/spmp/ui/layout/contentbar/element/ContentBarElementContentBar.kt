package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
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
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarList
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.Serializable
import LocalPlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.content_bar_selection
import spmp.shared.generated.resources.content_bar_selection_list_built_in
import spmp.shared.generated.resources.content_bar_element_content_bar_config_bar

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
        val custom_bars: List<CustomContentBar> by player.settings.Layout.CUSTOM_BARS.observe()
        val content_bar: ContentBar? = remember(bar) { bar.getBar(custom_bars) }
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
        val custom_bars: List<CustomContentBar> by player.settings.Layout.CUSTOM_BARS.observe()
        val content_bar: ContentBar? = remember(bar) { bar.getBar(custom_bars) }
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
        val custom_bars: List<CustomContentBar> by player.settings.Layout.CUSTOM_BARS.observe()
        val content_bar: ContentBar = remember(bar) { bar.getBar(custom_bars)!! }
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
                            contentColor = player.theme.onBackground
                        ),
                        modifier = Modifier.appHover(true)
                    ) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
                title = {
                    Text(stringResource(Res.string.content_bar_selection))
                },
                text = {
                    ContentBarList(
                        internal_bars,
                        stringResource(Res.string.content_bar_selection_list_built_in)
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
                stringResource(Res.string.content_bar_element_content_bar_config_bar),
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
