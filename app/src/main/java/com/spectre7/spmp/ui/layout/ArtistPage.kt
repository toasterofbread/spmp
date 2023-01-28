@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.ArtistInfoItem
import com.spectre7.spmp.api.getArtistInfo
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.Marquee
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.*
import java.util.regex.Pattern
import kotlin.concurrent.thread

@Composable
fun ArtistPage(
    pill_menu: PillMenu,
    artist: Artist,
    close: () -> Unit,
    onItemClicked: (MediaItem) -> Unit
) {
    var show_info by remember { mutableStateOf(false) }

    val share_intent = remember(artist.url, artist.name) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, artist.name)
            putExtra(Intent.EXTRA_TEXT, artist.url)
            type = "text/plain"
        }, null)
    }
    val open_intent: Intent? = remember(artist.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artist.url))
        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
            null
        }
        else {
            intent
        }
    }

    val gradient_size = 0.35f
    val background_colour = MainActivity.theme.getBackground(false)
    var accent_colour by remember { mutableStateOf(Color.Unspecified) }

    var artist_rows: List<ArtistInfoItem>? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        thread {
            runBlocking {
                val jobs = mutableListOf<Job>()
                val result = getArtistInfo(artist.id).getDataOrThrow()

                for (row in result) {
                    if (row.items == null) {
                        continue
                    }
                    val items = mutableListOf<MediaItem>()
                    for (item in row.items.items) {
                        jobs.add(launch {
                            item.loadData()
                            if (item.is_valid) {
                                items.add(item)
                            }
                        })
                    }
                    row.items.items = items
                }

                jobs.joinAll()
                artist_rows = result
            }
        }
    }

    DisposableEffect(accent_colour) {
        if (!accent_colour.isUnspecified) {
            pill_menu.setBackgroundColourOverride(accent_colour)
        }

        onDispose {
            pill_menu.setBackgroundColourOverride(null)
        }
    }

    BackHandler(onBack = close)

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {

        // Artist image
        Crossfade(artist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
            if (thumbnail != null) {
                if (accent_colour.isUnspecified) {
                    accent_colour = MediaItem.getDefaultPaletteColour(artist.thumbnail_palette!!, MainActivity.theme.getAccent())
                }

                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            Brush.verticalGradient(
                                0f to background_colour,
                                gradient_size to Color.Transparent
                            )
                        )
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {

            // Image spacing
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .background(
                            Brush.verticalGradient(
                                1f - gradient_size to Color.Transparent,
                                1f to background_colour
                            )
                        )
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(artist.name, fontSize = 40.sp)
                }
            }

            val content_padding = 20.dp

            // Action bar
            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .background(background_colour),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = content_padding)
                ) {
                    fun chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                        item {
                            ElevatedAssistChip(
                                onClick,
                                { Text(text, style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = {
                                    Icon(icon, null, tint = accent_colour)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MainActivity.theme.getBackground(false),
                                    labelColor = MainActivity.theme.getOnBackground(false),
                                    leadingIconContentColor = accent_colour
                                )
                            )
                        }
                    }

                    chip(getString(R.string.artist_chip_play), Icons.Outlined.PlayArrow) { TODO() }
                    chip(getString(R.string.artist_chip_shuffle), Icons.Outlined.Shuffle) { TODO() }
                    chip(getString(R.string.artist_chip_radio), Icons.Outlined.Radio) { TODO() }
                    chip(getString(R.string.action_share), Icons.Outlined.Share) { MainActivity.context.startActivity(share_intent) }
                    chip(getString(R.string.artist_chip_open), Icons.Outlined.OpenInNew) { MainActivity.context.startActivity(open_intent) }
                    chip(getString(R.string.artist_chip_details), Icons.Outlined.Info) { show_info = !show_info }
                }
            }

            // Loaded items
            item {
                Crossfade(artist_rows) { rows ->
                    if (rows == null) {
                        Box(Modifier.fillMaxSize().background(background_colour).padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour)
                        }
                    }
                    else {
                        Column(
                            Modifier
                                .background(background_colour)
                                .fillMaxSize()
                                .padding(content_padding)
                        ) {
                            for (row in rows) {
                                if (row.items == null) {
                                    continue
                                }

                                MediaItemGrid(row.items.title, null, row.items.items, onClick = onItemClicked)
                            }

                            val description = rows.firstOrNull { it.description != null }?.description?.third ?: artist.description
                            if (description.isNotBlank()) {
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        AssistChip(
                                            { show_info = !show_info },
                                            {
                                                Text(getString(R.string.artist_info_label), style = MaterialTheme.typography.labelLarge)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Info, null)
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MainActivity.theme.getBackground(false),
                                                labelColor = MainActivity.theme.getOnBackground(false),
                                                leadingIconContentColor = accent_colour
                                            )
                                        )

                                        LinkifyText(
                                            description,
                                            MainActivity.theme.getOnBackground(false).setAlpha(0.8),
                                            MainActivity.theme.getOnBackground(false),
                                            MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkifyText(
    text: String,
    colour: Color,
    highlight_colour: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val linksList = extractUrls(text)
    val annotatedString = buildAnnotatedString {
        append(text)
        linksList.forEach { link ->
            addStyle(
                style = SpanStyle(
                    color = highlight_colour,
                    textDecoration = TextDecoration.Underline
                ),
                start = link.second,
                end = link.third
            )
            addStringAnnotation(
                tag = "URL",
                annotation = link.first,
                start = link.second,
                end = link.third
            )
        }
    }
    Text(text = annotatedString, color = colour, style = style, modifier = modifier.pointerInput(Unit) {
        detectTapGestures { offsetPosition ->
            layoutResult.value?.let {
                val position = it.getOffsetForPosition(offsetPosition)
                annotatedString.getStringAnnotations(position, position).firstOrNull()
                    ?.let { result ->
                        if (result.tag == "URL") {
                            uriHandler.openUri(result.item)
                        }
                    }
            }
        }
    },
        onTextLayout = { layoutResult.value = it }
    )
}

private val urlPattern: Pattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
)

fun extractUrls(text: String): List<Triple<String, Int, Int>> {
    val matcher = urlPattern.matcher(text)
    var start: Int
    var end: Int
    val links = arrayListOf<Triple<String, Int, Int>>()

    while (matcher.find()) {
        start = matcher.start(1)
        end = matcher.end()

        var url = text.substring(start, end)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        links.add(Triple(url, start, end))
    }
    return links
}

@Composable
private fun InfoDialog(artist: Artist, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text("Artist info") },
        text = {
            @Composable
            fun InfoValue(name: String, value: String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Box(Modifier.fillMaxWidth()) {
                            Marquee(false) {
                                Text(value, softWrap = false)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End) {
                        val clipboard = LocalClipboardManager.current
                        IconButton({
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
                        IconButton({
                            MainActivity.context.startActivity(share_intent)
                        }) {
                            Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                InfoValue("Name", artist.name)
                InfoValue("Id", artist.id)
                InfoValue("Url", artist.url)
            }
        }
    )
}