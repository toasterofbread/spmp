package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.ui.layout.contentbar.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarElementSelector
import com.toasterofbread.spmp.ui.theme.appHover
import dev.toastbits.composekit.platform.composable.platformClickable
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.utils.composable.AlignableCrossfade

@Serializable
data class ContentBarElementCrossfade(
    override val config: ContentBarElementConfig = ContentBarElementConfig(),
    val elements: List<ContentBarElement> = listOf()
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.CROSSFADE

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun isDisplaying(): Boolean {
        return elements.any { it.isDisplaying() }
    }

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        if (onPreviewClick != null) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconButton(onPreviewClick) {
                    Icon(Icons.Default.Shuffle, null)
                }
            }
            return
        }

        if (slot == null) {
            return
        }

        var current_element_index: Int by remember { mutableIntStateOf(0) }

        Crossfade(
            (current_element_index until elements.size).firstOrNull { elements.getOrNull(it)?.isDisplaying() == true }?.let { elements[it] },
            modifier =
                Modifier
                    .fillMaxSize()
        ) { element ->
            element?.Element(
                vertical,
                slot,
                bar_size,
                modifier
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .platformClickable(onClick = {
                        if (current_element_index + 1 >= elements.size) {
                            current_element_index = 0
                        }
                        else {
                            current_element_index++
                        }
                    }
                )
            )
        }
    }

    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        var show_element_selector: Boolean by remember { mutableStateOf(false) }

        val internal_bars: List<ContentBarReference> = remember {
            InternalContentBar.ALL.map { bar ->
                ContentBarReference.ofInternalBar(bar)
            } + CustomContentBarTemplate.entries.map { template ->
                ContentBarReference.ofTemplate(template)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                getString("content_bar_element_content_bar_config_bar"),
                softWrap = false
            )

            val indent: Dp = 50.dp

            for ((index, element) in elements.withIndex()) {
                Row(Modifier.padding(start = indent)) {
                    val type: ContentBarElement.Type = element.getType()
                    Icon(type.getIcon(), null)
                    Text(type.getName(), Modifier.fillMaxWidth().weight(1f))

                    IconButton({
                        onModification(copy(elements = elements.toMutableList().apply { removeAt(index) }))
                    }) {
                        Icon(Icons.Default.Remove, null)
                    }
                }
            }

            AlignableCrossfade(
                show_element_selector,
                modifier = Modifier.fillMaxWidth().padding(start = indent),
                contentAlignment = Alignment.TopCenter
            ) { selecting ->
                if (selecting) {
                    ContentBarElementSelector(
                        button_colours =
                            ButtonDefaults.buttonColors(
                                containerColor = player.theme.on_background,
                                contentColor = player.theme.background
                            )
                    ) { type ->
                        onModification(copy(elements = elements + listOf(type.createElement())))
                        show_element_selector = false
                    }
                }
                else {
                    IconButton({ show_element_selector = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    }
}
