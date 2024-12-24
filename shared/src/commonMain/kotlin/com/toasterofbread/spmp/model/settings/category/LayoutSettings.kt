package com.toasterofbread.spmp.model.settings.category

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLayoutCategoryItems
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlotEditorPreviewOptions
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.thenWith
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.layout_editor_preview_options
import spmp.shared.generated.resources.s_cat_desc_layout
import spmp.shared.generated.resources.s_cat_layout

class LayoutSettings(val context: AppContext): SettingsGroupImpl("LAYOUT", context.getPrefs()) {
        // // Map of LayoutSlot to ContentBarReference?
        // PORTRAIT_SLOTS,
        // LANDSCAPE_SLOTS,

        // // Map of LayoutSlot to string where values are either:
        // // - Hex colours starting with '#'
        // // - Or an integer index of Theme.Colour
        // SLOT_COLOURS,

        // // Map of LayoutSlot to slot-specific configuration
        // SLOT_CONFIGS,

        // // List of serialised CustomBars
        // CUSTOM_BARS;

    val PORTRAIT_SLOTS: PlatformSettingsProperty<Map<String, ContentBarReference?>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val LANDSCAPE_SLOTS: PlatformSettingsProperty<Map<String, ContentBarReference?>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val SLOT_COLOURS: PlatformSettingsProperty<Map<String, ColourSource>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val SLOT_CONFIGS: PlatformSettingsProperty<Map<String, JsonElement>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val CUSTOM_BARS: PlatformSettingsProperty<List<CustomContentBar>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyList() }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_layout)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_layout)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.VerticalSplit

    override fun getConfigurationItems(): List<SettingsItem> = getLayoutCategoryItems(context)

    @Composable
    override fun titleBarEndContent(modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current
        var show_preview_options: Boolean by remember { mutableStateOf(false) }

        var button_size: DpSize? by remember { mutableStateOf(null) }
        var options_height: Int by remember { mutableStateOf(0) }

        Box(
            modifier.thenWith(button_size) {
                requiredSize(it)
            }
        ) {
            Button(
                { show_preview_options = !show_preview_options },
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .onSizeChanged {
                        button_size = with(density) {
                            DpSize(it.width.toDp(), it.height.toDp())
                        }
                    }
            ) {
                Icon(Icons.Default.RemoveRedEye, null, Modifier.padding(end = 10.dp))
                Text(stringResource(Res.string.layout_editor_preview_options))
            }

            AnimatedVisibility(
                show_preview_options,
                Modifier
                    .wrapContentSize(unbounded = true)
                    .offset {
                        with (density) {
                            val button_height: Int = button_size?.height?.roundToPx() ?: 0
                            IntOffset(
                                0,
                                ((options_height + button_height) / 2) + 10.dp.roundToPx()
                            )
                        }
                    },
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                val shape: Shape = RoundedCornerShape(16.dp)
                LayoutSlotEditorPreviewOptions(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .onSizeChanged {
                            options_height = it.height
                        }
                        .platformClickable(onClick = {})
                        .background(player.theme.background, shape)
                        .border(1.dp, player.theme.vibrantAccent, shape)
                        .padding(20.dp)
                )
            }
        }
    }
}
