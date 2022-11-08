package com.spectre7.composesettings.model

import android.content.SharedPreferences
import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.spectre7.utils.*

abstract class SettingsItem {
    @Composable
    abstract fun GetItem(theme: Theme, open_page: (Int) -> Unit)
}

class SettingsGroup(var title: String?): SettingsItem() {
    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {
        Spacer(Modifier.requiredHeight(20.dp))
        if (title != null) {
            Text(title!!.uppercase(), color = theme.getVibrantAccent(), fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
    }
}

class SettingsValueState<T>(initial_value: T, val key: String, val prefs: SharedPreferences) {
    private var _value: T by mutableStateOf(getInitialValue(initial_value))
    internal var autosave: Boolean = true

    var value: T
        get() = _value
        set(new_value: T) {
            _value = new_value
            if (autosave) {
                save()
            }
        }

    private fun getInitialValue(default: T): T {
        return when (default!!::class) {
            Boolean::class -> prefs.getBoolean(key, default as Boolean)
            Float::class -> prefs.getFloat(key, default as Float)
            Int::class -> prefs.getInt(key, default as Int)
            Long::class -> prefs.getLong(key, default as Long)
            String::class -> prefs.getString(key, default as String)
            else -> throw java.lang.ClassCastException()
        } as T
    }

    internal fun save() {
        with (prefs.edit()) {
            when (value!!::class) {
                Boolean::class -> putBoolean(key, value as Boolean)
                Float::class -> putFloat(key, value as Float)
                Int::class -> putInt(key, value as Int)
                Long::class -> putLong(key, value as Long)
                String::class -> putString(key, value as String)
                else -> throw java.lang.ClassCastException()
            }
            apply()
        }
    }
}

class SettingsItemToggle(
    val state: SettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
                }
            }
            Switch(checked = state.value, onCheckedChange = {state.value = it}, colors = SwitchDefaults.colors(
                checkedThumbColor = theme.getVibrantAccent(),
                checkedTrackColor = theme.getVibrantAccent().setAlpha(0.5)
            ))
        }
    }
}

class SettingsItemSlider(
    val state: SettingsValueState<Float>,
    val title: String?,
    val subtitle: String?,
    val min_label: String? = null,
    val max_label: String? = null,
    val steps: Int = 0
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            if (title != null) {
                Text(title)
            }
            if (subtitle != null) {
                Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
            }

            Spacer(Modifier.requiredHeight(10.dp))

            state.autosave = false
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (min_label != null) {
                    Text(min_label, fontSize = 12.sp)
                }
                SliderValueHorizontal(
                    value = state.value,
                    onValueChange = {
                        state.value = it
                    },
                    onValueChangeFinished = {
                        state.save()
                    },
                    thumbSizeInDp = DpSize(12.dp, 12.dp),
                    track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, theme.getVibrantAccent().setAlpha(0.5), theme.getVibrantAccent(), colorTickProgress = theme.getVibrantAccent().getContrasted().setAlpha(0.5)) },
                    thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, theme.getVibrantAccent(), 1f) },
                    steps = steps,
                    modifier = Modifier.weight(1f)
                )
                if (max_label != null) {
                    Text(max_label, fontSize = 12.sp)
                }
            }
        }
    }
}

class SettingsItemMultipleChoice(
    val state: SettingsValueState<Int>,
    val title: String?,
    val subtitle: String?,
    val choice_amount: Int,
    val radio_style: Boolean,
    val get_choice: (Int) -> String,
): SettingsItem() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {
        Column {
            Column(Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75), fontSize = 15.sp)
                }

                Spacer(Modifier.height(10.dp))

                if (radio_style) {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .border(Dp.Hairline, theme.getOnBackground(false), RoundedCornerShape(16.dp))
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        null
                                    ) { state.value = i }
                            ) {
                                Text(get_choice(i), color = theme.getOnAccent())
                                RadioButton(i == state.value, onClick = { state.value = i }, colors = RadioButtonDefaults.colors(theme.getVibrantAccent()))
                            }
                        }
                    }
                }
                else {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {

                            val colour = remember(i) { Animatable(if (state.value == i) theme.getVibrantAccent() else Color.Transparent) }
                            LaunchedEffect(state.value, theme.getAccent()) {
                                colour.animateTo(if (state.value == i) theme.getAccent() else Color.Transparent, TweenSpec(150))
                            }

                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .border(Dp.Hairline, theme.getOnBackground(false), RoundedCornerShape(16.dp))
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) {
                                        state.value = i
                                    }
                                    .background(colour.value, RoundedCornerShape(16.dp))
                            ) {
                                Box(Modifier.padding(horizontal = 10.dp)) {
                                    Text(get_choice(i), color = if (state.value == i) theme.getOnAccent() else theme.getOnBackground(false))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class SettingsItemDropdown(
    val state: SettingsValueState<Int>,
    val title: String,
    val subtitle: String?,
    val item_count: Int,
    val getItem: (Int) -> String
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(Modifier.fillMaxWidth().weight(1f)) {
                Text(title)
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
                }
            }

            var open by remember { mutableStateOf(false) }

            Button({ open = !open }, Modifier.requiredHeight(40.dp), shape = RoundedCornerShape(16.dp)) {
                Text(getItem(state.value), color = theme.getOnAccent())
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = theme.getOnAccent()
                )
            }

            Popup(Alignment.TopEnd, onDismissRequest = { open = false }) {
                Crossfade(open) {
                    Row(Modifier.fillMaxWidth().offset((-20).dp), horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier.background(theme.getAccent(), RoundedCornerShape(16.dp)), contentAlignment = Alignment.TopEnd
                        ) {
                            if (it) {
                                Column(
                                    Modifier
                                        .width(100.dp)
                                        .padding(10.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = {

                                            })
                                        },
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (i in 0 until item_count) {
                                        val item = getItem(i)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .height(30.dp).fillMaxWidth()
                                                .clickable {
                                                    open = false
                                                    state.value = i
                                                }
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowRight,
                                                null,
                                                tint = theme.getOnAccent()
                                            )
                                            Text(
                                                item,
                                                color = theme.getOnAccent(),
                                                fontWeight = FontWeight.Medium
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
}

class SettingsItemSubpage(
    val title: String,
    val subtitle: String?,
    val target_page: Int,
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme, open_page: (Int) -> Unit) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            open_page(target_page)
        }, colors = ButtonDefaults.buttonColors(theme.getAccent(), theme.getOnAccent())
        ) {
            Column(Modifier.weight(1f)) {
                Text(title)
                if (subtitle != null) {
                    Text(subtitle)
                }
            }
        }
    }
}
