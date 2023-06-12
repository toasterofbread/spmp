package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.ceil

const val RADIO_BUILDER_ICON_WIDTH = 35f

@Composable
fun RadioBuilderIcon(modifier: Modifier = Modifier) {
    Row(modifier.requiredSize(RADIO_BUILDER_ICON_WIDTH.dp, 25.dp)) {
        Icon(Icons.Default.Radio, null, Modifier.align(Alignment.CenterVertically))
        Icon(Icons.Default.Add, null, Modifier.align(Alignment.Top))
    }
}

@Composable
fun RadioBuilderPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    val player = LocalPlayerState.current

    var artists_result: Result<List<RadioBuilderArtist>>? by remember { mutableStateOf(null) }
    var selected_artists: Set<Int>? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            artists_result = getRadioBuilderArtists { thumbnails ->
                thumbnails.maxBy { it.width * it.height }
            }
        }
    }

    Column(modifier.padding(horizontal = 10.dp)) {
        MusicTopBar(
            Settings.INTERNAL_TOPBAR_MODE_RADIOBUILDER,
            Modifier.fillMaxWidth()
        )

        Crossfade(selected_artists) { selected ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Row(
                    Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioBuilderIcon()

    //                Text(getString("radio_builder_title"), fontSize = 15.sp)
                    Text(
                        getString(
                            if (selected == null) "radio_builder_artists_title"
                            else "radio_builder_modifiers_title"
                        ),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.width(RADIO_BUILDER_ICON_WIDTH.dp))
                }

                if (selected == null) {
                    if (artists_result?.isFailure == true) {
                        // TODO
                        SpMp.ErrorDisplay(artists_result?.exceptionOrNull())
                    }
                    else {
                        RadioArtistSelector(artists_result?.getOrNull(), pill_menu, Modifier.fillMaxSize()) { selected_artists = it.toSet() }
                    }
                }
                else {
                    BackHandler {
                        selected_artists = null
                    }

                    val selection_type = remember { mutableStateOf(RadioModifier.SelectionType.BLEND) }
                    SelectionTypeRow(selection_type)

                    val artist_variety = remember { mutableStateOf(RadioModifier.Variety.MEDIUM) }
                    ArtistVarietyRow(artist_variety)

                    val filter_a: MutableState<RadioModifier.FilterA?> = remember { mutableStateOf(null) }
                    FilterARow(filter_a)

                    val filter_b: MutableState<RadioModifier.FilterB?> = remember { mutableStateOf(null) }
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
                            selected_artists!!.map { artists_result!!.getOrThrow()[it] }.toSet(),
                            setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
                        )

                        invalid_modifiers = false
                        if (preview) {
                            preview_loading = true
                        }
                        else if (preview_playlist?.id == radio_token) {
                            player.player.startRadioAtIndex(0, preview_playlist)
                            return
                        }
                        else {
                            is_loading = true
                        }

                        Api.scope.launch {
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
                                        withContext(Dispatchers.Main) {
                                            player.player.startRadioAtIndex(0, it)
                                        }
                                    }
                                },
                                {
                                    if (it is InvalidRadioException) {
                                        invalid_modifiers = true
                                        preview_playlist = null
                                    }
                                    else {
                                        SpMp.error_manager.onError("radio_builder_load_radio", result.exceptionOrNull()!!)
                                    }
                                }
                            )

                            is_loading = false
                            preview_loading = false
                        }
                    }

                    Box {
                        var action_buttons_visible: Boolean by remember { mutableStateOf(true) }

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
                                        Text(getString("radio_builder_no_songs_match_criteria"), Modifier.padding(10.dp))

                                        Row {
                                            SpMp.context.CopyShareButtons(name = "") {
                                                buildRadioToken(
                                                    selected_artists!!.map { i -> artists_result!!.getOrThrow()[i] }.toSet(),
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
                                    SubtleLoadingIndicator(Modifier.offset(y = -bottom_padding), { Theme.current.on_background })
                                }
                                else if (playlist?.feed_layouts?.isNotEmpty() == true) {
                                    val layout = playlist.feed_layouts!!.first()
                                    val multiselect_context = remember { MediaItemMultiSelectContext() {} }

                                    DisposableEffect(multiselect_context.is_active) {
                                        action_buttons_visible = !multiselect_context.is_active
                                        onDispose {
                                            action_buttons_visible = true
                                        }
                                    }

                                    Column {
                                        AnimatedVisibility(multiselect_context.is_active) {
                                            multiselect_context.InfoDisplay()
                                        }
                                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom_padding)) {
                                            items(layout.items) { item ->
                                                item.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            action_buttons_visible,
                            Modifier.align(Alignment.TopEnd),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column {
                                val icon_button_colours = IconButtonDefaults.iconButtonColors(
                                    containerColor = Theme.current.accent,
                                    contentColor = Theme.current.on_accent
                                )
                                ShapedIconButton({ loadRadio(false) }, colors = icon_button_colours) {
                                    Crossfade(is_loading) { loading ->
                                        if (loading) {
                                            SubtleLoadingIndicator(colourProvider = { Theme.current.on_accent })
                                        } else {
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
    }
}

@Composable
private fun SelectionTypeRow(state: MutableState<RadioModifier.SelectionType>) {
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
            Text(getString("radio_builder_modifier_selection_type"))

            MultiSelectRow(
                amount = RadioModifier.SelectionType.values().size,
                arrangement = Arrangement.spacedBy(10.dp),
                isSelected = { it == state.value.ordinal },
                onSelected = { state.value =  RadioModifier.SelectionType.values()[it!!] },
                getText = {
                    RadioModifier.SelectionType.values()[it].getReadable()
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
private fun ArtistVarietyRow(state: MutableState<RadioModifier.Variety>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_variety"))
        MultiSelectRow(
            RadioModifier.Variety.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value.ordinal == it },
            onSelected = { state.value =  RadioModifier.Variety.values()[it!!] },
            getText = {
                RadioModifier.Variety.values()[it].getReadable()
            }
        )
    }
}

@Composable
private fun FilterARow(state: MutableState<RadioModifier.FilterA?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_a"))
        MultiSelectRow(
            RadioModifier.FilterA.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioModifier.FilterA.values()[it] } },
            getText = {
                RadioModifier.FilterA.values()[it].getReadable()
            },
            nullable = true
        )
    }
}

@Composable
private fun FilterBRow(state: MutableState<RadioModifier.FilterB?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_b"))
        MultiSelectRow(
            RadioModifier.FilterB.values().size,
            arrangement = Arrangement.spacedBy(20.dp),
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioModifier.FilterB.values()[it] } },
            getText = {
                RadioModifier.FilterB.values()[it].getReadable()
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
    modifier: Modifier = Modifier,
    onFinished: (List<Int>) -> Unit
) {
    val selected_artists: MutableList<Int> = remember { mutableStateListOf() }
    val player = LocalPlayerState.current

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
                    Text(getString("radio_builder_next_button"), Modifier.crossOut(selected_artists.isEmpty(), { content_colour }) { it * 1.2f })
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
                SubtleLoadingIndicator(colourProvider = { Theme.current.on_background })
            }
        }
        else {
            val thumb_size = 80.dp
            val selected_border_size = 10.dp

            LazyVerticalGrid(GridCells.Fixed(3), modifier) {
                items(artists.size) { index ->

                    val radio_artist = artists[index]
                    val artist = remember(radio_artist) {
                        Artist.createTemp(index.toString()).editArtistData {
                            supplyTitle(radio_artist.name)
                            supplyThumbnailProvider(MediaItemThumbnailProvider.fromThumbnails(listOf(radio_artist.thumbnail)))
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val selected by remember { derivedStateOf { selected_artists.contains(index) } }
                        val border_expansion = remember { Animatable(if (selected) 1f else 0f) }

                        OnChangedEffect(selected) {
                            border_expansion.animateTo(if (selected) 1f else 0f)
                        }

                        val long_press_menu_data = remember(artist) {
                            getArtistLongPressMenuData(artist)
                        }

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
                                        player.showLongPressMenu(long_press_menu_data)
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
                                        .border(1.dp, Theme.current.on_background, RoundedCornerShape(ARTIST_THUMB_CORNER_ROUNDING))
                                    )
                                }
                                artist.Thumbnail(
                                    MediaItemThumbnailProvider.Quality.LOW,
                                    Modifier
                                        .longPressMenuIcon(long_press_menu_data)
                                        .size(thumb_size)
                                )
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

private fun getRecordArcValues(type: RadioModifier.SelectionType, i: Int): Triple<Dp, Float, Float> {
    return when (type) {
        RadioModifier.SelectionType.FAMILIAR -> Triple((20f + 10f * i).dp, 40f + 20f * i, 0f)
        RadioModifier.SelectionType.BLEND -> Triple((20f + 10f * i).dp, 35f + 15f * i, -20f + i * 20f)
        RadioModifier.SelectionType.DISCOVER -> Triple((20f + 10f * i).dp, 40f + 25f * i, -25f + i * 35f)
    }
}

@Composable
private fun RadioSelectionTypeAnimation(type: RadioModifier.SelectionType, animate_state: Any = Unit, colour: Color = Color.White) {
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
