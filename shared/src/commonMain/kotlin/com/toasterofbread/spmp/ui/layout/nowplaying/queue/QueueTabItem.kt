package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.modifier.background
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.ytmkt.uistrings.durationToString
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_song_now_playing
import spmp.shared.generated.resources.`lpm_song_played_$x_ago`
import spmp.shared.generated.resources.`lpm_song_playing_in_$x`

@Composable
fun TouchSlopScope(getTouchSlop: ViewConfiguration.() -> Float, content: @Composable (ViewConfiguration) -> Unit) {
    val view_configuration: ViewConfiguration = LocalViewConfiguration.current
    CompositionLocalProvider(
        remember {
            LocalViewConfiguration provides object : ViewConfiguration {
                override val doubleTapMinTimeMillis get() = view_configuration.doubleTapMinTimeMillis
                override val doubleTapTimeoutMillis get() = view_configuration.doubleTapTimeoutMillis
                override val longPressTimeoutMillis get() = view_configuration.longPressTimeoutMillis
                override val touchSlop: Float get() = getTouchSlop(view_configuration)
            }
        }
    ) {
        content(view_configuration)
    }
}

class QueueTabItem(val song: Song, val key: Int) {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun queueElementSwipeState(requestRemove: () -> Unit, max_offset: Float): AnchoredDraggableState<Int> {
        val density: Density = LocalDensity.current
        val swipe_state: AnchoredDraggableState<Int> = remember {
            AnchoredDraggableState(
                initialValue = 1,
                anchors = DraggableAnchors {
                    0 at -max_offset
                    1 at 0f
                    2 at max_offset
                },
                positionalThreshold = { it * 0.2f },
                velocityThreshold = { with (density) { 100.dp.toPx() } },
                snapAnimationSpec = tween(),
                decayAnimationSpec = exponentialDecay()
            )
        }
        var removed by remember { mutableStateOf(false) }

        LaunchedEffect(remember { derivedStateOf { swipe_state.progress > 0.8f } }.value) {
            if (!removed && swipe_state.targetValue != 1 && swipe_state.progress > 0.8f) {
                requestRemove()
                removed = true
            }
        }

        return swipe_state
    }

    @Composable
    private fun getLPMTitle(index: Int): String? {
        val player = LocalPlayerState.current
        val playing_index = player.status.m_index
        if (index == playing_index) {
            return stringResource(Res.string.lpm_song_now_playing)
        }

        val service = player.controller ?: return null

        var delta = 0L
        val indices = if (index < playing_index) index + 1 .. playing_index else playing_index until index
        for (i in indices) {
            val duration =
                service.getSong(i)?.Duration?.observe(player.database)?.value
                ?: return null
            delta += duration
        }

        val `lpm_song_played_$x_ago`: String = stringResource(Res.string.`lpm_song_played_$x_ago`)
        val `lpm_song_playing_in_$x`: String = stringResource(Res.string.`lpm_song_playing_in_$x`)
        val ui_language: Locale by player.context.observeUiLanguage()

        return remember(delta, ui_language) {
            (
                if (index < playing_index) `lpm_song_played_$x_ago`
                else `lpm_song_playing_in_$x`
            ).replace("\$x", durationToString(delta, ui_language.toTag(), true))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun QueueElement(
        list_state: ReorderableLazyListState,
        index: Int,
        getBackgroundColour: () -> Color,
        multiselect_context: MediaItemMultiSelectContext,
        requestRemove: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

        val max_offset: Float = with(LocalDensity.current) { player.screen_size.width.toPx() }
        val swipe_state: AnchoredDraggableState<Int> = queueElementSwipeState(requestRemove, max_offset)
        val swipe_sensitivity: Float by player.settings.Player.QUEUE_ITEM_SWIPE_SENSITIVITY.observe()

        TouchSlopScope({
            touchSlop * 2f * (2.1f - swipe_sensitivity)
        }) { parent_view_configuration ->
            Row(
                Modifier
                    .offset { IntOffset(swipe_state.offset.roundToInt(), 0) }
                    .background(MaterialTheme.shapes.extraLarge, getBackgroundColour)
                    .padding(start = 10.dp, end = 10.dp)
                    .thenIf(Platform.DESKTOP.isCurrent()) {
                        detectReorder(list_state)
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalPlayerClickOverrides provides
                    if (Platform.DESKTOP.isCurrent())
                        click_overrides.copy(
                            onClickOverride = { _, _ -> }
                        )
                    else click_overrides
                ) {
                    MediaItemPreviewLong(
                        song,
                        Modifier
                            .weight(1f)
                            .padding(top = 5.dp, bottom = 5.dp)
                            .thenIf(Platform.ANDROID.isCurrent()) {
                                anchoredDraggable(
                                    swipe_state,
                                    Orientation.Horizontal
                                )
                            },
                        contentColour = { getBackgroundColour().getContrasted() },
                        show_type = false,
                        multiselect_context = multiselect_context,
                        multiselect_key = index,
                        queue_index = index,
                        getTitle = { getLPMTitle(index) }
                    )
                }

                val radio_item_index: Int? = player.controller?.radio_instance?.state?.item_queue_index
                if (radio_item_index == index) {
                    Icon(Icons.Default.Radio, null, Modifier.size(20.dp))
                }

                Platform.ANDROID.only {
                    TouchSlopScope({ parent_view_configuration.touchSlop }) {
                        // Drag handle
                        Icon(
                            Icons.Default.Menu,
                            null,
                            Modifier
                                .detectReorder(list_state)
                                .requiredSize(25.dp)
                                .appHover(true),
                            tint = getBackgroundColour().getContrasted()
                        )
                    }
                }

                Platform.DESKTOP.only {
                    Row(Modifier.alpha(0.8f)) {
                        IconButton({
                            click_overrides.onMediaItemClicked(song, player, multiselect_key = index)
                        }) {
                            Icon(Icons.Default.PlayArrow, null)
                        }

                        IconButton(requestRemove) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    }
}
