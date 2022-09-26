package com.spectre7.composesettings.model

import androidx.compose.runtime.*
import com.spectre7.utils.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.foundation.*
import android.util.Log

abstract class SettingsItem {
    @Composable
    abstract fun GetItem(theme: Theme)
}

class SettingsGroup(var title: String?): SettingsItem() {
    @Composable
    override fun GetItem(theme: Theme) {
        val colour = offsetColourRGB(theme.getAccent(), if (theme.getBackground(false).isDark()) 0.5 else -0.5)
        if (title != null) {
            Text(title!!.uppercase(), color = colour, fontSize = 15.sp)
        }
    }
}

class SettinsValueState<T>(initial_value: T) {
    private var _value: T by mutableStateOf(initial_value)
    var value: T
        get() = _value
        set(new_value: T) {
            _value = new_value
        }
}

class SettingsValueToggle(
    val state: SettinsValueState<Boolean>,
    val title: String,
    val subtitle: String?
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme) {
        Row() {
            Column(Modifier.fillMaxWidth().weight(1f).clickable {

            }) {
                Text(title)
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
                }
            }
            Switch(checked = state.value, onCheckedChange = {state.value = it})
        }
    }
}
