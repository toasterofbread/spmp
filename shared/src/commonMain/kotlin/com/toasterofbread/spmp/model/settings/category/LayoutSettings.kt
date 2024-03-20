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
import com.toasterofbread.composekit.settings.ui.SettingsPage
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.utils.common.thenWith
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLayoutCategoryItems
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlotEditorPreviewOptions
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.composekit.utils.modifier.disableGestures
import com.toasterofbread.composekit.platform.composable.platformClickable

data object LayoutSettings: SettingsCategory("player") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_layout"),
            getString("s_cat_desc_layout"),
            { getLayoutCategoryItems() },
            { Icons.Outlined.VerticalSplit },
            titleBarEndContent = {
                val player: PlayerState = LocalPlayerState.current
                val density: Density = LocalDensity.current
                var show_preview_options: Boolean by remember { mutableStateOf(false) }

                var button_size: DpSize? by remember { mutableStateOf(null) }
                var options_height: Int by remember { mutableStateOf(0) }

                Box(
                    Modifier.thenWith(button_size) {
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
                        Text(getString("layout_editor_preview_options"))
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
                                .border(1.dp, player.theme.vibrant_accent, shape)
                                .padding(20.dp)
                        )
                    }
                }
            }
        )

    enum class Key: SettingsKey {
        // Map of LayoutSlot to ContentBarReference?
        PORTRAIT_SLOTS,
        LANDSCAPE_SLOTS,

        // Map of LayoutSlot to string where values are either:
        // - Hex colours starting with '#'
        // - Or an integer index of Theme.Colour
        SLOT_COLOURS,

        // List of serialised CustomBars
        CUSTOM_BARS;

        override val category: SettingsCategory get() = PlayerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                PORTRAIT_SLOTS -> "{}"
                LANDSCAPE_SLOTS -> "{}"
                SLOT_COLOURS -> "{}"
                CUSTOM_BARS -> "[]"
            } as T
    }
}
