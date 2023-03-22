package com.spectre7.spmp.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.concurrent.thread

@Composable
fun RadioBuilderPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    var available_artists: List<RadioBuilderArtist>? by remember { mutableStateOf(null) }
    var selected_artists: Set<Int>? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        thread {
            val result = getRadioBuilderArtists { thumbnails ->
                thumbnails.maxBy { it.width * it.height }
            }
            result.fold({ artists ->
                available_artists = artists
            }, { exception ->
                MainActivity.error_manager.onError("radio_builder_artists", exception)
            })
        }
    }

    Crossfade(selected_artists) { selected ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(getString(R.string.radio_builder_title), fontSize = 15.sp)
                Text(
                    getString(
                        if (selected == null) R.string.radio_builder_artists_title
                        else R.string.radio_builder_modifiers_title
                    ), fontSize = 30.sp
                )
            }

            if (selected == null) {
                RadioArtistSelector(available_artists, pill_menu, playerProvider, Modifier.fillMaxSize()) { selected_artists = it.toSet() }
            }
            else {
                BackHandler {
                    selected_artists = null
                }

                val selection_type = remember { mutableStateOf(RadioBuilderModifier.SelectionType.BLEND) }
                SelectionTypeRow(selection_type)

                val artist_variety = remember { mutableStateOf(RadioBuilderModifier.Variety.MEDIUM) }
                ArtistVarietyRow(artist_variety)

                val filter_a: MutableState<RadioBuilderModifier.FilterA?> = remember { mutableStateOf(null) }
                FilterARow(filter_a)

                val filter_b: MutableState<RadioBuilderModifier.FilterB?> = remember { mutableStateOf(null) }
                FilterBRow(filter_b)

                var is_loading by remember { mutableStateOf(false) }
                var preview_loading by remember { mutableStateOf(false) }
                var preview_playlist: Playlist? by remember { mutableStateOf(null) }
                var invalid_modifiers: Boolean by remember { mutableStateOf(false) }

                fun loadRadio(preview: Boolean) {
                    if (is_loading || preview_loading) {
                        return
                    }

                    val radio_token = buildRadioToken(
                        selected_artists!!.map { available_artists!![it] }.toSet(),
                        setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
                    )

                    invalid_modifiers = false
                    if (preview) {
                        preview_loading = true
                    }
                    else if (preview_playlist?.id == radio_token) {
                        PlayerServiceHost.service.startRadioAtIndex(0, preview_playlist)
                        return
                    }
                    else {
                        is_loading = true
                    }

                    thread {
                        val result = getBuiltRadio(radio_token)

                        result.fold(
                            {
                                if (it == null) {
                                    invalid_modifiers = true
                                }
                                else if (preview) {
                                    preview_playlist = it
                                }
                                else {
                                    mainThread {
                                        PlayerServiceHost.service.startRadioAtIndex(0, it)
                                    }
                                }
                            },
                            {
                                MainActivity.error_manager.onError("radio_builder_load_radio", result.exceptionOrNull()!!)
                            }
                        )

                        is_loading = false
                        preview_loading = false
                    }
                }

                Box {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Crossfade(Triple(preview_loading, preview_playlist, invalid_modifiers)) {
                            val (loading, playlist, invalid) = it
                            if (invalid) {
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .padding(bottom = bottom_padding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(getString("No songs match criteria"), Modifier.padding(10.dp))

                                    Row {
                                        CopyShareButtons(name = "") {
                                            buildRadioToken(
                                                selected_artists!!.map { i -> available_artists!![i] }.toSet(),
                                                setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
                                            )
                                        }
                                    }

//                                    val clipboard = LocalClipboardManager.current
//                                    Button(
//                                        {
//                                            clipboard.setText(AnnotatedString(
//                                                buildRadioToken(
//                                                    selected_artists!!.map { i -> available_artists!![i] }.toSet(),
//                                                    setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
//                                                )
//                                            ))
//                                        },
//                                        colors = ButtonDefaults.buttonColors(
//                                            containerColor = Theme.current.accent,
//                                            contentColor = Theme.current.on_accent
//                                        )
//                                    ) {
//                                        Text(getString("Copy radio token"))
//                                    }
                                }
                            }
                            else if (loading) {
                                SubtleLoadingIndicator(Theme.current.on_background, Modifier.offset(y = -bottom_padding))
                            }
                            else if (playlist?.feed_layouts?.isNotEmpty() == true) {
                                val layout = playlist.feed_layouts!!.first()
                                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom_padding)) {
                                    items(layout.items) { item ->
                                        item.PreviewLong(
                                            content_colour = Theme.current.on_background_provider,
                                            playerProvider = playerProvider,
                                            enable_long_press_menu = true,
                                            modifier = Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(Modifier.align(Alignment.TopEnd)) {
                        val icon_button_colours = IconButtonDefaults.iconButtonColors(
                            containerColor = Theme.current.accent,
                            contentColor = Theme.current.on_accent
                        )
                        ShapedIconButton({ loadRadio(false) }, colors = icon_button_colours) {
                            Crossfade(is_loading) { loading ->
                                if (loading) {
                                    SubtleLoadingIndicator()
                                }
                                else {
                                    Icon(Icons.Filled.PlayArrow, null)
                                }
                            }
                        }
                        ShapedIconButton({ loadRadio(true) }, colors = icon_button_colours) {
                            Icon(Icons.Filled.RemoveRedEye, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionTypeRow(state: MutableState<RadioBuilderModifier.SelectionType>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            for (i in 0 until 5) {
                val size = remember { Animatable(0f) }
                val arc_angle = remember { Animatable(0f) }
                val offset = remember { Animatable(0f) }

                LaunchedEffect(state.value) {
                    val values = getRecordArcValues(state.value, i)

                    launch {
                        size.animateTo(values.first.value)
                    }
                    launch {
                        arc_angle.animateTo(values.second)
                    }
                    launch {
                        offset.animateTo(values.third, SpringSpec(stiffness = Spring.StiffnessVeryLow))
                    }
                }

                RecordArc(size.value.dp, arc_angle.value, offset.value, Theme.current.vibrant_accent)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(getString(R.string.radio_builder_modifier_selection_type))

            MultiSelectRow(
                amount = RadioBuilderModifier.SelectionType.values().size,
                arrangement = Arrangement.spacedBy(10.dp),
                isSelected = { it == state.value.ordinal },
                onSelected = { state.value =  RadioBuilderModifier.SelectionType.values()[it!!] },
                getText = {
                    getString(when (RadioBuilderModifier.SelectionType.values()[it]) {
                        RadioBuilderModifier.SelectionType.FAMILIAR -> R.string.radio_builder_modifier_selection_type_familiar
                        RadioBuilderModifier.SelectionType.BLEND -> R.string.radio_builder_modifier_selection_type_blend
                        RadioBuilderModifier.SelectionType.DISCOVER -> R.string.radio_builder_modifier_selection_type_discover
                    })
                },
                button_padding = PaddingValues(0.dp)
            )
        }

//            for (type in RadioBuilderModifier.SelectionType.values()) {
//                var animate by remember { mutableStateOf(false) }
//                Column(
//                    Modifier
//                        .fillMaxWidth()
//                        .weight(1f)
//                        .cliclableNoIndication {
//                            if (type != state.value) {
//                                state.value = type
//                                animate = !animate
//                            }
//                        },
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(15.dp)
//                ) {
//                    RadioSelectionTypeAnimation(type, animate, colour = if (state.value == type) Theme.current.vibrant_accent else Theme.current.on_background)
//                    Text(text)
//                }
//            }
    }
}

@Composable
private fun ArtistVarietyRow(state: MutableState<RadioBuilderModifier.Variety>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString(R.string.radio_builder_modifier_variety))
        MultiSelectRow(
            RadioBuilderModifier.Variety.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value.ordinal == it },
            onSelected = { state.value =  RadioBuilderModifier.Variety.values()[it!!] },
            getText = {
                getString(when (RadioBuilderModifier.Variety.values()[it]) {
                    RadioBuilderModifier.Variety.LOW -> R.string.radio_builder_modifier_variety_low
                    RadioBuilderModifier.Variety.MEDIUM -> R.string.radio_builder_modifier_variety_medium
                    RadioBuilderModifier.Variety.HIGH -> R.string.radio_builder_modifier_variety_high
                })
            }
        )
    }
}

@Composable
private fun FilterARow(state: MutableState<RadioBuilderModifier.FilterA?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString(R.string.radio_builder_modifier_filter_a))
        MultiSelectRow(
            RadioBuilderModifier.FilterA.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioBuilderModifier.FilterA.values()[it] } },
            getText = {
                getString(when (RadioBuilderModifier.FilterA.values()[it]) {
                    RadioBuilderModifier.FilterA.POPULAR -> R.string.radio_builder_modifier_filter_a_popular
                    RadioBuilderModifier.FilterA.HIDDEN -> R.string.radio_builder_modifier_filter_a_hidden
                    RadioBuilderModifier.FilterA.NEW -> R.string.radio_builder_modifier_filter_a_new
                })
            },
            nullable = true
        )
    }
}

