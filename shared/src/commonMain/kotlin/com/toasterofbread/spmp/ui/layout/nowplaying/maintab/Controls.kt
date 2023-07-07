package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

@Composable
private fun Controls(
    song: Song?,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        show_title_edit_dialog = false
    }

    if (show_title_edit_dialog && song != null) {
        MediaItemTitleEditDialog(song) { show_title_edit_dialog = false }
    }

    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(
            image: ImageVector,
            size: Dp = 60.dp,
            enabled: Boolean = true,
            onClick: () -> Unit
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = enabled
                    )
                    .alpha(if (enabled) 1.0f else 0.5f)
            ) {
                val painter = rememberVectorPainter(image)

                Canvas(
                    Modifier
                        .requiredSize(size)
                        // https://stackoverflow.com/a/67820996
                        .graphicsLayer { alpha = 0.99f }
                ) {
                    with(painter) {
                        draw(this@Canvas.size)
                    }

                    val gradient_end = this@Canvas.size.width * 1.7f
                    drawRect(
                        Brush.linearGradient(
                            listOf(getNPOnBackground(), getNPBackground()),
                            end = Offset(gradient_end, gradient_end)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(25.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                Marquee(Modifier.fillMaxWidth()) {
                    Text(
                        song?.title ?: "",
                        fontSize = 20.sp,
                        color = getNPOnBackground(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // TODO Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .platformClickable(
                                onAltClick = {
                                    show_title_edit_dialog = !show_title_edit_dialog
                                    SpMp.context.vibrateShort()
                                }
                            )
                    )
                }

                Text(
                    song?.artist?.title ?: "",
                    fontSize = 12.sp,
                    color = getNPOnBackground(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .platformClickable(
                            onClick = {
                                if (song?.artist?.is_for_item == false) {
                                    player.status.m_song?.artist?.also {
                                        player.onMediaItemClicked(it)
                                    }
                                }
                            },
                            onAltClick = {
                                if (song?.artist?.is_for_item == false) {
                                    player.status.m_song?.artist?.also {
                                        player.onMediaItemLongClicked(it)
                                        SpMp.context.vibrateShort()
                                    }
                                }
                            }
                        )
                )
            }

            SeekBar(seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous
                PlayerButton(
                    Icons.Rounded.SkipPrevious,
                    enabled = player.status.m_has_previous,
                    size = 60.dp
                ) {
                    player.player?.seekToPrevious()
                }

                // Play / pause
                PlayerButton(
                    if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    enabled = song != null,
                    size = 75.dp
                ) {
                    player.player?.playPause()
                }

                // Next
                PlayerButton(
                    Icons.Rounded.SkipNext,
                    enabled = player.status.m_has_next,
                    size = 60.dp
                ) {
                    player.player?.seekToNext()
                }
            }
            
            Spacer(Modifier.fillMaxHeight().weight(2f))

            var volume_slider_visible by remember { mutableStateOf(false) }
            val bottom_row_content_colour = getNPOnBackground().setAlpha(0.5f)

            AnimatedVisibility(
                volume_slider_visible, 
                Modifier.fillMaxSize().weight(1f),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd)
                VolumeSlider(bottom_row_content_colour)
            }
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    { volume_slider_visible = !volume_slider_visible }
                ) {
                    Icon(Icons.Filled.VolumeUp, null, tint = bottom_row_content_colour)
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(colour: Color, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    SliderValueVertical(
        value = player.status.m_volume,
        onValueChange = {
            player.player?.volume = it
        },
        thumbSizeInDp = DpSize(12.dp, 12.dp),
        track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, colour.setAlpha(0.5f), colour) },
        thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, colour, 1f) },
        modifier = modifier
    )
}

