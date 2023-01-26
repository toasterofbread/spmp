package com.spectre7.spmp.ui.component

class LongPressMenuActionProvider {
    companion object {
        @Composable
        fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
            Row(modifier.clickable(onClick = onClick), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(icon, null)
                Text(label, fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LongPressIconMenu(
    showing: Boolean,
    onDismissRequest: () -> Unit,
    media_item: MediaItem,
    _thumb_size: Dp,
    thumb_shape: Shape,
    actions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit,
    onShown: () -> Unit
) {
    var hide_thumb by remember { mutableStateOf(false) }
    var thumb_position: Offset? by remember { mutableStateOf(null) }
    var thumb_size: IntSize? by remember { mutableStateOf(null) }

    Image(
        painter = rememberAsyncImagePainter(media_item.getThumbUrl(false)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(_thumb_size)
            .clip(thumb_shape)
            .onGloballyPositioned {
                thumb_position = it.positionInWindow()
            }
            .onSizeChanged {
                thumb_size = it
            }
            .alpha(if (hide_thumb) 0f else 1f)
    )

    if (show_popup && thumb_position != null && thumb_size != null) {
        val density = LocalDensity.current
        val status_bar_height = getStatusBarHeight(MainActivity.context)

        val initial_pos = remember { with (density) { DpOffset(thumb_position!!.x.toDp(), thumb_position!!.y.toDp() - status_bar_height) } }
        val initial_size = remember { with (density) { DpSize(thumb_size!!.width.toDp(), thumb_size!!.height.toDp()) } }

        val anim_duration = 200
        var fully_open by remember { mutableStateOf(false) }

        val pos = remember { Animatable(initial_pos, DpOffset.VectorConverter) }
        val width = remember { Animatable(initial_size.width.value) }
        val height = remember { Animatable(initial_size.height.value) }
        val panel_alpha = remember { Animatable(1f) }

        var target_position: Offset? by remember { mutableStateOf(null) }
        var target_size: IntSize? by remember { mutableStateOf(null) }

        suspend fun animateValues(to_target: Boolean) {

            val pos_target: DpOffset
            val width_target: Float
            val height_target: Float

            if (to_target) {
                with (density) {
                    pos_target = DpOffset(target_position!!.x.toDp(), target_position!!.y.toDp())
                    width_target = target_size!!.width.toDp().value
                    height_target = target_size!!.height.toDp().value
                }
            }
            else {
                pos_target = initial_pos
                width_target = initial_size.width.value
                height_target = initial_size.height.value
            }

            if (!to_target) {
                fully_open = false
            }

            coroutineScope {
                launch {
                    panel_alpha.animateTo(if (to_target) 1f else 0f, tween(anim_duration))
                }

                val pos_job = launch {
                    pos.animateTo(pos_target, tween(anim_duration))
                }
                val width_job = launch {
                    width.animateTo(width_target, tween(anim_duration))
                }
                val height_job = launch {
                    height.animateTo(height_target, tween(anim_duration))
                }

                pos_job.join()
                width_job.join()
                height_job.join()

                fully_open = to_target
            }
        }

        LaunchedEffect(Unit) {
            animateValues(true)
        }

        suspend fun closePopup() {
            animateValues(false)
            hide_thumb = false
        }

        var close_requested by remember { mutableStateOf(false) }
        LaunchedEffect(close_requested) {
            if (close_requested) {
                closePopup()
            }
        }

        Dialog(
            onDismissRequest = { close_requested = true },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {

            val dialog = LocalView.current.parent as DialogWindowProvider
            dialog.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            Box(Modifier.fillMaxSize()) {

                val shape = RoundedCornerShape(topStartPercent = 12, topEndPercent = 12)
                Column(
                    Modifier
                        .alpha(panel_alpha.value)
                        .background(MainActivity.theme.getBackground(false), shape)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(25.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(Modifier.fillMaxHeight().weight(1f).clickable { close_requested = true })

                    Row(
                        Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(media_item.getThumbUrl(false)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(clip)
                                .alpha(if (fully_open) 1f else 0f)
                                .aspectRatio(1f)
                                .onSizeChanged {
                                    target_size = it
                                }
                                .onGloballyPositioned {
                                    target_position = it.localPositionOf(
                                        it.parentCoordinates!!.parentCoordinates!!,
                                        it.positionInRoot()
                                    )
                                }
                        )

                        Column(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 15.dp)
                            , verticalArrangement = Arrangement.Center) {

                            if (media_item is Song) {
                                Marquee(false) {
                                    Text(media_item.title, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                                }
                            }

                            val artist = media_item.getArtist()
                            if (artist != null) {
                                Marquee(false) {
                                    ArtistPreview(
                                        when (media_item) {
                                            is Artist -> media_item
                                            is Song -> media_item.artist
                                        },
                                        false,
                                        MainActivity.theme.getOnBackground(false),
                                        Modifier.fillMaxWidth(),
                                        icon_size = 30.dp, font_size = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Divider(thickness = Dp.Hairline)

                    actions(LongPressMenuActionProvider(), media_item)
                    
                    val share_intent = remember(media_item.url) {
                        Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TITLE, song.title)
                            putExtra(Intent.EXTRA_TEXT, song.url)
                            type = "text/plain"
                        }, null)
                    }

                    LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, "Share") {
                        MainActivity.context.startActivity(share_intent)
                    }

                    val open_intent: Intent? = remember(media_item.url) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media_item.url))
                        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
                            null
                        }
                        else {
                            intent
                        }
                    }

                    if (open_intent != null) {
                        LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, "Open externally") {
                            MainActivity.context.startActivity(open_intent)
                        }
                    }
                }

                if (!fully_open) {
                    Box(
                        Modifier
                            .offset(pos.value.x, pos.value.y)
                            .requiredSize(width.value.dp, height.value.dp)
                            .clip(clip)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                        hide_thumb = true
                    }
                }
            }
        }
    }
}