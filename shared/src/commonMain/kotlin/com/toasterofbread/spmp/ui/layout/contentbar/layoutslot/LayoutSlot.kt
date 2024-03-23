package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope

@Serializable
sealed interface LayoutSlot {
    fun getKey(): String
    fun getName(): String

    fun getDefaultContentBar(): ContentBar?
    fun getDefaultBackgroundColour(theme: Theme): ColourSource

    fun hasConfig(): Boolean = false
    @Composable
    fun ConfigurationItems(
        config_data: JsonElement?,
        item_modifier: Modifier,
        onModification: (JsonElement?) -> Unit
    ) {}

    val is_vertical: Boolean
    val is_start: Boolean
    val slots_key: SettingsKey

    fun getContentBarSelectionState(): ContentBar.BarSelectionState? =
        ContentBar.bar_selection_state

    fun mustShow(): Boolean = 
        getContentBarSelectionState() != null

    @Serializable
    data class BelowPlayerConfig(val show_when_expanded: Boolean = false)
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
fun LayoutSlot.observeConfigData(): JsonElement? {
    val slot_configs_data: String by LayoutSettings.Key.SLOT_CONFIGS.rememberMutableState()
    return remember(slot_configs_data) {
        val slot_configs: Map<String, JsonElement> = Json.decodeFromString(slot_configs_data)
        return@remember slot_configs[getKey()]
    }
}

@Composable
inline fun <reified T> LayoutSlot.observeConfig(noinline getDefault: @DisallowComposableCalls () -> T): T {
    val slot_configs_data: String by LayoutSettings.Key.SLOT_CONFIGS.rememberMutableState()
    return remember(slot_configs_data) {
        val slot_configs: Map<String, JsonElement> = Json.decodeFromString(slot_configs_data)
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
    config_data: JsonElement?,
    item_modifier: Modifier,
    onModification: (JsonElement?) -> Unit
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
        Text(getString("layout_slot_config_below_player_show_when_expanded"))

        Switch(
            config.show_when_expanded,
            {
                onModification(
                    Json.encodeToJsonElement(config.copy(show_when_expanded = it))
                )
            }
        )
    }
}
