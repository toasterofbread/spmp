package com.spectre7.settings.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.settings.model.SettingsItem
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.utils.*

class SettingsItemThemeSelector<T>(
    val state: SettingsValueState<T>,
    val title: String?,
    val subtitle: String?,
    val editor_title: String?,
    val themeProvider: () -> Theme
): SettingsItem() {
    override fun initialiseValueStates(
        prefs: SharedPreferences,
        default_provider: (String) -> Any,
    ) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button(
            { openCustomPage(getEditPage(editor_title, themeProvider)) },
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.vibrant_accent,
                contentColor = theme.on_accent
            )
        ) {
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, color = theme.on_accent)
                }
                ItemText(subtitle, theme.on_accent)
            }
        }
    }
}

private fun getEditPage(editor_title: String?, themeProvider: () -> Theme): SettingsPage {
    return object : SettingsPage(editor_title) {

        @Composable
        override fun PageView(
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {

            Crossfade(themeProvider().theme_data) { theme ->

                var background by remember { mutableStateOf(theme.background) }
                var on_background by remember { mutableStateOf(theme.on_background) }
                var accent by remember { mutableStateOf(theme.accent) }

                var previewing by remember { mutableStateOf(Theme.preview_active) }
                OnChangedEffect(previewing) {
                    if (previewing) {
                        Theme.startPreview(ThemeData(background, on_background, accent))
                    }
                    else {
                        Theme.stopPreview()
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Spacer(Modifier)

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button({ previewing = !previewing }) {
                            Switch(previewing, { previewing = it },
                                Modifier
                                    .scale(0.75f)
                                    .height(0.dp))
                            Text("Preview", Modifier.padding(start = 5.dp))
                        }

                        Button({}) {
                            Icon(painterResource(R.drawable.ic_die), null, Modifier.size(25.dp))
                            Text("Randomise", Modifier.padding(start = 5.dp))
                        }
                    }

                    ColourField("Background colour", theme.background) { colour ->
                        background = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setBackground(colour)
                        }
                    }
                    ColourField("On background colour", theme.on_background) { colour ->
                        on_background = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setOnBackground(colour)
                        }
                    }
                    ColourField("Accent colour", theme.accent) { colour ->
                        accent = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setAccent(colour)
                        }
                    }
                }
            }
        }

        override fun resetKeys() {
            TODO("Not yet implemented")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
private fun ColourField(name: String, default_colour: Color, onChanged: suspend (Color) -> Unit) {
    var show_picker by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(default_colour) }
    var instance by remember { mutableStateOf(0) }

    OnChangedEffect(current) {
        onChanged(current)
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier
                .size(40.dp)
                .background(current, CircleShape)
                .border(1.dp, Theme.current.on_background, CircleShape)
                .clickable {
                    show_picker = !show_picker
                }
            )
        }

        Column(Modifier
            .align(Alignment.End)
            .fillMaxWidth()
            .animateContentSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (show_picker) {
                Crossfade(instance) {
                    ClassicColorPicker(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        HsvColor.from(current),
                        showAlphaBar = false
                    ) { colour ->
                        current = colour.toColor()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilledIconButton({
                        current = Color.random()
                        instance++
                    }) {
                        Icon(painterResource(R.drawable.ic_die), null, Modifier.size(25.dp))
                    }
                    FilledIconButton({
                        show_picker = false
                        current = default_colour
                    }) {
                        Icon(Icons.Filled.Close, null)
                    }
                    FilledIconButton({ show_picker = false }) {
                        Icon(Icons.Filled.Done, null, Modifier.size(25.dp))
                    }
                }
            }
        }
    }
}
