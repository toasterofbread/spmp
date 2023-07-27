package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.theme.StaticThemeData
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.ui.theme.ThemeData
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.WidthShrinkText
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

class SettingsItemThemeSelector(
    val state: SettingsValueState<Int>,
    val title: String?,
    val subtitle: String?,
    val editor_title: String?,
    val getThemeCount: () -> Int,
    val getTheme: (index: Int) -> ThemeData,
    val onThemeEdited: (index: Int, edited_theme: ThemeData) -> Unit,
    val createTheme: (Int) -> Unit,
    val removeTheme: (index: Int) -> Unit
): SettingsItem() {
    override fun initialiseValueStates(
        prefs: ProjectPreferences,
        default_provider: (String) -> Any,
    ) {
        state.init(prefs, default_provider)
    }

    override fun releaseValueStates(prefs: ProjectPreferences) {
        state.release(prefs)
    }

    override fun resetValues() {
        state.reset()
        for (i in getThemeCount() - 1 downTo 0) {
            removeTheme(i)
        }
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemTitleText(
                    title,
                    theme,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Text("${state.get() + 1} / ${getThemeCount()}")

                ShapedIconButton(
                    { state.set((state.get() - 1).coerceAtLeast(0)) },
                    onLongClick = {
                        context.vibrateShort()
                        state.set(0)
                    },
                    enabled = state.get() > 0
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, null)
                }
                ShapedIconButton(
                    { state.set((state.get() + 1).coerceAtMost(getThemeCount() - 1)) },
                    onLongClick = {
                        context.vibrateShort()
                        state.set(getThemeCount() - 1)
                    },
                    enabled = state.get() + 1 < getThemeCount()
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, null)
                }
                IconButton({
                    val index = state.get() + 1
                    createTheme(index)
                    state.set(index)
                }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
            Crossfade(state.get()) { theme_index ->
                val theme_data = getTheme(theme_index)
                val height = 40.dp
                Row(
                    Modifier.height(height),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WidthShrinkText(
                        theme_data.name,
                        Modifier
                            .border(2.dp, theme_data.accent, CircleShape)
                            .fillMaxHeight()
                            .weight(1f)
                            .padding(start = 15.dp)
                    )

                    val icon_button_colours = IconButtonDefaults.iconButtonColors(
                        containerColor = theme_data.accent,
                        contentColor = theme_data.accent.getContrasted(),
                        disabledContainerColor = theme_data.accent.setAlpha(0.1f)
                    )

                    ShapedIconButton(
                        {
                            openCustomPage(
                                getEditPage(editor_title, theme_data) {
                                    onThemeEdited(theme_index, it)
                                }
                            )
                        },
                        Modifier.size(height),
                        colours = icon_button_colours,
                        enabled = theme_data.isEditable()
                    ) {
                        Icon(Icons.Filled.Edit, null, tint = theme_data.accent.getContrasted())
                    }

                    ShapedIconButton(
                        {
                            removeTheme(theme_index)
                            state.set(maxOf(0, theme_index - 1))
                        },
                        Modifier.size(height),
                        colours = icon_button_colours,
                        enabled = theme_data.isEditable()
                    ) {
                        Icon(Icons.Filled.Close, null, tint = theme_data.accent.getContrasted())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
private fun getEditPage(
    editor_title: String?,
    theme: ThemeData,
    onEditCompleted: (theme_data: ThemeData) -> Unit
): SettingsPage {
    return object : SettingsPage() {
        override val title: String? = editor_title

        private var reset by mutableStateOf(false)
        private var close by mutableStateOf(false)

        private var pill_extra: (@Composable PillMenu.Action.(Int) -> Unit)? = null
        private var pill_side_extra: (@Composable PillMenu.Action.() -> Unit)? = null

        private var name: String by mutableStateOf(theme.name)
        private var background: Color by mutableStateOf(theme.background)
        private var on_background: Color by mutableStateOf(theme.on_background)
        private var accent: Color by mutableStateOf(theme.accent)

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            val ui_theme = settings_interface.theme
            var previewing by remember { mutableStateOf(Theme.preview_active) }

            val icon_button_colours = IconButtonDefaults.iconButtonColors(
                containerColor = ui_theme.vibrant_accent,
                contentColor = ui_theme.vibrant_accent.getContrasted()
            )

            var randomise: Boolean by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                resetKeys()
                if (pill_extra == null) {
                    pill_extra = {
                        ActionButton(Icons.Filled.Done) {
                            onEditCompleted(StaticThemeData(name, background, on_background, accent))
                            close = true
                        }
                    }
                    settings_interface.pill_menu?.addExtraAction(action = pill_extra!!)
                }
                if (pill_side_extra == null) {
                    pill_side_extra =  {
                        val colours = ButtonDefaults.buttonColors(
                            containerColor = background_colour,
                            contentColor = content_colour
                        )

                        Button(
                            { previewing = !previewing },
                            Modifier.fillMaxHeight(),
                            colors = colours,
                            contentPadding = PaddingValues(start = 10.dp, end = 15.dp)
                        ) {
                            Switch(
                                previewing,
                                { previewing = it },
                                Modifier
                                    .scale(0.75f)
                                    .height(0.dp),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = ui_theme.vibrant_accent.getContrasted(),
                                    checkedThumbColor = ui_theme.vibrant_accent
                                )
                            )
                            Text("Preview", Modifier.padding(start = 5.dp))
                        }

                        Spacer(Modifier.width(10.dp))

                        Box(
                            Modifier
                                .clickable { randomise = !randomise }
                                .background(background_colour, CircleShape)
                                .clip(CircleShape)
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource("drawable/ic_die.xml"),
                                null,
                                Modifier.size(25.dp),
                                tint = content_colour
                            )
                        }
                    }
                    settings_interface.pill_menu?.addAlongsideAction(pill_side_extra!!)
                }
            }

            OnChangedEffect(previewing) {
                if (previewing) {
                    Theme.setPreviewThemeData(StaticThemeData(name, background, on_background, accent))
                }
                else {
                    Theme.setPreviewThemeData(null)
                }
            }

            OnChangedEffect(close) {
                if (close) {
                    close = false
                    goBack()
                }
            }

            val focus_manager = LocalFocusManager.current

            Column(
                Modifier
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focus_manager.clearFocus()
                        }
                    }
                    .padding(content_padding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    name,
                    { name = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    isError = name.isEmpty(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ui_theme.vibrant_accent,
                        cursorColor = ui_theme.vibrant_accent,
                        focusedLabelColor = ui_theme.vibrant_accent
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focus_manager.clearFocus()
                    })
                )

                fun updatePreview() {
                    if (Theme.preview_active) {
                        Theme.setPreviewThemeData(StaticThemeData(
                            "",
                            background,
                            on_background,
                            accent
                        ))
                    }
                }

                ColourField(
                    "Background",
                    ui_theme,
                    theme.background,
                    icon_button_colours,
                    randomise
                ) { colour ->
                    background = colour
                    updatePreview()
                }
                ColourField(
                    "On background",
                    ui_theme,
                    theme.on_background,
                    icon_button_colours,
                    randomise
                ) { colour ->
                    on_background = colour
                    updatePreview()
                }
                ColourField(
                    "Accent",
                    ui_theme,
                    theme.accent,
                    icon_button_colours,
                    randomise
                ) { colour ->
                    accent = colour
                    updatePreview()
                }
            }
        }

        override suspend fun resetKeys() {
            name = theme.name
            background = theme.background
            on_background = theme.on_background
            accent = theme.accent
            reset = !reset
        }

        override suspend fun onClosed() {
            super.onClosed()
            Theme.setPreviewThemeData(null)

            if (pill_extra != null) {
                settings_interface.pill_menu?.removeExtraAction(pill_extra!!)
                pill_extra = null
            }
            if (pill_side_extra != null) {
                settings_interface.pill_menu?.removeAlongsideAction(pill_side_extra!!)
                pill_side_extra = null
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ColourField(
    name: String,
    ui_theme: Theme,
    default_colour: Color,
    button_colours: IconButtonColors,
    randomise: Any,
    onChanged: suspend (Color) -> Unit
) {
    var show_picker by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(default_colour) }
    var instance by remember { mutableStateOf(false) }
    val presets = remember(current) { current.generatePalette(10, 1f).sorted(true) }

    @Composable
    fun Color.presetItem() {
        Spacer(Modifier
            .size(40.dp)
            .background(this, CircleShape)
            .border(Dp.Hairline, contrastAgainst(current), CircleShape)
            .clickable {
                current = this
                instance = !instance
            }
        )
    }

    LaunchedEffect(current) {
        onChanged(current)
    }

    OnChangedEffect(randomise) {
        current = Color.random()
        instance = !instance
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = ui_theme.background.getContrasted())

            Spacer(Modifier
                .fillMaxWidth()
                .weight(1f))

            FilledIconButton({
                show_picker = !show_picker
            }, colors = button_colours) {
                Crossfade(show_picker) { picker ->
                    Icon(if (picker) Icons.Filled.Done else Icons.Filled.Edit, null, Modifier.size(22.dp))
                }
            }
            FilledIconButton({
                current = Color.random()
                instance = !instance
            }, colors = button_colours) {
                Icon(painterResource("drawable/ic_die.xml"), null, Modifier.size(22.dp))
            }
            FilledIconButton({
                current = default_colour
                instance = !instance
            }, colors = button_colours) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(22.dp))
            }
        }

        val shape = RoundedCornerShape(13.dp)

        Column(Modifier
            .align(Alignment.End)
            .fillMaxWidth()
            .animateContentSize()
            .background(current, shape)
            .border(Dp.Hairline, current.contrastAgainst(ui_theme.background), shape)
            .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val spacing = 10.dp

            Crossfade(show_picker) { picker ->
                if (picker) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        var height by remember { mutableStateOf(0) }
                        LazyColumn(
                            Modifier.height( with(LocalDensity.current) { height.toDp() } ),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            items(presets) { colour ->
                                colour.presetItem()
                            }
                        }
                        Crossfade(instance) {
                            ClassicColorPicker(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .onSizeChanged {
                                        height = it.height
                                    },
                                HsvColor.from(current),
                                showAlphaBar = false
                            ) { colour ->
                                current = colour.toColor()
                            }
                        }
                    }
                }
                else {
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        items(presets) { colour ->
                            colour.presetItem()
                        }
                    }
                }
            }
        }
    }
}
