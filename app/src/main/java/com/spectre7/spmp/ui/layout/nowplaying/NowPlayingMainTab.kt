package com.spectre7.spmp.ui.layout.nowplaying

import MainOverlayMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.*
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200

@Composable
fun ColumnScope.NowPlayingMainTab(
    expansion: Float,
    max_height: Float,
    thumbnail: ImageBitmap?,
    _setThumbnail: (ImageBitmap?) -> Unit,
    player: PlayerViewContext
) {
    Spacer(Modifier.requiredHeight(50.dp * expansion))

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    fun setThemeColour(value: Color?) {
        theme_colour = value
        PlayerServiceHost.status.song?.theme_colour = theme_colour
    }

    val screen_width_dp = LocalConfiguration.current.screenWidthDp.dp

    var seek_state by remember { mutableStateOf(-1f) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var accent_colour_source by remember { mutableStateOf(AccentColourSource.values()[Settings.prefs.getInt(Settings.KEY_ACCENT_COLOUR_SOURCE.name, 0)]) }
    var theme_mode by remember { mutableStateOf(ThemeMode.values()[Settings.prefs.getInt(Settings.KEY_NOWPLAYING_THEME_MODE.name, 0)]) }
    var loaded_song: Song? by remember { mutableStateOf(null) }

    val prefs_listener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Settings.KEY_ACCENT_COLOUR_SOURCE.name) {
                accent_colour_source = AccentColourSource.values()[Settings.get(Settings.KEY_ACCENT_COLOUR_SOURCE)]
            } else if (key == Settings.KEY_NOWPLAYING_THEME_MODE.name) {
                theme_mode = ThemeMode.values()[Settings.get(Settings.KEY_NOWPLAYING_THEME_MODE)]
            }
        }
    }

    DisposableEffect(Unit) {
        Settings.prefs.registerOnSharedPreferenceChangeListener(prefs_listener)
        onDispose {
            Settings.prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener)
        }
    }

    LaunchedEffect(key1 = theme_colour, key2 = accent_colour_source, key3 = theme_mode) {
        MainActivity.theme.setAccent(if (accent_colour_source == AccentColourSource.SYSTEM) null else theme_colour)

        val accent = MainActivity.theme.getAccent()

        if (theme_colour == null) {
            launch {
                MainActivity.theme.setBackground(true, null)
            }
            launch {
                MainActivity.theme.setOnBackground(true, null)
            }
        }
        else if (theme_mode == ThemeMode.BACKGROUND) {
            launch {
                MainActivity.theme.setBackground(true, accent)
            }
            launch {
                MainActivity.theme.setOnBackground(true,
                    getContrastedColour(accent)
                )
            }
        }
        else if (theme_mode == ThemeMode.ELEMENTS) {
            launch {
                MainActivity.theme.setBackground(true, null)
            }
            launch {
                MainActivity.theme.setOnBackground(true, accent.contrastAgainst(MainActivity.theme.getBackground(true)))
            }
        }
        else {
            launch {
                MainActivity.theme.setBackground(true, null)
            }
            launch {
                MainActivity.theme.setOnBackground(true, null)
            }
        }
    }

    fun loadThumbnail(song: Song) {
        _setThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.LOW)?.asImageBitmap())
        theme_palette = song.thumbnail_palette

        if (song.theme_colour != null) {
            theme_colour = song.theme_colour
        }
        else if (theme_palette != null) {
            theme_colour = MediaItem.getDefaultPaletteColour(theme_palette!!, Color.Unspecified)
            if (theme_colour!!.isUnspecified) {
                theme_colour = null
            }
        }

        loaded_song = song
    }

    LaunchedEffect(PlayerServiceHost.status.m_song, PlayerServiceHost.status.m_song?.loaded) {
        val song = PlayerServiceHost.status.song
        if (loaded_song == song) {
            return@LaunchedEffect
        }

        if (song == null || !song.loaded) {
            _setThumbnail(null)
            theme_palette = null
            theme_colour = null
            loaded_song = null
        }
        else if (song.isThumbnailLoaded(MediaItem.ThumbnailQuality.HIGH)) {
            loadThumbnail(song)
        }
        else {
            thread {
                loadThumbnail(song)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f * max(expansion, (MINIMISED_NOW_PLAYING_HEIGHT + 20f) / max_height))
    ) {

        var overlay_menu by remember { mutableStateOf<OverlayMenu?>(null) }
        var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }

        LaunchedEffect(expansion > 0f) {
            overlay_menu = null
        }

        var get_shutter_menu by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        var shutter_menu_open by remember { mutableStateOf(false) }
        LaunchedEffect(expansion >= EXPANDED_THRESHOLD) {
            shutter_menu_open = false
            overlay_menu = null
        }

        Box(Modifier.aspectRatio(1f)) {
            Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                if (image == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MainActivity.theme.getAccent())
                    }
                }
                else {
                    var image_size by remember { mutableStateOf(IntSize(1, 1)) }
                    Image(
                        image, null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5))
                            .onSizeChanged {
                                image_size = it
                            }
                            .run {
                                if (colourpick_callback == null) {
                                    this.clickable(
                                        enabled = remember { derivedStateOf { expansion == 1.0f } }.value,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (overlay_menu == null || overlay_menu!!.closeOnTap()) {
                                            overlay_menu = if (overlay_menu == null) MainOverlayMenu(
                                                { overlay_menu = it },
                                                theme_palette,
                                                { colourpick_callback = it },
                                                {
                                                    setThemeColour(it)
                                                    overlay_menu = null
                                                },
                                                screen_width_dp
                                            ) else null
                                        }
                                    }
                                }
                                else {
                                    this.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                if (colourpick_callback != null) {
                                                    val bitmap_size = min(image.width, image.height)
                                                    var x = (offset.x / image_size.width) * bitmap_size
                                                    var y = (offset.y / image_size.height) * bitmap_size

                                                    if (image.width > image.height) {
                                                        x += (image.width - image.height) / 2
                                                    }
                                                    else if (image.height > image.width) {
                                                        y += (image.height - image.width) / 2
                                                    }

                                                    colourpick_callback?.invoke(
                                                        Color(image.asAndroidBitmap().getPixel(x.toInt(), y.toInt()))
                                                    )
                                                    colourpick_callback = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                    )
                }
            }

            // Thumbnail overlay menu
            androidx.compose.animation.AnimatedVisibility(
                overlay_menu != null,
                enter = fadeIn(tween(OVERLAY_MENU_ANIMATION_DURATION)),
                exit = fadeOut(tween(OVERLAY_MENU_ANIMATION_DURATION))
            ) {

                Box(Modifier.alpha(expansion)) {
                    Box(
                        Modifier
                            .background(
                                setColourAlpha(Color.DarkGray, 0.85),
                                shape = RoundedCornerShape(5)
                            )
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(overlay_menu) { menu ->
                            if (menu != null) {
                                BackHandler {
                                    overlay_menu = null
                                    colourpick_callback = null
                                }
                            }

                            menu?.Menu(
                                PlayerServiceHost.status.song!!,
                                expansion,
                                {
                                    get_shutter_menu = it
                                    shutter_menu_open = true
                                },
                                {
                                    overlay_menu = null
                                },
                                seek_state,
                                player
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    shutter_menu_open,
                    enter = expandVertically(tween(200)),
                    exit = shrinkVertically(tween(200))
                ) {
                    val padding = 15.dp
                    val background = if (theme_mode == ThemeMode.BACKGROUND) MainActivity.theme.getBackground(false) else MainActivity.theme.getAccent()
                    CompositionLocalProvider(
                        LocalContentColor provides background.getContrasted()
                    ) {
                        Column(
                            Modifier
                                .background(
                                    background.setAlpha(0.9),
                                    RoundedCornerShape(5)
                                )
                                .padding(start = padding, top = padding, end = padding)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                get_shutter_menu?.invoke()
                            }
                            IconButton(onClick = { shutter_menu_open = false }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp, null,
                                    tint = background.getContrasted(),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(0.9f * (1f - expansion)),
            horizontalArrangement = Arrangement.End
        ) {

            Spacer(Modifier.requiredWidth(10.dp))

            Text(
                PlayerServiceHost.status.m_song?.getLoadedOrNull()?.title ?: "",
                maxLines = 1,
                color = MainActivity.theme.getOnBackground(true),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .fillMaxWidth()
            )

            AnimatedVisibility(PlayerServiceHost.status.m_has_previous, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.player.seekToPreviousMediaItem()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_previous),
                        null,
                        colorFilter = ColorFilter.tint(MainActivity.theme.getOnBackground(true))
                    )
                }
            }

            AnimatedVisibility(PlayerServiceHost.status.m_song != null, enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.service.playPause()
                    }
                ) {
                    Image(
                        painterResource(if (PlayerServiceHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        getString(if (PlayerServiceHost.status.m_playing) R.string.media_pause else R.string.media_play),
                        colorFilter = ColorFilter.tint(MainActivity.theme.getOnBackground(true))
                    )
                }
            }

            AnimatedVisibility(PlayerServiceHost.status.m_has_next, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.player.seekToNextMediaItem()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_next),
                        null,
                        colorFilter = ColorFilter.tint(MainActivity.theme.getOnBackground(true))
                    )
                }
            }
        }
    }

    if (expansion > 0.0f) {
        TODO()
        Controls(
            player,
            {
                PlayerServiceHost.player.seekTo((PlayerServiceHost.player.duration * it).toLong())
                seek_state = it
            },
            Modifier.weight(1f).recomposeHighlighter()
        )
    }
}

