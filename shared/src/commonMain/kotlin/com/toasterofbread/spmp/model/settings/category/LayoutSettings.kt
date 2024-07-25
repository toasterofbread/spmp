package com.toasterofbread.spmp.model.settings.category

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Shape
import dev.toastbits.composekit.settings.ui.SettingsPage
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.utils.common.thenWith
import LocalAppState
import LocalTheme
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLayoutCategoryItems
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlotEditorPreviewOptions
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.modifier.disableGestures
import dev.toastbits.composekit.platform.composable.platformClickable
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.vibrant_accent
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_layout
import spmp.shared.generated.resources.s_cat_desc_layout
import spmp.shared.generated.resources.layout_editor_preview_options

class LayoutSettings(val context: AppContext): SettingsGroup("LAYOUT", context.getPrefs()) {
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

    val PORTRAIT_SLOTS: PreferencesProperty<Map<String, ContentBarReference?>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val LANDSCAPE_SLOTS: PreferencesProperty<Map<String, ContentBarReference?>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val SLOT_COLOURS: PreferencesProperty<Map<String, ColourSource>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val SLOT_CONFIGS: PreferencesProperty<Map<String, JsonElement>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )
    val CUSTOM_BARS: PreferencesProperty<List<CustomContentBar>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyList() }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_layout) },
            { stringResource(Res.string.s_cat_desc_layout) },
            { getLayoutCategoryItems(context) },
            { Icons.Outlined.VerticalSplit },
            titleBarEndContent = { modifier ->
                val theme: ThemeValues = LocalTheme.current
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
                                .background(theme.background, shape)
                                .border(1.dp, theme.vibrant_accent, shape)
                                .padding(20.dp)
                        )
                    }
                }
            }
        )
}
