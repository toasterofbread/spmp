class MainOverlayMenu(val setOverlayMenu: (OverlayMenu?) -> Unit): OverlayMenu() {
    @Composable
    fun Menu(song: Song, seek_state: Any, openShutterMenu: (@Composable () -> Unit) -> Unit, close: () -> Unit) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            @Composable
            fun infoField(name: String, value: String, shareable: Boolean) {
                Row {
                    Text("$name: $value")

                    if (shareable) {
                        val clipboard = LocalClipboardManager.current
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(value))
                            sendToast("Copied $name to clipboard")
                        }) {
                            Icon(Icons.Filled.ContentCopy, null)
                        }

                        val share_intent = Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, value)
                            type = "text/plain"
                        }, null)
                        IconButton(onClick = {
                            MainActivity.context.startActivity(share_intent)
                        }) {
                            Icon(Icons.Filled.Share, null)
                        }
                    }
                }
            }

            if (PlayerServiceHost.status.m_song != null) {
                val song = PlayerServiceHost.status.m_song!!
                InfoField("Original title", song.original_title, true)
                InfoField("Video id", song.id, true)

                song.artist.Preview(false)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                val button_modifier = Modifier
                    .background(
                        MainActivity.theme.getAccent(),
                        CircleShape
                    )
                    .size(45.dp)
                    .padding(8.dp)
                val button_colour =
                    ColorFilter.tint(MainActivity.theme.getOnAccent())

                Box(
                    button_modifier.clickable {
                        setOverlayMenu(
                            PaletteSelectorOverlayMenu(theme_palette, {
                                colourpick_callback = it
                            }) { colour ->
                                setThemeColour(colour)
                                overlay_menu = NowPlayingOverlayMenu.NONE
                            }
                        )
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_palette), null,
                        colorFilter = button_colour
                    )
                }

                Box(
                    button_modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { setOverlayMenu(
                            LyricsOverlayMenu(
                                (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                            )
                        ) },
                        onLongClick = { 
                            vibrateShort()
                            setOverlayMenu(
                                LyricsOverlayMenu(
                                    (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                )
                            )
                        }
                    )
                ) {
                    Image(
                        painterResource(R.drawable.ic_music_note), null,
                        colorFilter = button_colour
                    )
                }

                Box(
                    button_modifier.clickable {
                        setOverlayMenu(DownloadOverlayMenu())
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_download), null,
                        colorFilter = button_colour
                    )
                }
            }
        }
    }
}