@Composable
private fun Controls(
    player: PlayerViewContext,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(painter: Painter, size: Dp = 60.dp, alpha: Float = 1f, colourProvider: (() -> Color)? = null, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = on_click,
                        indication = rememberRipple(
                            radius = 25.dp,
                            bounded = false
                        ),
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = enabled
                    )
                    .alpha(if (enabled) 1.0f else 0.5f)
            ) {
                val colour = remember (colourProvider) { colourProvider?.invoke() ?: MainActivity.theme.getOnBackground(true) }
                Image(
                    painter, null,
                    Modifier
                        .requiredSize(size, 60.dp)
                        .offset(y = if (label != null) (-7).dp else 0.dp),
                    colorFilter = ColorFilter.tint(colour),
                    alpha = alpha
                )
                if (label != null) {
                    Text(label, color = colour, fontSize = 10.sp, modifier = Modifier.offset(y = (10).dp))
                }
            }
        }

        @Composable
        fun PlayerButton(image_id: Int, size: Dp = 60.dp, alpha: Float = 1f, colourProvider: (() -> Color)? = null, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
            PlayerButton(painterResource(image_id), size, alpha, colourProvider, label, enabled, on_click)
        }

        Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                val song = PlayerServiceHost.status.m_song?.getLoadedOrNull() as Song?

                // Title text
                Marquee(false) {
                    Text(
                        song?.title ?: "",
                        fontSize = 17.sp,
                        color = MainActivity.theme.getOnBackground(true),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clickable {
                                TODO("Edit song info")
                            }
                    )
                }

                // Artist text
                Text(
                    song?.artist?.getLoadedOrNull()?.title ?: "",
                    fontSize = 12.sp,
                    color = MainActivity.theme.getOnBackground(true),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable {
                            if (PlayerServiceHost.status.song?.loaded == true) {
                                player.onMediaItemClicked(PlayerServiceHost.status.song!!.artist)
                            }
                        }
                )
            }

            SeekBar(seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {

                val utility_separation = 25.dp

                // Toggle shuffle
                PlayerButton(R.drawable.ic_shuffle, 60.dp * 0.65f, if (PlayerServiceHost.status.m_shuffle) 1f else 0.25f) {
                    PlayerServiceHost.player.shuffleModeEnabled = !PlayerServiceHost.player.shuffleModeEnabled
                }

                Spacer(Modifier.requiredWidth(utility_separation))

                // Previous
                PlayerButton(R.drawable.ic_skip_previous, enabled = PlayerServiceHost.status.m_has_previous) {
                    PlayerServiceHost.player.seekToPreviousMediaItem()
                }

                // Play / pause
                PlayerButton(if (PlayerServiceHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, enabled = PlayerServiceHost.status.m_song != null) {
                    PlayerServiceHost.service.playPause()
                }

                // Next
                PlayerButton(R.drawable.ic_skip_next, enabled = PlayerServiceHost.status.m_has_next) {
                    PlayerServiceHost.player.seekToNextMediaItem()
                }

                Spacer(Modifier.requiredWidth(utility_separation))

                // Cycle repeat mode
                PlayerButton(
                    if (PlayerServiceHost.status.m_repeat_mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                    60.dp * 0.65f,
                    if (PlayerServiceHost.status.m_repeat_mode != Player.REPEAT_MODE_OFF) 1f else 0.25f
                ) {
                    PlayerServiceHost.player.repeatMode = when (PlayerServiceHost.player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_ALL
                    }
                }
            }
        }
    }
}
