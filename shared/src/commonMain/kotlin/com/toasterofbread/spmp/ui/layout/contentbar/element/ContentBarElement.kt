package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.draw.clipToBounds
import com.toasterofbread.composekit.utils.common.thenWith
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.platform.visualiser.Visualiser
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject

private const val SIZE_DP_STEP: Float = 10f
private const val MIN_SIZE_DP: Float = 10f
private val DEFAULT_SIZE_MODE: ContentBarElement.SizeMode = ContentBarElement.SizeMode.STATIC
private const val DEFAULT_SIZE: Int = 50

abstract class ContentBarElement(data: ContentBarElementData) {
    val type: Type = data.type
    var size_mode: SizeMode by mutableStateOf(data.size_mode)
    var size: Int by mutableStateOf(data.size)

    fun shouldFillLength(): Boolean = size_mode == SizeMode.FILL

    fun getData(): ContentBarElementData =
        ContentBarElementData(
            type = type,
            size_mode = size_mode,
            size = size,
            data = getSubData()
        )
    open fun getSubData(): JsonObject? = null

    @Composable
    open fun isSelected(): Boolean = false
    @Composable
    open fun shouldShow(): Boolean = true
    open fun blocksIndicatorAnimation(): Boolean = false

    @Composable
    fun Element(
        vertical: Boolean,
        bar_size: DpSize,
        modifier: Modifier = Modifier,
        enable_interaction: Boolean = true
    ) {
        val size_dp: Dp? =
            when (size_mode) {
                SizeMode.FILL -> null
                SizeMode.STATIC -> size.dp
                SizeMode.PERCENTAGE -> bar_size.width * size * 0.01f
            }

        ElementContent(
            vertical,
            enable_interaction,
            modifier
                .thenWith(size_dp) {
                    if (vertical) height(it)
                    else width(it)
                }
                .run {
                    if (vertical) width(bar_size.height)
                    else height(bar_size.height)
                }
                .clipToBounds()
        )
    }

    @Composable
    protected abstract fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier)

    @Composable
    open fun SubConfigurationItems(onModification: () -> Unit) {}

    @Composable
    fun ConfigurationItems(onModification: () -> Unit) {
        SubConfigurationItems(onModification)

        var show_mode_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            show_mode_selector,
            onDismissRequest = {
                show_mode_selector = false
            },
            SizeMode.entries.size,
            size_mode.ordinal,
            { SizeMode.entries[it].getName() }
        ) {
            show_mode_selector = false
            size_mode = SizeMode.entries[it]
            size = 50
            onModification()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(getString("content_bar_element_builtin_config_size_mode"))

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Button({ show_mode_selector = !show_mode_selector }) {
                Text(size_mode.getName())
            }
        }

        AnimatedVisibility(size_mode != SizeMode.FILL) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(getString("content_bar_element_builtin_config_size"))

                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({
                    size = (size - size_mode.getStep()).coerceAtLeast(10)
                    onModification()
                }) {
                    Icon(Icons.Default.Remove, null)
                }

                Text(
                    if (size_mode == SizeMode.STATIC) "${size}dp"
                    else "${size}%"
                )

                IconButton({
                    size = size + size_mode.getStep()
                    if (size_mode == SizeMode.PERCENTAGE) {
                        size = size.coerceAtMost(100)
                    }

                    onModification()
                }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }

    enum class Type {
        BUTTON,
        SPACER,
        LYRICS,
        VISUALISER,
        PINNED_ITEMS;

        fun isAvailable(): Boolean =
            when (this) {
                VISUALISER -> Visualiser.isSupported()
                else -> true
            }

        fun getName(): String =
            when (this) {
                BUTTON -> getString("content_bar_element_type_button")
                SPACER -> getString("content_bar_element_type_spacer")
                LYRICS -> getString("content_bar_element_type_lyrics")
                VISUALISER -> getString("content_bar_element_type_visualiser")
                PINNED_ITEMS -> getString("content_bar_element_type_pinned_items")
            }

        fun getIcon(): ImageVector =
            when (this) {
                BUTTON -> Icons.Default.TouchApp
                SPACER -> Icons.Default.Expand
                LYRICS -> Icons.Default.MusicNote
                VISUALISER -> Icons.Default.Waves
                PINNED_ITEMS -> Icons.Default.PushPin
            }
    }

    enum class SizeMode {
        FILL,
        STATIC,
        PERCENTAGE;

        fun getName(): String =
            when (this) {
                FILL -> getString("content_bar_element_builtin_config_size_mode_fill")
                STATIC -> getString("content_bar_element_builtin_config_size_mode_static")
                PERCENTAGE -> getString("content_bar_element_builtin_config_size_mode_percentage")
            }

        fun getStep(): Int =
            when (this) {
                FILL -> 0
                STATIC -> 10
                PERCENTAGE -> 10
            }
    }
}

@Serializable
data class ContentBarElementData(
    val type: ContentBarElement.Type,
    val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    val size: Int = DEFAULT_SIZE,
    val data: JsonObject? = null
) {
    fun toElement(): ContentBarElement =
        when (type) {
            ContentBarElement.Type.BUTTON -> ContentBarElementButton(this)
            ContentBarElement.Type.SPACER -> ContentBarElementSpacer(this)
            ContentBarElement.Type.LYRICS -> ContentBarElementLyrics(this)
            ContentBarElement.Type.VISUALISER -> ContentBarElementVisualiser(this)
            ContentBarElement.Type.PINNED_ITEMS -> ContentBarElementPinnedItems(this)
        }
}
