package com.spectre7.composesettings.model

import androidx.compose.runtime.*
import com.spectre7.utils.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp

abstract class SettingsItem {
    @Composable
    abstract fun GetItem(theme_colour: Color)
}

class SettingsGroup(var title: String?): SettingsItem() {
    @Composable
    override fun GetItem(theme_colour: Color) {
        if (title != null) {
            Text(title!!.uppercase(), color = theme_colour, fontSize = 15.sp)
        }

        // Divider(color = offsetColourRGB(theme_colour, 0.5), thickness = Dp.Hairline)
    }
}
