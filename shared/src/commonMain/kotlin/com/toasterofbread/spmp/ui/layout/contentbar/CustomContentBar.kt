package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import dev.toastbits.composekit.components.utils.composable.*
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.get
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.*
import kotlinx.serialization.*

const val CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP: Float = 50f

@Composable
fun List<ContentBarElement>.shouldDisplayBarOf(): Boolean {
    var any_displaying: Boolean = false
    for (element in this) {
        if (element.isDisplaying()) {
            any_displaying = true
        }
        else if (element.config.hide_bar_when_empty) {
            return false
        }
    }

    return any_displaying
}

@Serializable
data class CustomContentBar(
    val bar_name: String,
    val size_dp: Float = CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP,
    val elements: List<ContentBarElement> = emptyList()
): ContentBar() {
    @Composable
    override fun getName(): String = bar_name
    @Composable
    override fun getDescription(): String? = null
    override fun getIcon(): ImageVector = Icons.Default.Build

    @Composable
    override fun isDisplaying(): Boolean =
        elements.shouldDisplayBarOf()

    @Composable
    override fun BarContent(
        slot: LayoutSlot,
        background_colour: ThemeValues.Slot?,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        return CustomBarContent(elements, size_dp.dp, slot.is_vertical, content_padding, slot, background_colour, modifier)
    }

    @Composable
    internal fun CustomBarContent(
        vertical: Boolean,
        content_padding: PaddingValues,
        background_colour: ThemeValues.Slot? = null,
        modifier: Modifier = Modifier,
        selected_element_override: Int? = null,
        apply_size: Boolean = true,
        scrolling: Boolean = true,
        always_display: Boolean = false,
        getFillLengthModifier: RowOrColumnScope.() -> Modifier = {
            Modifier
                .weight(1f)
                .then(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
        },
        getSpacerElementModifier: (@Composable RowOrColumnScope.(Int, ContentBarElementSpacer) -> Modifier)? = null,
        shouldShowButton: @Composable (ContentBarElement) -> Boolean = { it.shouldShow() },
        buttonContent: @Composable (Int, ContentBarElement, DpSize) -> Unit =
            { _, element, size -> element.Element(vertical, null, size, Modifier.fillMaxSize()) }
    ): Boolean {
        return CustomBarContent(
            elements,
            size_dp.dp,
            vertical = vertical,
            content_padding = content_padding,
            background_colour = background_colour,
            modifier = modifier,
            selected_element_override = selected_element_override,
            apply_size = apply_size,
            scrolling = scrolling,
            always_display = always_display,
            getFillLengthModifier = getFillLengthModifier,
            getSpacerElementModifier = getSpacerElementModifier,
            shouldShowButton = shouldShowButton,
            buttonContent = buttonContent
        )
    }
}

@Composable
internal fun CustomBarContent(
    elements: List<ContentBarElement>,
    size: Dp,
    vertical: Boolean,
    content_padding: PaddingValues,
    slot: LayoutSlot? = null,
    background_colour: ThemeValues.Slot? = null,
    modifier: Modifier = Modifier,
    selected_element_override: Int? = null,
    apply_size: Boolean = true,
    scrolling: Boolean = true,
    always_display: Boolean = false,
    getFillLengthModifier: RowOrColumnScope.() -> Modifier = {
        Modifier
            .weight(1f)
            .then(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
    },
    getSpacerElementModifier: (@Composable RowOrColumnScope.(Int, ContentBarElementSpacer) -> Modifier)? = null,
    shouldShowButton: @Composable (ContentBarElement) -> Boolean = { it.shouldShow() },
    buttonContent: @Composable (Int, ContentBarElement, DpSize) -> Unit =
        { _, element, size -> element.Element(vertical, slot, size, Modifier.fillMaxSize()) }
): Boolean {
    val player: PlayerState = LocalPlayerState.current
    val selected_element: Int? =
        selected_element_override ?: elements.indexOfFirst { it.isSelected() }.takeIf { it != -1 }

    val content_colour: Color = LocalContentColor.current
    val indicator_colour: Color =
        when (background_colour) {
            ThemeValues.Slot.BuiltIn.BACKGROUND -> player.theme.vibrantAccent
            ThemeValues.Slot.BuiltIn.CARD -> player.theme.vibrantAccent
            ThemeValues.Slot.BuiltIn.ACCENT -> player.theme.background
            ThemeValues.Slot.Extension.VIBRANT_ACCENT -> player.theme.background
            else -> content_colour
        }

    if (!elements.shouldDisplayBarOf() && !always_display) {
        return false
    }

    BoxWithConstraints(
        modifier
            .padding(content_padding)
            .thenIf(apply_size) {
                if (vertical) requiredWidth(size)
                else requiredHeight(size)
            },
        contentAlignment = Alignment.Center
    ) {
        SidebarButtonSelector(
            selected_button = selected_element,
            buttons = elements,
            indicator_colour = indicator_colour,
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
                    else background_colour?.let { player.theme[it] }?.getContrasted() ?: LocalContentColor.current
            ) {
                buttonContent(index, element, DpSize(this@BoxWithConstraints.maxWidth, this@BoxWithConstraints.maxHeight))
            }
        }
    }

    return true
}
