package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.SidebarButtonSelector
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope
import com.toasterofbread.composekit.settings.ui.Theme
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
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: Theme.Colour?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        modifier: Modifier
    ): Boolean {
        CustomBarContent(slot.is_vertical, content_padding, background_colour, modifier)
        return true
    }

    @Composable
    internal fun CustomBarContent(
        vertical: Boolean,
        content_padding: PaddingValues,
        background_colour: Theme.Colour? = null,
        modifier: Modifier = Modifier,
        selected_element_override: Int? = null,
        apply_size: Boolean = true,
        scrolling: Boolean = true,
        getFillLengthModifier: RowOrColumnScope.() -> Modifier = {
            Modifier
                .weight(1f)
                .then(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
        },
        getSpacerElementModifier: (@Composable RowOrColumnScope.(Int, ContentBarElementSpacer) -> Modifier)? = null,
        shouldShowButton: @Composable (ContentBarElement) -> Boolean = { it.shouldShow() },
        buttonContent: @Composable (Int, ContentBarElement, DpSize) -> Unit =
            { _, element, size -> element.Element(vertical, size, Modifier) }
    ) {
        val player: PlayerState = LocalPlayerState.current
        val selected_element: Int? =
            selected_element_override ?: elements.indexOfFirst { it.isSelected() }.takeIf { it != -1 }

        val content_colour: Color = LocalContentColor.current
        val indicator_colour: Color =
            when (background_colour) {
                Theme.Colour.BACKGROUND -> player.theme.vibrant_accent
                Theme.Colour.CARD -> player.theme.vibrant_accent
                Theme.Colour.ACCENT -> player.theme.background
                Theme.Colour.VIBRANT_ACCENT -> player.theme.background
                else -> content_colour
            }

        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            SidebarButtonSelector(
                selected_button = selected_element,
                buttons = elements,
                indicator_colour = indicator_colour,
                modifier = Modifier
                    .padding(content_padding)
                    .thenIf(apply_size) {
                        if (vertical) width(size_dp.dp)
                        else height(size_dp.dp)
                    },
                vertical = vertical,
                scrolling = scrolling,
                alignment = 0,
                isSpacing = {
                    it.blocksIndicatorAnimation()
                },
                arrangement = Arrangement.spacedBy(1.dp),
                showButton = { element ->
                    return@SidebarButtonSelector shouldShowButton(element)
                },
                getButtonModifier = { index, element ->
                    val base_modifier: Modifier =
                        if (element.shouldFillLength()) getFillLengthModifier()
                        else Modifier

                    if (element is ContentBarElementSpacer && getSpacerElementModifier != null) {
                        return@SidebarButtonSelector base_modifier.then(getSpacerElementModifier(index, element))
                    }
                    return@SidebarButtonSelector base_modifier
                }
            ) { index, element ->
                CompositionLocalProvider(
                    LocalContentColor provides
                        if (index == selected_element) indicator_colour.getContrasted()
                        else background_colour?.get(player.theme)?.getContrasted() ?: LocalContentColor.current
                ) {
                    buttonContent(index, element, DpSize(this@BoxWithConstraints.maxWidth, this@BoxWithConstraints.maxHeight))
                }
            }
        }
    }
}
