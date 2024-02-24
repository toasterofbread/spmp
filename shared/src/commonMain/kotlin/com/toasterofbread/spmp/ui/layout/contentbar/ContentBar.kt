package com.toasterofbread.spmp.ui.layout.contentbar

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.spmp.ui.component.RowOrColumn
import com.toasterofbread.composekit.utils.common.thenIf
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.items
import com.toasterofbread.spmp.resources.getString
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.vertical
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import com.toasterofbread.composekit.utils.composable.MeasureUnconstrainedView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.IntSize
import LocalPlayerState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.toasterofbread.composekit.utils.modifier.background
import androidx.compose.foundation.background
import com.toasterofbread.composekit.utils.common.getContrasted
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.ui.theme.appHover
import androidx.compose.animation.core.tween
import com.toasterofbread.composekit.platform.composable.platformClickable
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme

@Composable
private fun Modifier.contentBarPreview(): Modifier {
    val player: PlayerState = LocalPlayerState.current
    return (
        appHover(
            button = true,
            expand = true,
            hover_scale = 0.98f,
            animation_spec = tween(200)
        )
        .background(RoundedCornerShape(20.dp)) { player.theme.background }
        .padding(10.dp)
    )
}

sealed class ContentBar {
    abstract fun getName(): String
    abstract fun getDescription(): String?
    abstract fun getIcon(): ImageVector

    @Composable
    fun Bar(slot: LayoutSlot, modifier: Modifier = Modifier) {
        Crossfade(bar_selection_state) { selection_state ->
            if (selection_state == null) {
                BarContent(slot, modifier)
                return@Crossfade
            }

            MeasureUnconstrainedView({
                val content_modifier: Modifier =
                    if (slot.is_vertical) modifier.height(1.dp)
                    else modifier.width(1.dp)
                BarContent(slot, content_modifier)
            }) { size ->
                val density: Density = LocalDensity.current

                val selector_modifier: Modifier = with (density) {
                    if (slot.is_vertical) modifier.width(size.width.toDp())
                    else modifier.height(size.height.toDp())
                }

                ContentBarSelector(
                    selection_state,
                    slot,
                    selector_modifier
                )
            }
        }
    }

    @Composable
    protected abstract fun BarContent(slot: LayoutSlot, modifier: Modifier)

    companion object {
        var bar_selection_state: BarSelectionState? by mutableStateOf(null)

        fun getDefaultPortraitSlots(): Map<String, Int> =
            mapOf(
                PortraitLayoutSlot.LOWER_TOP_BAR.name to InternalContentBar.PRIMARY.ordinal,
                PortraitLayoutSlot.ABOVE_PLAYER.name to InternalContentBar.SECONDARY.ordinal,

                // TEMP
                PortraitLayoutSlot.BELOW_PLAYER.name to InternalContentBar.NAVIGATION.ordinal
            )

        fun getDefaultLandscapeSlots(): Map<String, Int> =
            mapOf(
                LandscapeLayoutSlot.SIDE_LEFT.name to InternalContentBar.NAVIGATION.ordinal,
                LandscapeLayoutSlot.PAGE_TOP.name to InternalContentBar.PRIMARY.ordinal,
                LandscapeLayoutSlot.ABOVE_PLAYER.name to InternalContentBar.SECONDARY.ordinal,

                // TEMP
                LandscapeLayoutSlot.SIDE_RIGHT.name to InternalContentBar.SECONDARY.ordinal
            )

        fun deserialise(data: String): ContentBar {
            val internal_bar_index: Int? = data.toIntOrNull()
            if (internal_bar_index != null) {
                return InternalContentBar.getAll()[internal_bar_index]
            }

            return Json.decodeFromString<CustomContentBar>(data)
        }
    }

    interface BarSelectionState {
        val available_bars: List<Pair<ContentBar, Int>>
        fun onBarSelected(slot: LayoutSlot, bar: Pair<ContentBar, Int>?)
    }

    @Composable
    private fun ContentBarSelector(state: BarSelectionState, slot: LayoutSlot, modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current

        var show_bar_selector: Boolean by remember { mutableStateOf(false) }

        if (show_bar_selector) {
            BarSelectorPopup(
                slot = slot,
                bars = state.available_bars,
                modifier = Modifier.requiredWidth(player.screen_size.width * 0.6f)
            ) { bar ->
                show_bar_selector = false
                if (bar != null) {
                    state.onBarSelected(slot, bar)
                }
            }
        }

        CompositionLocalProvider(LocalContentColor provides player.theme.on_accent) {
            RowOrColumn(
                row = !slot.is_vertical,
                modifier.background { player.theme.accent }.padding(10.dp),
                arrangement = Arrangement.spacedBy(10.dp)
            ) { getWeightModifier ->
                BoxWithConstraints(
                    getWeightModifier(1f)
                        .then(
                            if (slot.is_vertical) Modifier.fillMaxSize()
                            else Modifier.fillMaxWidth()
                        )
                ) {
                    var text_size: IntSize by remember { mutableStateOf(IntSize.Zero) }

                    Row(
                        Modifier
                            .thenIf(slot.is_vertical) { rotate(-90f).vertical() }
                            .requiredSize(maxHeight, maxWidth)
                            .offset { with (density) {
                                IntOffset(
                                    (maxWidth - maxHeight).roundToPx() / 2,
                                    0
                                )
                            } }
                            .wrapContentHeight()
                            .onSizeChanged {
                                text_size = it
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Text(slot.getName())

                        Row(
                            Modifier
                                .platformClickable(
                                    onClick = {
                                        show_bar_selector = true
                                    },
                                    onAltClick = {
                                        state.onBarSelected(slot, null)
                                    }
                                )
                                .fillMaxWidth()
                                .weight(1f)
                                .contentBarPreview(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                                Icon(getIcon(), null)
                                Text(getName())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarSelectorPopup(
    slot: LayoutSlot,
    bars: List<Pair<ContentBar, Int>>,
    modifier: Modifier = Modifier,
    onSelected: (Pair<ContentBar, Int>?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        { onSelected(null) },
        modifier = modifier,
        containerColor = player.theme.accent,
        titleContentColor = player.theme.on_accent,
        textContentColor = player.theme.on_background,
        confirmButton = {
            Button(
                { onSelected(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = player.theme.background,
                    contentColor = player.theme.on_background
                ),
                modifier = Modifier.appHover(true)
            ) {
                Text(getString("action_cancel"))
            }
        },
        title = {
            Text(getString("layout_slot_content_bar_selection"))
        },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bars) { bar ->
                    ContentBarPreview(
                        bar.first,
                        Modifier
                            .contentBarPreview()
                            .platformClickable(onClick = { onSelected(bar) })
                    )
                }
            }
        }
    )
}

@Composable
private fun ContentBarPreview(bar: ContentBar, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(bar.getIcon(), null)

        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text(bar.getName(), style = MaterialTheme.typography.titleLarge)

            bar.getDescription()?.also { description ->
                Text(description, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
