package com.toasterofbread.composesettings.ui.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.LargeDropdownMenu
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.ui.theme.Theme

class SettingsDropdownItem(
    val state: BasicSettingsValueState<Int>,
    val title: String,
    val subtitle: String?,
    val item_count: Int,
    val getButtonItem: ((Int) -> String)? = null,
    val getItem: (Int) -> String,
): SettingsItem() {

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }
    override fun releaseValueStates(prefs: ProjectPreferences) {
        state.release(prefs)
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

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            var open by remember { mutableStateOf(false) }

            Button(
                { open = !open },
                Modifier.requiredHeight(40.dp),
                shape = SETTINGS_ITEM_ROUNDED_SHAPE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.vibrant_accent,
                    contentColor = theme.on_accent
                )
            ) {
                Text(getButtonItem?.invoke(state.get()) ?: getItem(state.get()))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = theme.on_accent
                )
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = SETTINGS_ITEM_ROUNDED_SHAPE)
                ){
                    LargeDropdownMenu(
                        open,
                        { open = false },
                        item_count,
                        state.get(),
                        getItem,
                        selected_item_colour = Theme.vibrant_accent
                    ) {
                        state.set(it)
                        open = false
                    }
                }
            }
        }
    }
}
