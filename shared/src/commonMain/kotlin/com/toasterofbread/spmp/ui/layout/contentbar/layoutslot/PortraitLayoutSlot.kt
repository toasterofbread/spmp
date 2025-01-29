package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBarTemplate
import com.toasterofbread.spmp.platform.AppContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.theme.core.ThemeValues
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.layout_slot_portrait_upper_top_bar
import spmp.shared.generated.resources.layout_slot_portrait_lower_top_bar
import spmp.shared.generated.resources.layout_slot_portrait_above_player
import spmp.shared.generated.resources.layout_slot_portrait_below_player
import spmp.shared.generated.resources.layout_slot_portrait_player_top
import spmp.shared.generated.resources.layout_slot_config_portrait_player_top_show_in_queue

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

    override fun getSlotsProperty(context: AppContext): PlatformSettingsProperty<Map<String, ContentBarReference?>> =
        context.settings.Layout.PORTRAIT_SLOTS

    override fun getKey(): String = name

    @Composable
    override fun getName(): String =
        when (this) {
            UPPER_TOP_BAR -> stringResource(Res.string.layout_slot_portrait_upper_top_bar)
            LOWER_TOP_BAR -> stringResource(Res.string.layout_slot_portrait_lower_top_bar)
            ABOVE_PLAYER -> stringResource(Res.string.layout_slot_portrait_above_player)
            BELOW_PLAYER -> stringResource(Res.string.layout_slot_portrait_below_player)
            PLAYER_TOP -> stringResource(Res.string.layout_slot_portrait_player_top)
        }

    override fun getDefaultContentBar(): ContentBar? =
        when (this) {
            UPPER_TOP_BAR -> CustomContentBarTemplate.DEFAULT_PORTRAIT_TOP_UPPER.getContentBar()
            LOWER_TOP_BAR -> CustomContentBarTemplate.DEFAULT_PORTRAIT_TOP_LOWER.getContentBar()
            ABOVE_PLAYER -> InternalContentBar.SECONDARY
            BELOW_PLAYER -> null
            PLAYER_TOP -> CustomContentBarTemplate.LYRICS.getContentBar()
        }

    override fun getDefaultBackgroundColour(theme: ThemeValues): ColourSource =
        when (this) {
            UPPER_TOP_BAR -> ThemeColourSource(ThemeValues.Slot.BuiltIn.BACKGROUND)
            LOWER_TOP_BAR -> ThemeColourSource(ThemeValues.Slot.BuiltIn.BACKGROUND)
            ABOVE_PLAYER -> CustomColourSource(Color.Transparent)
            BELOW_PLAYER -> ThemeColourSource(ThemeValues.Slot.BuiltIn.ACCENT)
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
            Text(stringResource(Res.string.layout_slot_config_portrait_player_top_show_in_queue))

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
        val show_in_queue: Boolean = true
    )
}
