package com.spectre7.spmp.ui.layout

import com.spectre7.spmp.model.Artist

@Composable
fun ArtistPage(pill_menu: PillMenu, artist: Artist) {
    var show_info by remember{ mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Share
                ) {
                    val share_intent = remember(song.url) {
                        Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TITLE, artist.name)
                            putExtra(Intent.EXTRA_TEXT, artist.url)
                            type = "text/plain"
                        }, null)
                    }
                    MainActivity.context.startActivity(share_intent)
                }
            }
        }
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Info
                ) {
                    show_info = true
                }
            }
        }
    }

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Image(
            painter = rememberAsyncImagePainter(artist.getThumbUrl(true)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f), 
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(artist.name, fontSize = 40.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(artist: Artist, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                {
                    try {
                        setValue(if (is_int) text.toInt().toFloat() else text.toFloat())
                        show_edit_dialog = false
                    }
                    catch(_: NumberFormatException) {}
                },
                enabled = error == null
            ) {
                Text("Done")
            }
        },
        title = { Text("Artist info") },
        text = {
            fun InfoValue(name: String, value: String) {
                Column {
                    Text(name, style = MaterialTheme.typography.h1)
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(value)

                        OutlinedCard {
                            Row {
                                val clipboard = LocalClipboardManager.current
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(value))
                                    sendToast("Copied ${name.lowercase()} to clipboard")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                                }

                                val share_intent = Intent.createChooser(Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, value)
                                    type = "text/plain"
                                }, null)
                                IconButton(onClick = {
                                    MainActivity.context.startActivity(share_intent)
                                }) {
                                    Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            InfoValue("Name", artist.name)
            InfoValue("Id", artist.id)
            InfoValue("Url", artist.url)
        }
    )
}