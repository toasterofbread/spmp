package com.toasterofbread.spmp.model.state

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.settings.Settings
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.container.npAnchorToDp
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlayerStateImpl(
    settings: Settings,
    private val session_state: SessionState,
    private val ui_state: UiState,
    private val coroutine_scope: CoroutineScope,
    initial_theme_mode: ThemeMode,
    initial_swipe_sensitivity: Float
): PlayerState {
    override var hide_player: Boolean by mutableStateOf(false)
    override fun isPlayerShowing(): Boolean = session_state.session_started && !hide_player

    init {
        ui_state.app_page_state.addPageListener {
            if (np_swipe_state.targetValue != 0) {
                switchNowPlayingPage(0)
            }
        }
    }

    override var np_theme_mode: ThemeMode by mutableStateOf(initial_theme_mode)
        private set
    override var np_swipe_sensitivity: Float by mutableStateOf(initial_swipe_sensitivity)
        private set

    override var np_bottom_bar_config: LayoutSlot.BelowPlayerConfig? by mutableStateOf(null)
    override var np_bottom_bar_showing: Boolean by mutableStateOf(false)

    private var _np_bottom_bar_height: Dp by mutableStateOf(0.dp)
    override var np_bottom_bar_height: Dp
        get() =
            if (!np_bottom_bar_showing) 0.dp
            else _np_bottom_bar_height
        set(value) { _np_bottom_bar_height = value }

    private val np_overlay_menu_queue: MutableList<PlayerOverlayMenu> = mutableListOf()
    override var np_overlay_menu: PlayerOverlayMenu? by mutableStateOf(null)

    override val expansion: PlayerExpansionState =
        object : PlayerExpansionState(this, ui_state) {
            override val swipe_state: AnchoredDraggableState<Int>
                get() = np_swipe_state
        }

    override fun switchNowPlayingPage(page: Int) {
        coroutine_scope.launch {
            np_swipe_state.animateTo(page)
        }
    }

    override fun navigateNpOverlayMenuBack() {
        np_overlay_menu = np_overlay_menu_queue.removeLastOrNull()
    }

    override fun openNpOverlayMenu(menu: PlayerOverlayMenu?) {
        if (menu == null) {
            np_overlay_menu = null
            np_overlay_menu_queue.clear()
            return
        }

        np_overlay_menu?.also {
            np_overlay_menu_queue.add(it)
        }
        np_overlay_menu = menu
        expansion.scrollTo(1)
    }

    override fun getNowPlayingExpansionOffset(density: Density): Dp =
        -np_swipe_state.offset.npAnchorToDp(density, np_swipe_sensitivity)

    @Composable
    override fun nowPlayingTopOffset(
        base: Modifier,
        section: NowPlayingTopOffsetSection,
        apply_spacing: Boolean,
        displaying: Boolean
    ): Modifier {
        val density: Density = LocalDensity.current
        val system_insets: WindowInsets = WindowInsets.systemBars
        val navigation_insets: WindowInsets = WindowInsets.navigationBars
        val keyboard_insets: WindowInsets = WindowInsets.ime

        val offset_items: MutableList<TopOffsetItem?> = remember (section) {
            now_playing_top_offset_items.getOrPut(section) {
                mutableStateListOf()
            }
        }
        val index: Int = remember(offset_items) {
            offset_items.add(null)
            return@remember offset_items.size - 1
        }

        OnChangedEffect(displaying) {
            val item: TopOffsetItem? = offset_items.getOrNull(index)
            if (item != null) {
                offset_items[index] = item.copy(
                    displaying = displaying
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                offset_items[index] = null
            }
        }

        val additional_offset: Dp by animateDpAsState(
            getTopItemsHeight(filter = { item_section, item_index ->
                !section.shouldIgnoreSection(item_section)
                && (
                    item_section.ordinal > section.ordinal
                    || (item_section.ordinal == section.ordinal && item_index < index)
                )
            })
        )

        return base
            .offset {
                val bottom_padding: Int = getNpBottomPadding(system_insets, navigation_insets, keyboard_insets)
                val swipe_offset: Dp =
                    if (isPlayerShowing()) -np_swipe_state.offset.npAnchorToDp(density, np_swipe_sensitivity) - np_bottom_bar_height// - ((screen_size.height + np_bottom_bar_height) * 0.5f)
                    else -np_bottom_bar_height

                IntOffset(
                    0,
                    swipe_offset.notUnspecified().roundToPx() - bottom_padding - additional_offset.notUnspecified().roundToPx() + 1 // Avoid single-pixel gap
                )
            }
            .padding(start = system_insets.getStart(), end = system_insets.getEnd())
            .onSizeChanged {
                with (density) {
                    offset_items[index] = TopOffsetItem(
                        height = it.height.toDp(),
                        apply_spacing = apply_spacing,
                        displaying = displaying
                    )
                }
            }
    }

    @Composable
    override fun nowPlayingBottomPadding(
        include_np: Boolean,
        include_top_items: Boolean
    ): Dp {
        var bottom_padding: Dp =
            with(LocalDensity.current) {
                LocalDensity.current.getNpBottomPadding(WindowInsets.systemBars, WindowInsets.navigationBars, WindowInsets.ime).toDp()
            }

        if (include_np) {
            bottom_padding += animateDpAsState(
                np_bottom_bar_height
                + (
                    if (isPlayerShowing()) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp
                    else 0.dp
                )
            ).value
        }
        if (include_top_items) {
            bottom_padding += animateDpAsState(
                getTopItemsHeight()
            ).value
        }

        return bottom_padding
    }

    private fun Density.getNpBottomPadding(system_insets: WindowInsets, navigation_insets: WindowInsets, keyboard_insets: WindowInsets?): Int {
        val ime_padding: Int =
            if (keyboard_insets == null || np_overlay_menu != null) 0
            else keyboard_insets.getBottom(this).let { ime ->
                if (ime > 0) {
                    val nav = navigation_insets.getBottom(this@getNpBottomPadding)
                    return@let ime.coerceAtMost(
                        (ime - nav).coerceAtLeast(0)
                    )
                }
                return@let ime
            }

        return system_insets.getBottom(this) + ime_padding
    }


    private fun getTopItemsHeight(spacing: Dp = 15.dp, filter: ((NowPlayingTopOffsetSection, Int) -> Boolean)? = null): Dp =
        now_playing_top_offset_items.entries.sumOf { items ->
            var acc: Float = 0f

            for (item in items.value.withIndex()) {
                if (item.value?.displaying != true) {
                    continue
                }

                if (filter?.invoke(items.key, item.index) == false) {
                    continue
                }

                val height: Float =
                    (item.value!!.height + (if (item.value!!.apply_spacing) spacing else 0.dp)).value

                if (items.key.isMerged()) {
                    acc = maxOf(acc, height)
                }
                else {
                    acc += height
                }
            }

            // https://youtrack.jetbrains.com/issue/KT-43310/Add-sumOf-with-Float-return-type
            return@sumOf acc.toDouble()
        }.dp

    private var np_swipe_state: AnchoredDraggableState<Int> by mutableStateOf(createSwipeState())
    private var now_playing_top_offset_items: MutableMap<NowPlayingTopOffsetSection, MutableList<TopOffsetItem?>> = mutableStateMapOf()
    private data class TopOffsetItem(
        val height: Dp,
        val apply_spacing: Boolean = true,
        val displaying: Boolean = true
    )

    private fun createSwipeState(
        anchors: DraggableAnchors<Int> = DraggableAnchors {},
        snap_animation_spec: AnimationSpec<Float> = tween(),
        decay_animation_spec: DecayAnimationSpec<Float> = exponentialDecay()
    ): AnchoredDraggableState<Int> =
        AnchoredDraggableState(
            initialValue = 0,
            anchors = anchors,
            positionalThreshold = { total_distance ->
                total_distance * 0.2f
            },
            velocityThreshold = {
                1f
            },
            snapAnimationSpec = snap_animation_spec,
            decayAnimationSpec = decay_animation_spec
        )

    private val prefs_listner: PlatformPreferencesListener =
        PlatformPreferencesListener { prefs, key ->
            when (key) {
                settings.theme.NOWPLAYING_THEME_MODE.key -> coroutine_scope.launch {
                    np_theme_mode = settings.theme.NOWPLAYING_THEME_MODE.get()
                }
                settings.player.EXPAND_SWIPE_SENSITIVITY.key -> coroutine_scope.launch {
                    np_swipe_sensitivity = settings.player.EXPAND_SWIPE_SENSITIVITY.get()
                }
            }
        }

    init {
        settings.context.getPrefs().addListener(prefs_listner)
    }
}
