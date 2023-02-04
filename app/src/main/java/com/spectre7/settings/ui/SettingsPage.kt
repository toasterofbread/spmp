package com.spectre7.composesettings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.settings.model.SettingsGroup
import com.spectre7.settings.model.SettingsItem
import com.spectre7.utils.WidthShrinkText

class SettingsPage(val title: String, val items: List<SettingsItem>, val modifier: Modifier = Modifier) {

    fun resetKeys(settings_interface: SettingsInterface) {
        for (item in items) {
            item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
            item.resetValues()
        }
    }

    @Composable
    fun Page(settings_interface: SettingsInterface, openPage: (Int) -> Unit, goBack: () -> Unit) {

        // Page items
        val spacing = 20.dp
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            if (items.isNotEmpty() && items[0] !is SettingsGroup) {
                Spacer(Modifier.requiredHeight(spacing))
            }

            for (item in items) {
                item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
                item.GetItem(settings_interface.theme, openPage)
            }
        }

        BackHandler {
            goBack()
        }
    }

    @Composable
    fun TitleBar(settings_interface: SettingsInterface, is_root: Boolean, goBack: () -> Unit) {
        WidthShrinkText(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }

}
