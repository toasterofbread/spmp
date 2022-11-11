package com.spectre7.spmp.ui.layout.nowplaying

import androidx.compose.runtime.Composable

enum class NowPlayingOverlayMenu { NONE, MAIN, PALETTE, LYRICS, DOWNLOAD }

const val OVERLAY_MENU_ANIMATION_DURATION = 0.2f

@Composable
fun MainTab() {
    Spacer(Modifier.requiredHeight(50.dp * expansion))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f * max(expansion, (MINIMISED_NOW_PLAYING_HEIGHT + 20f) / max_height))
    ) {

        var overlay_menu by remember { mutableStateOf(NowPlayingOverlayMenu.NONE) }

        LaunchedEffect(expansion == 0.0f) {
            overlay_menu = NowPlayingOverlayMenu.NONE
        }

        Box(Modifier.aspectRatio(1f)) {
            Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                if (image != null) {
                    Image(
                        image, "",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5))
                            .clickable(
                                enabled = expansion == 1.0f,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // TODO Make this less hardcoded
                                if (overlay_menu == NowPlayingOverlayMenu.NONE || overlay_menu == NowPlayingOverlayMenu.MAIN || overlay_menu == NowPlayingOverlayMenu.PALETTE) {
                                    overlay_menu =
                                        if (overlay_menu == NowPlayingOverlayMenu.NONE) NowPlayingOverlayMenu.MAIN else NowPlayingOverlayMenu.NONE
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

                Box(
                    Modifier
                        .background(
                            setColourAlpha(Color.DarkGray, 0.85),
                            shape = RoundedCornerShape(5)
                        )
                        .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Crossfade(overlay_menu) { menu ->
                        when (menu) {
                            NowPlayingOverlayMenu.MAIN ->
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {

                                    PlayerHost.status.m_song?.artist?.Preview(false)

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                                        val button_modifier = Modifier
                                            .background(
                                                MainActivity.theme.getAccent(),
                                                CircleShape
                                            )
                                            .size(40.dp)
                                            .padding(8.dp)
                                        val button_colour = ColorFilter.tint(MainActivity.theme.getOnAccent())

                                        Box(
                                            button_modifier.clickable { overlay_menu = NowPlayingOverlayMenu.LYRICS }
                                        ) {
                                            Image(
                                                painterResource(R.drawable.ic_music_note), "",
                                                colorFilter = button_colour
                                            )
                                        }

                                        Box(
                                            button_modifier.clickable { overlay_menu = NowPlayingOverlayMenu.PALETTE }
                                        ) {
                                            Image(
                                                painterResource(R.drawable.ic_palette), "",
                                                colorFilter = button_colour
                                            )
                                        }

                                        Box(
                                            button_modifier.clickable { overlay_menu = NowPlayingOverlayMenu.DOWNLOAD }
                                        ) {
                                            Image(
                                                painterResource(R.drawable.ic_download), "",
                                                colorFilter = button_colour
                                            )
                                        }
                                    }
                                }
                            NowPlayingOverlayMenu.PALETTE ->
                                PaletteSelector(theme_palette) { index, _ ->
                                    setThemeColour(getPaletteColour(theme_palette!!, index))
                                    overlay_menu = NowPlayingOverlayMenu.NONE
                                }
                            NowPlayingOverlayMenu.LYRICS ->
                                if (PlayerHost.status.m_song != null) {
                                    LyricsDisplay(PlayerHost.status.song!!, { overlay_menu = NowPlayingOverlayMenu.NONE }, (screen_width_dp - (main_padding * 2) - (15.dp * expansion * 2)).value * 0.9.dp, seek_state) {
                                        get_shutter_menu = it
                                        shutter_menu_open = true
                                    }
                                }
                            NowPlayingOverlayMenu.DOWNLOAD ->
                                if (PlayerHost.status.m_song != null) {
                                    DownloadMenu(PlayerHost.status.song!!) { overlay_menu = NowPlayingOverlayMenu.NONE }
                                }
                            NowPlayingOverlayMenu.NONE -> {}
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
                                    Icons.Filled.KeyboardArrowUp, "",
                                    tint = background.getContrasted(),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(inv_expansion * 0.9f)) {

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
                        "",
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
                        "",
                        colorFilter = colour_filter
                    )
                }
            }
        }
    }

    if (expansion > 0.0f) {
        Spacer(Modifier.requiredHeight(30.dp))

        Box(
            Modifier
                .alpha(expansion)
                .weight(1f), contentAlignment = Alignment.TopCenter) {

            @Composable
            fun PlayerButton(painter: Painter, size: Dp = button_size, alpha: Float = 1f, colour: Color = MainActivity.theme.getOnBackground(true), label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
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
                        painter, "",
                        Modifier
                            .requiredSize(size, button_size)
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
            fun PlayerButton(image_id: Int, size: Dp = button_size, alpha: Float = 1f, colour: Color = MainActivity.theme.getOnBackground(true), label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
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
                    PlayerButton(R.drawable.ic_shuffle, button_size * 0.65f, if (PlayerHost.status.m_shuffle) 1f else 0.25f) {
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
                        button_size * 0.65f,
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
