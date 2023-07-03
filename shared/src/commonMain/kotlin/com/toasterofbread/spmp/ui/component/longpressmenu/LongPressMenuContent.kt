package com.toasterofbread.spmp.ui.component.longpressmenu

@Composable
internal fun LongPressMenuContent(
    data: LongPressMenuData,
    accent_colour: MutableState<Color?>,
    modifier: Modifier,
    onAction: () -> Unit,
    close: () -> Unit
) {
    @Composable
    fun Thumb(modifier: Modifier) {
        data.item.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, modifier.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING)))
    }

    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }
    if (show_title_edit_dialog) {
        MediaItemTitleEditDialog(data.item) {
            show_title_edit_dialog = false
        }
    }

    Box(
        Modifier
            .requiredHeight(SpMp.context.getScreenHeight() + SpMp.context.getNavigationBarHeight())
            .offset(y = -SpMp.context.getStatusBarHeight())
            .background(Color.Black.setAlpha(0.5f))
    ) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

        Column(Modifier.fillMaxSize().animateContentSize()) {
            Spacer(Modifier
                .fillMaxSize()
                .weight(1f)
                .clickable(remember { MutableInteractionSource() }, null, onClick = close)
            )

            var height by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            val padding = 25.dp

            var show_info by remember { mutableStateOf(false) }
            var main_actions_showing by remember { mutableStateOf(false) }
            var info_showing by remember { mutableStateOf(false) }

            Column(
                modifier
                    .background(Theme.current.background, shape)
                    .padding(bottom = SpMp.context.getNavigationBarHeight())
                    .fillMaxWidth()
                    .onSizeChanged {
                        if (show_info || !main_actions_showing) {
                            return@onSizeChanged
                        }

                        val h = with(density) { it.height.toDp() }
                        if (h > height) {
                            height = h
                        }
                    }
                    .thenIf(show_info || info_showing, Modifier.height(height)),
            ) {
                Box(Modifier.height(padding).fillMaxWidth()) {
                    NoRipple {
                        val pin_button_size = 24.dp
                        val pin_button_padding = 15.dp
                        Crossfade(
                            data.item.pinned_to_home,
                            Modifier.align(Alignment.CenterEnd).offset(x = -pin_button_padding, y = pin_button_padding)
                        ) { pinned ->
                            IconButton(
                                {
                                    data.item.setPinnedToHome(!pinned)
                                },
                                Modifier.size(pin_button_size)
                            ) {
                                Icon(
                                    if (pinned) Icons.Filled.PushPin
                                    else Icons.Outlined.PushPin,
                                    null
                                )
                            }
                        }
                    }
                }

                Column(
                    Modifier.padding(start = padding, end = padding, bottom = padding).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides Theme.current.on_background) {
                        Row(
                            Modifier
                                .height(80.dp)
                                .fillMaxWidth()
                        ) {

                            Thumb(Modifier.aspectRatio(1f))

                            // Item info
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 15.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Title
                                Marquee(
                                    Modifier.platformClickable(
                                        onAltClick = {
                                            show_title_edit_dialog = !show_title_edit_dialog
                                            SpMp.context.vibrateShort()
                                        }
                                    )
                                ) {
                                    Text(
                                        data.item.title ?: "",
                                        Modifier.fillMaxWidth(),
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Artist
                                if (data.item !is Artist) {
                                    val artist = data.item.artist
                                    if (artist != null) {
                                        Marquee {
                                            val player = LocalPlayerState.current
                                            CompositionLocalProvider(
                                                LocalPlayerState provides remember {
                                                    player.copy(
                                                        onClickedOverride = { item, _ ->
                                                            onAction()
                                                            player.onMediaItemClicked(item)
                                                        }
                                                    )
                                                }
                                            ) {
                                                artist.PreviewLong(MediaItemPreviewParams(Modifier.fillMaxWidth()))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Info header
                        Row(Modifier.requiredHeight(1.dp)) {
                            var info_title_width: Int by remember { mutableStateOf(0) }
                            var box_width: Int by remember { mutableStateOf(-1) }

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .onSizeChanged { box_width = it.width }
                            ) {
                                Crossfade(show_info) { info ->
                                    val text = if (info) data.info_title else data.getInitialInfoTitle?.invoke()
                                    val current = info == show_info
                                    if (text != null) {
                                        Text(
                                            text,
                                            Modifier.offset(y = (-10).dp).onSizeChanged { if (current) info_title_width = it.width },
                                            overflow = TextOverflow.Visible
                                        )
                                    }
                                    else if (current) {
                                        info_title_width = 0
                                    }
                                }

                                Box(
                                    Modifier
                                        .run {
                                            if (box_width < 0) fillMaxWidth()
                                            else width(animateDpAsState(
                                                with(LocalDensity.current) {
                                                    if (info_title_width == 0) box_width.toDp() else (box_width - info_title_width).toDp() - 15.dp
                                                }
                                            ).value)
                                        }
                                        .requiredHeight(20.dp)
                                        .background(Theme.current.background)
                                        .align(Alignment.CenterEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Divider(
                                        thickness = Dp.Hairline,
                                        color = Theme.current.on_background
                                    )
                                }
                            }

                            if (data.infoContent != null) {
                                IconButton({ show_info = !show_info }, Modifier.requiredHeight(40.dp)) {
                                    Crossfade(show_info) { info ->
                                        Icon(if (info) Icons.Filled.Close else Icons.Filled.Info, null)
                                    }
                                }
                            }

                            data.SideButton(Modifier.requiredHeight(40.dp), Theme.current.background, accent_colour.value ?: Theme.current.accent)
                        }

                        // Info/action list
                        Crossfade(show_info) { info ->
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)) {
                                if (info) {
                                    DisposableEffect(Unit) {
                                        info_showing = true
                                        onDispose {
                                            info_showing = false
                                        }
                                    }
                                    data.infoContent?.invoke(this, accent_colour.value ?: Theme.current.accent)
                                }
                                else {
                                    DisposableEffect(Unit) {
                                        main_actions_showing = true
                                        onDispose {
                                            main_actions_showing = false
                                        }
                                    }
                                    LongPressMenuActions(data, accent_colour.value ?: Theme.current.accent, onAction = onAction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
