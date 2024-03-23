package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.composekit.settings.ui.Theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonElement

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


    override fun hasConfig(): Boolean =
        this == BELOW_PLAYER

    @Composable
    override fun ConfigurationItems(
        config_data: JsonElement?,
        item_modifier: Modifier,
        onModification: (JsonElement?) -> Unit
    ) {
        if (this != BELOW_PLAYER) {
            return
        }

        BelowPlayerConfigurationItems(config_data, item_modifier, onModification)
    }
}
