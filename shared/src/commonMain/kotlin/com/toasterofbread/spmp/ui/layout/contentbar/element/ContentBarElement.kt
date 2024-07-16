package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.utils.common.thenWith
import dev.toastbits.composekit.utils.composable.*
import com.toasterofbread.spmp.platform.visualiser.MusicVisualiser
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject

private const val SIZE_DP_STEP: Float = 10f
private const val MIN_SIZE_DP: Float = 10f

@Serializable
data class ContentBarElementConfig(
    val size_mode: ContentBarElement.SizeMode = ContentBarElement.SizeMode.STATIC,
    val size: Int = 50,
    val hide_bar_when_empty: Boolean = false
)

@Serializable
sealed class ContentBarElement {
    abstract val config: ContentBarElementConfig
    abstract fun getType(): Type

    protected abstract fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement

    fun shouldFillLength(): Boolean = config.size_mode == SizeMode.FILL

    @Composable
    open fun isSelected(): Boolean = false

    @Composable
    open fun shouldShow(): Boolean = true

    @Composable
    open fun isDisplaying(): Boolean = true

    open fun blocksIndicatorAnimation(): Boolean = false

    open fun getContainedBars(): List<ContentBarReference> = emptyList()

    @Composable
    fun Element(
        vertical: Boolean,
        slot: LayoutSlot?,
        bar_size: DpSize,
        modifier: Modifier = Modifier,
        onPreviewClick: (() -> Unit)? = null
    ) {
        val size_dp: Dp? =
            when (config.size_mode) {
                SizeMode.FILL -> null
                SizeMode.STATIC -> config.size.dp
                SizeMode.PERCENTAGE -> (if (vertical) bar_size.height else bar_size.width) * config.size * 0.01f
            }

        ElementContent(
            vertical,
            slot,
            bar_size,
            onPreviewClick,
            modifier
                .thenWith(size_dp) {
                    if (vertical) height(it)
                    else width(it)
                }
                .run {
                    if (vertical) width(bar_size.height)
                    else height(bar_size.height)
                }
        )
    }

    @Composable
    protected abstract fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier)

    @Composable
    open fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {}

    @Composable
    fun ConfigurationItems(onModification: (ContentBarElement) -> Unit) {
        var show_mode_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            show_mode_selector,
            onDismissRequest = {
                show_mode_selector = false
            },
            SizeMode.entries.size,
            config.size_mode.ordinal,
            {
                Text(SizeMode.entries[it].getName())
            }
        ) {
            onModification(
                copyWithConfig(config.copy(
                    size_mode = SizeMode.entries[it], size = 50
                ))
            )
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
                Text(config.size_mode.getName())
            }
        }

        if (config.size_mode != SizeMode.FILL) {
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
                            copyWithConfig(config.copy(
                                size = (config.size - config.size_mode.getStep()).coerceAtLeast(10)
                            ))
                        )
                    }) {
                        Icon(Icons.Default.Remove, null)
                    }

                    Text(
                        if (config.size_mode == SizeMode.STATIC) "${config.size}dp"
                        else "${config.size}%"
                    )

                    IconButton({
                        var new_size: Int = config.size + config.size_mode.getStep()
                        if (config.size_mode == SizeMode.PERCENTAGE) {
                            new_size = new_size.coerceAtMost(100)
                        }

                        onModification(
                            copyWithConfig(config.copy(
                                size = new_size
                            ))
                        )
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("content_bar_element_builtin_config_hide_bar_when_empty"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Switch(
                config.hide_bar_when_empty,
                {
                    onModification(
                        copyWithConfig(config.copy(
                            hide_bar_when_empty = it
                        ))
                    )
                }
            )
        }

        SubConfigurationItems(Modifier.fillMaxWidth(), onModification)
    }

    enum class Type {
        BUTTON,
        SPACER,
        LYRICS,
        VISUALISER,
        PINNED_ITEMS,
        CONTENT_BAR,
        CROSSFADE;

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
                CONTENT_BAR -> getString("content_bar_element_type_content_bar")
                CROSSFADE -> getString("content_bar_element_type_crossfade")
            }

        fun getIcon(): ImageVector =
            when (this) {
                BUTTON -> Icons.Default.TouchApp
                SPACER -> Icons.Default.Expand
                LYRICS -> Icons.Default.MusicNote
                VISUALISER -> Icons.Default.Waves
                PINNED_ITEMS -> Icons.Default.PushPin
                CONTENT_BAR -> Icons.Default.TableRows
                CROSSFADE -> Icons.Default.Shuffle
            }

        fun createElement(): ContentBarElement =
            when (this) {
                BUTTON -> ContentBarElementButton()
                SPACER -> ContentBarElementSpacer()
                LYRICS -> ContentBarElementLyrics()
                VISUALISER -> ContentBarElementVisualiser()
                PINNED_ITEMS -> ContentBarElementPinnedItems()
                CONTENT_BAR -> ContentBarElementContentBar()
                CROSSFADE -> ContentBarElementCrossfade()
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
