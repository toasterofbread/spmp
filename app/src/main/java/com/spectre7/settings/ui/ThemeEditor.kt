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
import com.spectre7.utils.Theme
import com.spectre7.spmp.R
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.getContrasted
import com.spectre7.utils.random

class SettingsItemThemeSelector<T>(
    val state: SettingsValueState<T>,
    val title: String?,
    val subtitle: String?,
    val editor_title: String?,
    val getTheme: (T) -> Theme
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

    @OptIn(ExperimentalMaterial3Api::class)
    private fun getEditPage(): SettingsPage {
        return object : SettingsPage(editor_title) {

            @SuppressLint("UnusedCrossfadeTargetStateParameter")
            @Composable
            private fun ColourField(name: String, provider: () -> Color, onChanged: suspend (Color) -> Unit) {
                var show_picker by remember { mutableStateOf(false) }
                var current by remember { mutableStateOf(provider()) }
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
                            .border(1.dp, MainActivity.theme.getOnBackground(false), CircleShape)
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
                                    current = provider()
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

            @Composable
            override fun PageView(
                openPage: (Int) -> Unit,
                openCustomPage: (SettingsPage) -> Unit,
                goBack: () -> Unit,
            ) {
                Crossfade(getTheme(state.value)) { theme ->
                    var previewing by remember { mutableStateOf(false) }

                    var background by remember { mutableStateOf(theme.getBackground(false)) }
                    var on_background by remember { mutableStateOf(theme.getOnBackground(false)) }
                    var accent by remember { mutableStateOf(theme.getAccent()) }

                    OnChangedEffect(previewing) {
                        theme.setBackground(false, if (previewing) background else null)
                        theme.setOnBackground(false, if (previewing) on_background else null)
                        theme.setAccent(if (previewing) accent else null)
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

                        ColourField("Background colour", theme.getBackgroundProvider(false)) { colour ->
                            theme.setBackground(false, colour)
                            theme.setOnBackground(false, colour.getContrasted())
                        }
                        ColourField("On background colour", theme.getOnBackgroundProvider(false)) {}
                        ColourField("Accent colour", theme.getAccentProvider()) {}
                        ColourField("On accent colour", theme.getOnAccentProvider()) {}
                    }
                }
            }

            override fun resetKeys() {
                TODO("Not yet implemented")
            }
        }
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button({ openCustomPage(getEditPage()) }) {
            Text("THeeeeeeme")
        }
    }
}
