package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.runtime.*
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString

sealed interface LayoutSlot {
    fun getKey(): String
    fun getName(): String

    val is_vertical: Boolean
    val slots_key: SettingsKey

}

@Composable
fun LayoutSlot.observeContentBar(): State<ContentBar?> {
    val slots: String by slots_key.rememberMutableState()
    val custom_bars: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()

    return remember { derivedStateOf {
        val slot: Int =
            Json.decodeFromString<Map<String, Int>>(slots)
                .get(getKey()) ?: return@derivedStateOf null

        if (slot < 0) {
            val bars: List<CustomContentBar> = Json.decodeFromString(custom_bars)
            return@derivedStateOf bars.getOrNull(slot.absoluteValue - 1)
        }

        return@derivedStateOf InternalContentBar.getAll().getOrNull(slot)
    } }
}

@Composable
fun LayoutSlot.DisplayBar(modifier: Modifier = Modifier): Boolean {
    val content_bar: ContentBar? by observeContentBar()
    content_bar?.Bar(this, modifier)
    return content_bar != null
}

enum class PortraitLayoutSlot: LayoutSlot {
    UPPER_TOP_BAR,
    LOWER_TOP_BAR,
    ABOVE_PLAYER,
    BELOW_PLAYER;

    override val is_vertical: Boolean = false
    override val slots_key: SettingsKey = LayoutSettings.Key.PORTRAIT_SLOTS

    override fun getKey(): String = name

    override fun getName(): String =
        when (this) {
            UPPER_TOP_BAR -> getString("layout_slot_portrait_upper_top_bar")
            LOWER_TOP_BAR -> getString("layout_slot_portrait_lower_top_bar")
            ABOVE_PLAYER -> getString("layout_slot_portrait_above_player")
            BELOW_PLAYER -> getString("layout_slot_portrait_below_player")
        }
}

enum class LandscapeLayoutSlot: LayoutSlot {
    SIDE_LEFT,
    SIDE_RIGHT,
    PAGE_TOP,
    ABOVE_PLAYER,
    BELOW_PLAYER;

    override val is_vertical: Boolean get() =
        when (this) {
            SIDE_LEFT, SIDE_RIGHT -> true
            else -> false
        }

    override val slots_key: SettingsKey = LayoutSettings.Key.LANDSCAPE_SLOTS

    override fun getKey(): String = name

    override fun getName(): String =
        when (this) {
            SIDE_LEFT -> getString("layout_slot_landscape_side_left")
            SIDE_RIGHT -> getString("layout_slot_landscape_side_right")
            PAGE_TOP -> getString("layout_slot_landscape_page_top")
            ABOVE_PLAYER -> getString("layout_slot_landscape_above_player")
            BELOW_PLAYER -> getString("layout_slot_landscape_below_player")
        }
}
