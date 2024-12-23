package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import dev.toastbits.composekit.components.utils.composable.RowOrColumn
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.theme.core.ThemeValues
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import dev.toastbits.composekit.components.utils.composable.RowOrColumnScope
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.layout_slot_config_below_player_show_in_player
import spmp.shared.generated.resources.layout_slot_config_below_player_show_in_queue

@Serializable
sealed interface LayoutSlot {
    fun getKey(): String

    @Composable
    fun getName(): String

    fun getDefaultContentBar(): ContentBar?
    fun getDefaultBackgroundColour(theme: ThemeValues): ColourSource

    fun hasConfig(): Boolean = false
    @Composable
    fun ConfigurationItems(
        config_data: JsonElement?,
        item_modifier: Modifier,
        onModification: (JsonElement?) -> Unit
    ) {}

    val is_vertical: Boolean
    val is_start: Boolean
    fun getSlotsProperty(context: AppContext): PlatformSettingsProperty<Map<String, ContentBarReference?>>

    fun getContentBarSelectionState(): ContentBar.BarSelectionState? =
        ContentBar.bar_selection_state

    fun mustShow(): Boolean =
        getContentBarSelectionState() != null

    @Serializable
    data class BelowPlayerConfig(
        val show_in_player: Boolean = true,
        val show_in_queue: Boolean = true
    )
}

@Composable
fun LayoutSlot.observeContentBar(): State<ContentBar?> {
    val player: PlayerState = LocalPlayerState.current
    val slots: Map<String, ContentBarReference?> by getSlotsProperty(player.context).observe()
    val custom_bars: List<CustomContentBar> by player.settings.Layout.CUSTOM_BARS.observe()

    return remember(this) { derivedStateOf {
        if (!slots.contains(getKey())) {
            return@derivedStateOf getDefaultContentBar()
        }
        return@derivedStateOf slots[getKey()]?.getBar(custom_bars)
    } }
}

@Composable
fun LayoutSlot.observeConfigData(): JsonElement? {
    val player: PlayerState = LocalPlayerState.current
    val slot_configs: Map<String, JsonElement> by player.settings.Layout.SLOT_CONFIGS.observe()
    return remember(slot_configs, this) { slot_configs[getKey()] }
}

@Composable
inline fun <reified T> LayoutSlot.observeConfig(noinline getDefault: @DisallowComposableCalls () -> T): T {
    val player: PlayerState = LocalPlayerState.current
    val slot_configs: Map<String, JsonElement> by player.settings.Layout.SLOT_CONFIGS.observe()
    return remember(slot_configs, this) {
        val config_data: JsonElement = slot_configs[getKey()] ?: return@remember getDefault()
        return@remember Json.decodeFromJsonElement(config_data)
    }
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

@Composable
internal fun BelowPlayerConfigurationItems(
    slot: LayoutSlot,
    config_data: JsonElement?,
    item_modifier: Modifier,
    onModification: (JsonElement) -> Unit
) {
    val config: LayoutSlot.BelowPlayerConfig =
        remember(config_data) {
            config_data?.let { Json.decodeFromJsonElement(it) } ?: LayoutSlot.BelowPlayerConfig()
        }

    Row(
        item_modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(Res.string.layout_slot_config_below_player_show_in_player))

        Switch(
            config.show_in_player,
            {
                onModification(Json.encodeToJsonElement(config.copy(show_in_player = it)))
            }
        )
    }

    if (slot is PortraitLayoutSlot) {
        Row(
            item_modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(Res.string.layout_slot_config_below_player_show_in_queue))

            Switch(
                config.show_in_queue,
                {
                    onModification(Json.encodeToJsonElement(config.copy(show_in_queue = it)))
                }
            )
        }
    }
}
