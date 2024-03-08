package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.composable.SidebarButtonSelector
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

const val CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP: Float = 50f

@Serializable
data class CustomContentBar(
    val bar_name: String,
    val size_dp: Float = CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP,
    val element_data: List<ContentBarElementData> = emptyList()
): ContentBar() {
    @Transient
    val elements: List<ContentBarElement> = element_data.map { it.toElement() }

    override fun getName(): String = bar_name
    override fun getDescription(): String? = null
    override fun getIcon(): ImageVector = Icons.Default.Build

    @Composable
    override fun BarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        CustomBarContent(slot.is_vertical, content_padding, modifier)
        return true
    }

    @Composable
    internal fun CustomBarContent(
        vertical: Boolean,
        content_padding: PaddingValues,
        modifier: Modifier = Modifier,
        selected_element_override: Int? = null,
        spacerElementHandler: (@Composable RowOrColumnScope.(Int, ContentBarElementSpacer) -> Unit)? = null,
        buttonContent: @Composable (Int, ContentBarElement) -> Unit =
            { _, element -> element.Element(vertical, Modifier) }
    ) {
        val player: PlayerState = LocalPlayerState.current
        val selected_element: Int? =
            selected_element_override ?: elements.indexOfFirst { it.isSelected() }.takeIf { it != -1 }

        SidebarButtonSelector(
            selected_button = selected_element,
            buttons = elements,
            indicator_colour = player.theme.vibrant_accent,
            modifier = modifier
                .padding(content_padding)
                .then(if (vertical) Modifier.width(size_dp.dp) else Modifier.height(size_dp.dp)),
            vertical = vertical,
            alignment = 0,
            isSpacing = { it is ContentBarElementSpacer },
            arrangement = Arrangement.spacedBy(1.dp),
            // showButton = { button ->
            //     if (button == null) {
            //         return@SidebarButtonSelector false
            //     }

            //     val page: AppPage? = AppPageSidebarButton.entries[button.ordinal].getPage(player)
            //     return@SidebarButtonSelector button.shouldShow(page)
            // },
            extraContent = { index, element ->
                if (element is ContentBarElementSpacer) {
                    if (spacerElementHandler != null) {
                        spacerElementHandler(index, element)
                    }
                    else {
                        with (element) {
                            SpacerElement(vertical)
                        }
                    }
                }
            }
        ) { index, element ->
            if (spacerElementHandler != null && element is ContentBarElementSpacer) {
                return@SidebarButtonSelector
            }

            CompositionLocalProvider(
                LocalContentColor provides
                    if (index == selected_element) player.theme.vibrant_accent.getContrasted()
                    else player.theme.on_background
            ) {
                buttonContent(index, element)
            }
        }
    }
}
