package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.composekit.settings.ui.Theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable

enum class PortraitLayoutSlot: LayoutSlot {
    UPPER_TOP_BAR,
    LOWER_TOP_BAR,
    ABOVE_PLAYER,
    BELOW_PLAYER,
    PLAYER_TOP;

    override val is_vertical: Boolean = false
    override val is_start: Boolean get() =
        when (this) {
            UPPER_TOP_BAR -> true
            LOWER_TOP_BAR -> true
            ABOVE_PLAYER -> false
            BELOW_PLAYER -> false
            PLAYER_TOP -> true
        }
    override val slots_key: SettingsKey = LayoutSettings.Key.PORTRAIT_SLOTS

    override fun getKey(): String = name

    override fun getName(): String =
        when (this) {
            UPPER_TOP_BAR -> getString("layout_slot_portrait_upper_top_bar")
            LOWER_TOP_BAR -> getString("layout_slot_portrait_lower_top_bar")
            ABOVE_PLAYER -> getString("layout_slot_portrait_above_player")
            BELOW_PLAYER -> getString("layout_slot_portrait_below_player")
            PLAYER_TOP -> getString("layout_slot_portrait_player_top")
        }

    override fun getDefaultContentBar(): ContentBar? =
        when (this) {
            UPPER_TOP_BAR -> InternalContentBar.PRIMARY
            LOWER_TOP_BAR -> InternalContentBar.SECONDARY
            ABOVE_PLAYER -> InternalContentBar.LYRICS
            BELOW_PLAYER -> InternalContentBar.NAVIGATION
            PLAYER_TOP -> InternalContentBar.LYRICS
        }

    override fun getDefaultBackgroundColour(theme: Theme): ColourSource =
        when (this) {
            UPPER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            LOWER_TOP_BAR -> ThemeColourSource(Theme.Colour.CARD)
            ABOVE_PLAYER -> CustomColourSource(Color.Transparent)
            BELOW_PLAYER -> ThemeColourSource(Theme.Colour.CARD)
            PLAYER_TOP -> CustomColourSource(Color.Transparent)
        }

    override fun hasConfig(): Boolean =
        this == PLAYER_TOP || this == BELOW_PLAYER

    @Composable
    override fun ConfigurationItems(
        config_data: JsonElement?,
        item_modifier: Modifier,
        onModification: (JsonElement?) -> Unit
    ) {
        if (this == BELOW_PLAYER) {
            BelowPlayerConfigurationItems(this, config_data, item_modifier, onModification)
            return
        }

        if (this != PLAYER_TOP) {
            return
        }

        val config: PlayerTopConfig =
            remember(config_data) {
                config_data?.let { Json.decodeFromJsonElement(it) } ?: PlayerTopConfig()
            }

        Row(
            item_modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(getString("layout_slot_config_portrait_player_top_show_in_queue"))

            Switch(
                config.show_in_queue,
                {
                    onModification(
                        Json.encodeToJsonElement(config.copy(show_in_queue = it))
                    )
                }
            )
        }
    }

    @Serializable
    data class PlayerTopConfig(
        val show_in_queue: Boolean = false
    )
}