@Composable
private fun FilterBRow(state: MutableState<RadioBuilderModifier.FilterB?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString(R.string.radio_builder_modifier_filter_b))
        MultiSelectRow(
            RadioBuilderModifier.FilterB.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioBuilderModifier.FilterB.values()[it] } },
            getText = {
                getString(when (RadioBuilderModifier.FilterB.values()[it]) {
                    RadioBuilderModifier.FilterB.PUMP_UP -> R.string.radio_builder_modifier_filter_pump_up // 熱
                    RadioBuilderModifier.FilterB.CHILL -> R.string.radio_builder_modifier_filter_chill // 冷
                    RadioBuilderModifier.FilterB.UPBEAT -> R.string.radio_builder_modifier_filter_upbeat // 明るい
                    RadioBuilderModifier.FilterB.DOWNBEAT -> R.string.radio_builder_modifier_filter_downbeat // 重い
                    RadioBuilderModifier.FilterB.FOCUS -> R.string.radio_builder_modifier_filter_focus
                })
            },
            nullable = true,
            button_padding = PaddingValues(0.dp),
            columns = 3
        )
    }
}

@Composable
private fun MultiSelectRow(
    amount: Int,
    arrangement: Arrangement.Horizontal,
    isSelected: (Int) -> Boolean,
    onSelected: (Int?) -> Unit,
    getText: (Int) -> String,
    nullable: Boolean = false,
    button_padding: PaddingValues = ButtonDefaults.ContentPadding,
    columns: Int = amount
) {
    val rows = if (columns <= 0) 1 else ceil(amount / columns.toDouble()).toInt()
    for (row in 0 until rows) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in row * columns until (row + 1) * columns) {
                Box(Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    if (i < amount) {
                        NoRipple {
                            Crossfade(isSelected(i)) { selected ->
                                if (selected) {
                                    Button(
                                        { if (nullable) onSelected(null) },
                                        Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Theme.current.accent,
                                            contentColor = Theme.current.on_accent
                                        ),
                                        contentPadding = button_padding
                                    ) {
                                        Text(getText(i))
                                    }
                                }
                                else {
                                    OutlinedButton(
                                        { onSelected(i) },
                                        Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Theme.current.on_background
                                        ),
                                        contentPadding = button_padding
                                    ) {
                                        Text(getText(i))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadioArtistSelector(
    radio_artists: List<RadioBuilderArtist>?,
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    onFinished: (List<Int>) -> Unit
) {
    val selected_artists: MutableList<Int> = remember { mutableStateListOf() }

    DisposableEffect(Unit) {
        val actions = pill_menu.run { listOf (
            addExtraAction(false) {
                IconButton(
                    { selected_artists.clear() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = background_colour,
                        contentColor = content_colour
                    )
                ) {
                    Icon(Icons.Filled.Refresh, null)
                }
            },
            addExtraAction(false) {
                Button(
                    { if (selected_artists.isNotEmpty()) onFinished(selected_artists) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = background_colour,
                        contentColor = content_colour
                    ),
                    contentPadding = PaddingValues(0.dp, 0.dp)
                ) {
                    Text(getString(R.string.radio_builder_next_button), Modifier.crossOut(selected_artists.isEmpty(), content_colour, 3f, { it * 1.2f }))
                }
            }
        ) }

        onDispose {
            for (action in actions) {
                pill_menu.removeExtraAction(action)
            }
        }
    }

    Crossfade(radio_artists) { artists ->
        if (artists == null) {
            Box(modifier, contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator(Theme.current.on_background)
            }
        }
        else {
            val thumb_size = 80.dp
            val selected_border_size = 10.dp

            LazyVerticalGrid(GridCells.Fixed(3), modifier) {
                items(artists.size) { index ->

                    val radio_artist = artists[index]
                    val artist = remember(radio_artist) {
                        Artist.createTemp(index.toString()).apply {
                            supplyTitle(radio_artist.name)
                            supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(listOf(radio_artist.thumbnail)))
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val selected by remember { derivedStateOf { selected_artists.contains(index) } }
                        val border_expansion = remember { Animatable(if (selected) 1f else 0f) }

                        OnChangedEffect(selected) {
                            border_expansion.animateTo(if (selected) 1f else 0f)
                        }

                        val long_press_menu_data = remember(artist) { LongPressMenuData(
                            artist,
                            CircleShape,
                            artistLongPressPopupActions
                        ) }

                        Column(
                            modifier
                                .padding(10.dp, 0.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!selected_artists.remove(index)) {
                                            selected_artists.add(index)
                                        }
                                    },
                                    onLongClick = {
                                        playerProvider().showLongPressMenu(long_press_menu_data)
                                    }
                                )
                                .aspectRatio(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(thumb_size + selected_border_size), contentAlignment = Alignment.Center) {
                                if (border_expansion.value > 0f) {
                                    Box(Modifier
                                        .size(thumb_size + selected_border_size * border_expansion.value)
                                        .border(1.dp, Theme.current.on_background, CircleShape)
                                    )
                                }
                                artist.Thumbnail(MediaItem.ThumbnailQuality.LOW,
                                    Modifier
                                        .size(thumb_size)
                                        .longPressMenuIcon(long_press_menu_data))
                            }

                            Text(
                                artist.title ?: "",
                                fontSize = 12.sp,
                                color = Theme.current.on_background,
                                maxLines = 1,
                                lineHeight = 14.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordArc(size: Dp, arc_angle: Float, offset: Float = 0f, colour: Color, anim: Animatable<Float, AnimationVector1D>? = null) {
    val density = LocalDensity.current
    for (direction in listOf(-1, 1)) {
        val o = if (direction == -1) 180f else 0f
        Box(Modifier
            .size(size)
            .drawBehind {
                drawArc(
                    colour.setAlpha(0.5f),
                    (arc_angle * -0.5f) + o + ((anim?.value ?: 0f) * direction) + offset,
                    arc_angle,
                    false,
                    style = Stroke(with(density) { 2.dp.toPx() })
                )
            })
    }
}

private fun getRecordArcValues(type: RadioBuilderModifier.SelectionType, i: Int): Triple<Dp, Float, Float> {
    return when (type) {
        RadioBuilderModifier.SelectionType.FAMILIAR -> Triple((20f + 10f * i).dp, 40f + 20f * i, 0f)
        RadioBuilderModifier.SelectionType.BLEND -> Triple((20f + 10f * i).dp, 35f + 15f * i, -20f + i * 20f)
        RadioBuilderModifier.SelectionType.DISCOVER -> Triple((20f + 10f * i).dp, 40f + 25f * i, -25f + i * 35f)
    }
}

@Composable
private fun RadioSelectionTypeAnimation(type: RadioBuilderModifier.SelectionType, animate_state: Any = Unit, colour: Color = Color.White) {
    val animatable = remember { Animatable(0f) }
//    val anim_spec = SpringSpec<Float>(stiffness = Spring.StiffnessVeryLow)
    val anim_spec = TweenSpec<Float>(850, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f))

    OnChangedEffect(animate_state) {
        animatable.snapTo(0f)
        animatable.animateTo(360f, anim_spec)
    }

    Box(contentAlignment = Alignment.Center) {
        for (i in 0 until 5) {
            val values = getRecordArcValues(type, i)
            RecordArc(values.first, values.second, values.third, colour, animatable)
        }
    }
}
