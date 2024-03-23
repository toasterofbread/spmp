package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.composable.RowOrColumn
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import kotlin.math.absoluteValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope

@Serializable
sealed interface LayoutSlot {
    fun getKey(): String
    fun getName(): String

    fun getDefaultContentBar(): ContentBar?
    fun getDefaultBackgroundColour(theme: Theme): ColourSource

    val is_vertical: Boolean
    val is_start: Boolean
    val slots_key: SettingsKey
}

@Composable
fun LayoutSlot.observeContentBar(): State<ContentBar?> {
    val slots_data: String by slots_key.rememberMutableState()
    val custom_bars: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()

    return remember { derivedStateOf {
        val slots: Map<String, ContentBarReference?> = Json.decodeFromString(slots_data)
        if (!slots.contains(getKey())) {
            return@derivedStateOf getDefaultContentBar()
        }
        return@derivedStateOf slots[getKey()]?.getBar(custom_bars)
    } }
}

@Composable
fun LayoutSlot.OrientedLayout(
    content_padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable RowOrColumnScope.() -> Unit
) {
    val top_padding: Dp = content_padding.calculateTopPadding()
    val bottom_padding: Dp = content_padding.calculateBottomPadding()
    val start_padding: Dp = content_padding.calculateStartPadding(LocalLayoutDirection.current)
    val end_padding: Dp = content_padding.calculateEndPadding(LocalLayoutDirection.current)

    // val padding_modifier: Modifier =
    //     if (is_vertical) Modifier.padding(start = start_padding, end = end_padding)
    //     else Modifier.padding(top = top_padding, end = end_padding)

    RowOrColumn(!is_vertical, modifier, alignment = 0) {
        Spacer(
            if (is_vertical) Modifier.height(top_padding)
            else Modifier.width(start_padding)
        )

        content()

        Spacer(
            if (is_vertical) Modifier.height(bottom_padding)
            else Modifier.width(end_padding)
        )
    }
}

enum class PortraitLayoutSlot: LayoutSlot {
    UPPER_TOP_BAR,
    LOWER_TOP_BAR,
    ABOVE_PLAYER,
    BELOW_PLAYER;

    override val is_vertical: Boolean = false
    override val is_start: Boolean get() =
        when (this) {
            UPPER_TOP_BAR -> true
            LOWER_TOP_BAR -> true
            ABOVE_PLAYER -> false
            BELOW_PLAYER -> false
        }
    override val slots_key: SettingsKey = LayoutSettings.Key.PORTRAIT_SLOTS

    override fun getKey(): String = name

    override fun getName(): String =
        when (this) {
            UPPER_TOP_BAR -> getString("layout_slot_portrait_upper_top_bar")
            LOWER_TOP_BAR -> getString("layout_slot_portrait_lower_top_bar")
            ABOVE_PLAYER -> getString("layout_slot_portrait_above_player")
            BELOW_PLAYER -> getString("layout_slot_portrait_below_player")
        }

    override fun getDefaultContentBar(): ContentBar? =
        when (this) {
            LOWER_TOP_BAR -> InternalContentBar.PRIMARY
            ABOVE_PLAYER -> InternalContentBar.SECONDARY
            BELOW_PLAYER -> InternalContentBar.NAVIGATION

            else -> null
        }

    override fun getDefaultBackgroundColour(theme: Theme): ColourSource =
        when (this) {
            UPPER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            LOWER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            ABOVE_PLAYER -> ThemeColourSource(Theme.Colour.ACCENT)
            BELOW_PLAYER -> ThemeColourSource(Theme.Colour.CARD)
        }
}

enum class LandscapeLayoutSlot: LayoutSlot {
    OUTER_SIDE_LEFT,
    INNER_SIDE_LEFT,
    OUTER_SIDE_RIGHT,
    INNER_SIDE_RIGHT,
    UPPER_TOP_BAR,
    LOWER_TOP_BAR,
    ABOVE_PLAYER,
    BELOW_PLAYER;

    override val is_vertical: Boolean get() =
        when (this) {
            OUTER_SIDE_LEFT,
            INNER_SIDE_LEFT,
            OUTER_SIDE_RIGHT,
            INNER_SIDE_RIGHT -> true
            else -> false
        }

    override val is_start: Boolean get() =
        when (this) {
            OUTER_SIDE_LEFT -> true
            INNER_SIDE_LEFT -> true
            OUTER_SIDE_RIGHT -> false
            INNER_SIDE_RIGHT -> false
            UPPER_TOP_BAR -> true
            LOWER_TOP_BAR -> true
            ABOVE_PLAYER -> false
            BELOW_PLAYER -> false
        }

    override val slots_key: SettingsKey = LayoutSettings.Key.LANDSCAPE_SLOTS

    override fun getKey(): String = name

    override fun getName(): String =
        when (this) {
            OUTER_SIDE_LEFT -> getString("layout_slot_landscape_outer_side_left")
            INNER_SIDE_LEFT -> getString("layout_slot_landscape_inner_side_left")
            OUTER_SIDE_RIGHT -> getString("layout_slot_landscape_outer_side_right")
            INNER_SIDE_RIGHT -> getString("layout_slot_landscape_inner_side_right")
            UPPER_TOP_BAR -> getString("layout_slot_landscape_upper_top_bar")
            LOWER_TOP_BAR -> getString("layout_slot_landscape_lower_top_bar")
            ABOVE_PLAYER -> getString("layout_slot_landscape_above_player")
            BELOW_PLAYER -> getString("layout_slot_landscape_below_player")
        }

    override fun getDefaultContentBar(): ContentBar? =
        when (this) {
            OUTER_SIDE_LEFT -> InternalContentBar.NAVIGATION
            INNER_SIDE_LEFT -> InternalContentBar.NAVIGATION
            OUTER_SIDE_RIGHT -> InternalContentBar.NAVIGATION
            INNER_SIDE_RIGHT -> InternalContentBar.NAVIGATION

            UPPER_TOP_BAR -> InternalContentBar.NAVIGATION
            LOWER_TOP_BAR -> InternalContentBar.NAVIGATION

            ABOVE_PLAYER -> InternalContentBar.NAVIGATION
            BELOW_PLAYER -> InternalContentBar.NAVIGATION
        }

    override fun getDefaultBackgroundColour(theme: Theme): ColourSource =
        when (this) {
            OUTER_SIDE_LEFT -> ThemeColourSource(Theme.Colour.CARD)
            INNER_SIDE_LEFT -> ThemeColourSource(Theme.Colour.CARD)
            OUTER_SIDE_RIGHT -> ThemeColourSource(Theme.Colour.CARD)
            INNER_SIDE_RIGHT -> ThemeColourSource(Theme.Colour.CARD)
            UPPER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            LOWER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            ABOVE_PLAYER -> ThemeColourSource(Theme.Colour.ACCENT)
            BELOW_PLAYER -> ThemeColourSource(Theme.Colour.CARD)
        }
}
