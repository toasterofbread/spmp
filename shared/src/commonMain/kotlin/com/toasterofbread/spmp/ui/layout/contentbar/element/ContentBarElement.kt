package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.utils.common.thenWith
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.platform.visualiser.MusicVisualiser
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable

internal val DEFAULT_SIZE_MODE: ContentBarElement.SizeMode = ContentBarElement.SizeMode.STATIC
internal const val DEFAULT_SIZE: Int = 50

private const val SIZE_DP_STEP: Float = 10f
private const val MIN_SIZE_DP: Float = 10f

@Serializable
sealed class ContentBarElement {
    abstract val size_mode: SizeMode
    abstract val size: Int
    abstract fun getType(): Type

    protected abstract fun copyWithSize(size_mode: SizeMode, size: Int): ContentBarElement

    fun shouldFillLength(): Boolean = size_mode == SizeMode.FILL

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
    open fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {}

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ConfigurationItems(onModification: (ContentBarElement) -> Unit) {
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
            onModification(copyWithSize(SizeMode.entries[it], 50))
            show_mode_selector = false
        }

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("content_bar_element_builtin_config_size_mode"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_mode_selector = !show_mode_selector }) {
                Text(size_mode.getName())
            }
        }

        if (size_mode != SizeMode.FILL) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    getString("content_bar_element_builtin_config_size"),
                    Modifier.align(Alignment.CenterVertically),
                    softWrap = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    IconButton({
                        onModification(
                            copyWithSize(size_mode, (size - size_mode.getStep()).coerceAtLeast(10))
                        )
                    }) {
                        Icon(Icons.Default.Remove, null)
                    }

                    Text(
                        if (size_mode == SizeMode.STATIC) "${size}dp"
                        else "${size}%"
                    )

                    IconButton({
                        var new_size: Int = size + size_mode.getStep()
                        if (size_mode == SizeMode.PERCENTAGE) {
                            new_size = size.coerceAtMost(100)
                        }

                        onModification(copyWithSize(size_mode, new_size))
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }

        SubConfigurationItems(Modifier.fillMaxWidth(), onModification)
    }

    enum class Type {
        BUTTON,
        SPACER,
        LYRICS,
        VISUALISER,
        PINNED_ITEMS;

        fun isAvailable(): Boolean =
            when (this) {
                VISUALISER -> MusicVisualiser.isSupported()
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

        fun createElement(): ContentBarElement =
            when (this) {
                ContentBarElement.Type.BUTTON -> ContentBarElementButton()
                ContentBarElement.Type.SPACER -> ContentBarElementSpacer()
                ContentBarElement.Type.LYRICS -> ContentBarElementLyrics()
                ContentBarElement.Type.VISUALISER -> ContentBarElementVisualiser()
                ContentBarElement.Type.PINNED_ITEMS -> ContentBarElementPinnedItems()
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
