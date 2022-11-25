package com.spectre7.spmp.ui.layout.nowplaying

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
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
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.R
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DownloadMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.EditMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.LyricsDisplay
import com.spectre7.spmp.ui.layout.nowplaying.overlay.PaletteSelectorMenu
import com.spectre7.utils.*
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

enum class NowPlayingOverlayMenu { NONE, MAIN, PALETTE, LYRICS, DOWNLOAD, EDIT }

const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200

@Composable
fun MainTab(weight_modifier: Modifier, expansion: Float, max_height: Float, thumbnail: ImageBitmap?, _setThumbnail: (ImageBitmap?) -> Unit) {
    Spacer(Modifier.requiredHeight(50.dp * expansion))

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    fun setThemeColour(value: Color?) {
        theme_colour = value
        PlayerHost.status.song?.theme_colour = theme_colour
    }

    val screen_width_dp = LocalConfiguration.current.screenWidthDp.dp

    val colour_filter = ColorFilter.tint(MainActivity.theme.getOnBackground(true))
    var seek_state by remember { mutableStateOf(-1f) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var accent_colour_source by remember { mutableStateOf(AccentColourSource.values()[MainActivity.prefs.getInt("accent_colour_source", 0)]) }
    var theme_mode by remember { mutableStateOf(ThemeMode.values()[MainActivity.prefs.getInt("np_theme_mode", 0)]) }

    fun setThumbnail(thumb: ImageBitmap?, on_finished: () -> Unit) {
        _setThumbnail(thumb)
        if (thumb == null) {
            theme_palette = null
            theme_colour = null
        }
        else {
            Palette.from(thumb.asAndroidBitmap()).generate {
                theme_palette = it
                on_finished()
            }
        }
    }

    fun getSongTitle(): String {
        return PlayerHost.status.song?.title ?: "-----"
    }

    fun getSongArtist(): String {
        return PlayerHost.status.song?.artist?.name ?: "---"
    }

    val prefs_listener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "accent_colour_source") {
                accent_colour_source =
                    AccentColourSource.values()[prefs.getInt("accent_colour_source", 0)]
            } else if (key == "np_theme_mode") {
                theme_mode = ThemeMode.values()[prefs.getInt("np_theme_mode", 0)]
            }
        }
    }

    DisposableEffect(Unit) {
        MainActivity.prefs.registerOnSharedPreferenceChangeListener(prefs_listener)
        onDispose {
            MainActivity.prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener)
        }
    }

    LaunchedEffect(key1 = theme_colour, key2 = accent_colour_source, key3 = theme_mode) {

        MainActivity.theme.setAccent(if (accent_colour_source == AccentColourSource.SYSTEM) null else theme_colour)

        val accent = MainActivity.theme.getAccent()

        if (theme_colour == null) {
            MainActivity.theme.setBackground(true, null)
            MainActivity.theme.setOnBackground(true, null)
        }
        else if (theme_mode == ThemeMode.BACKGROUND) {
            MainActivity.theme.setBackground(true, accent)
            MainActivity.theme.setOnBackground(true,
                getContrastedColour(accent)
            )
        }
        else if (theme_mode == ThemeMode.ELEMENTS) {
            MainActivity.theme.setBackground(true, null)
            MainActivity.theme.setOnBackground(true, accent.contrastAgainst(MainActivity.theme.getBackground(true)))
        }
        else {
            MainActivity.theme.setBackground(true, null)
            MainActivity.theme.setOnBackground(true, null)
        }
    }

    LaunchedEffect(PlayerHost.status.m_song) {
        val song = PlayerHost.status.song
        val on_finished = {
            if (song!!.theme_colour != null) {
                theme_colour = song.theme_colour
            }
            else if (theme_palette != null) {
                for (i in (2 until 5) + (0 until 2)) {
                    theme_colour = getPaletteColour(theme_palette!!, i)
                    if (theme_colour != null) {
                        break
                    }
                }
            }
        }

        if (song == null) {
            setThumbnail(null, {})
            theme_colour = null
        }
        else if (song.thumbnailLoaded(true)) {
            setThumbnail(song.loadThumbnail(true).asImageBitmap(), on_finished)
        }
        else {
            thread {
                setThumbnail(song.loadThumbnail(true).asImageBitmap(), on_finished)
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

        var overlay_menu by remember { mutableStateOf(NowPlayingOverlayMenu.NONE) }
        var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }

        LaunchedEffect(expansion > 0f) {
            overlay_menu = NowPlayingOverlayMenu.NONE
        }

        Box(Modifier.aspectRatio(1f)) {
            Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                if (image != null) {
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
                                        enabled = expansion == 1.0f,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        // TODO | Make this less hardcoded
                                        if (overlay_menu == NowPlayingOverlayMenu.NONE || overlay_menu == NowPlayingOverlayMenu.MAIN || overlay_menu == NowPlayingOverlayMenu.PALETTE) {
                                            overlay_menu =
                                                if (overlay_menu == NowPlayingOverlayMenu.NONE) NowPlayingOverlayMenu.MAIN else NowPlayingOverlayMenu.NONE
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
                overlay_menu != NowPlayingOverlayMenu.NONE,
                enter = fadeIn(tween(OVERLAY_MENU_ANIMATION_DURATION)),
                exit = fadeOut(tween(OVERLAY_MENU_ANIMATION_DURATION))
            ) {
                var get_shutter_menu by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
                var shutter_menu_open by remember { mutableStateOf(false) }

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

                            if (menu != NowPlayingOverlayMenu.NONE) {
                                BackHandler {
                                    overlay_menu = NowPlayingOverlayMenu.NONE
                                    colourpick_callback = null
                                }
                            }

                            when (menu) {
                                NowPlayingOverlayMenu.MAIN ->
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {

                                        PlayerHost.status.m_song?.artist?.Preview(false)

                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {

                                            val button_modifier = Modifier
                                                .background(
                                                    MainActivity.theme.getAccent(),
                                                    CircleShape
                                                )
                                                .size(40.dp)
                                                .padding(8.dp)
                                            val button_colour =
                                                ColorFilter.tint(MainActivity.theme.getOnAccent())

                                            Box(
                                                button_modifier.clickable {
                                                    overlay_menu = NowPlayingOverlayMenu.LYRICS
                                                }
                                            ) {
                                                Image(
                                                    painterResource(R.drawable.ic_music_note), null,
                                                    colorFilter = button_colour
                                                )
                                            }

                                            Box(
                                                button_modifier.clickable {
                                                    overlay_menu = NowPlayingOverlayMenu.PALETTE
                                                }
                                            ) {
                                                Image(
                                                    painterResource(R.drawable.ic_palette), null,
                                                    colorFilter = button_colour
                                                )
                                            }

                                            Box(
                                                button_modifier.clickable {
                                                    overlay_menu = NowPlayingOverlayMenu.DOWNLOAD
                                                }
                                            ) {
                                                Image(
                                                    painterResource(R.drawable.ic_download), null,
                                                    colorFilter = button_colour
                                                )
                                            }

                                            Box(
                                                button_modifier.clickable {
                                                    overlay_menu = NowPlayingOverlayMenu.EDIT
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    null,
                                                    tint = MainActivity.theme.getOnAccent()
                                                )
                                            }
                                        }
                                    }
                                NowPlayingOverlayMenu.PALETTE ->
                                    PaletteSelectorMenu(theme_palette, {
                                        colourpick_callback = it
                                    }) { colour ->
                                        setThemeColour(colour)
                                        overlay_menu = NowPlayingOverlayMenu.NONE
                                    }
                                NowPlayingOverlayMenu.LYRICS ->
                                    if (PlayerHost.status.m_song != null) {
                                        LyricsDisplay(
                                            PlayerHost.status.song!!,
                                            { overlay_menu = NowPlayingOverlayMenu.NONE },
                                            (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp,
                                            seek_state
                                        ) {
                                            get_shutter_menu = it
                                            shutter_menu_open = true
                                        }
                                    }
                                NowPlayingOverlayMenu.DOWNLOAD ->
                                    if (PlayerHost.status.m_song != null) {
                                        DownloadMenu(PlayerHost.status.song!!) {
                                            overlay_menu = NowPlayingOverlayMenu.NONE
                                        }
                                    }
                                NowPlayingOverlayMenu.EDIT ->
                                    if (PlayerHost.status.m_song != null) {
                                        EditMenu(PlayerHost.status.song!!, {
                                            get_shutter_menu = it
                                            shutter_menu_open = true
                                        }) { overlay_menu = NowPlayingOverlayMenu.NONE }
                                    }
                                NowPlayingOverlayMenu.NONE -> {}
                            }
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
                    Column(
                        Modifier
                            .background(
                                background,
                                RoundedCornerShape(5)
                            )
                            .padding(start = padding, top = padding, end = padding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides background.getContrasted()
                        ) {
                            get_shutter_menu?.invoke()
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
                getSongTitle(),
                maxLines = 1,
                color = MainActivity.theme.getOnBackground(true),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .fillMaxWidth()
            )

            AnimatedVisibility(PlayerHost.status.m_has_previous, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerHost.player.seekToPreviousMediaItem()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_previous),
                        null,
                        colorFilter = colour_filter
                    )
                }
            }

            AnimatedVisibility(PlayerHost.status.m_song != null, enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = {
                        PlayerHost.service.playPause()
                    }
                ) {
                    Image(
                        painterResource(if (PlayerHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        getString(if (PlayerHost.status.m_playing) R.string.media_pause else R.string.media_play),
                        colorFilter = colour_filter
                    )
                }
            }

            AnimatedVisibility(PlayerHost.status.m_has_next, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerHost.player.seekToNextMediaItem()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_next),
                        null,
                        colorFilter = colour_filter
                    )
                }
            }
        }
    }

    if (expansion > 0.0f) {
        Spacer(Modifier.requiredHeight(30.dp))

        Box(
            weight_modifier.alpha(expansion),
            contentAlignment = Alignment.TopCenter
        ) {

            @Composable
            fun PlayerButton(painter: Painter, size: Dp = 60.dp, alpha: Float = 1f, colour: Color = MainActivity.theme.getOnBackground(true), label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
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
            fun PlayerButton(image_id: Int, size: Dp = 60.dp, alpha: Float = 1f, colour: Color = MainActivity.theme.getOnBackground(true), label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
                PlayerButton(painterResource(image_id), size, alpha, colour, label, enabled, on_click)
            }

            Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                    // Title text
                    Text(getSongTitle(),
                        fontSize = 17.sp,
                        color = MainActivity.theme.getOnBackground(true),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize())

                    // Artist text
                    Text(getSongArtist(),
                        fontSize = 12.sp,
                        color = MainActivity.theme.getOnBackground(true),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize())

                }

                SeekBar {
                    PlayerHost.player.seekTo((PlayerHost.player.duration * it).toLong())
                    seek_state = it
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {

                    val utility_separation = 25.dp

                    // Toggle shuffle
                    PlayerButton(R.drawable.ic_shuffle, 60.dp * 0.65f, if (PlayerHost.status.m_shuffle) 1f else 0.25f) {
                        PlayerHost.player.shuffleModeEnabled = !PlayerHost.player.shuffleModeEnabled
                    }

                    Spacer(Modifier.requiredWidth(utility_separation))

                    // Previous
                    PlayerButton(R.drawable.ic_skip_previous, enabled = PlayerHost.status.m_has_previous) {
                        PlayerHost.player.seekToPreviousMediaItem()
                    }

                    // Play / pause
                    PlayerButton(if (PlayerHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, enabled = PlayerHost.status.m_song != null) {
                        PlayerHost.service.playPause()
                    }

                    // Next
                    PlayerButton(R.drawable.ic_skip_next, enabled = PlayerHost.status.m_has_next) {
                        PlayerHost.player.seekToNextMediaItem()
                    }

                    Spacer(Modifier.requiredWidth(utility_separation))

                    // Cycle repeat mode
                    PlayerButton(
                        if (PlayerHost.status.m_repeat_mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                        60.dp * 0.65f,
                        if (PlayerHost.status.m_repeat_mode != Player.REPEAT_MODE_OFF) 1f else 0.25f
                    ) {
                        PlayerHost.player.repeatMode = when (PlayerHost.player.repeatMode) {
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                            else -> Player.REPEAT_MODE_ALL
                        }
                    }
                }
            }
        }
    }
}
