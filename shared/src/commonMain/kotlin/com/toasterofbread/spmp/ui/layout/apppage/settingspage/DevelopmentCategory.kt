package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import SpMp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsComposableItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsGroupItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsSubpageItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.UnlocalisedStringCollector.UnlocalisedString
import com.toasterofbread.toastercomposetools.utils.common.blendWith
import com.toasterofbread.toastercomposetools.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.ui.component.CopyShareButtons

internal fun getDevelopmentCategory(): List<SettingsItem> {
    return mutableListOf(
        SettingsSubpageItem(
            getString("s_subpage_ui_debug_info"),
            null,
            PrefsPageScreen.UI_DEBUG_INFO.ordinal,
            null
        ),

        SettingsGroupItem(
            getString("s_group_localisation")
        )
    ).apply {
        val unlocalised_strings = SpMp.unlocalised_string_collector?.getStrings()
        if (unlocalised_strings != null) {
            add(
                SettingsComposableItem {
                    UnlocalisedStringList(unlocalised_strings)
                }
            )
        }
    }
}

@Composable
private fun UnlocalisedStringList(strings: List<UnlocalisedString>) {
    val player = LocalPlayerState.current

    val strings_by_type: Map<String, List<UnlocalisedString>> = remember(strings) {
        mutableMapOf<String, MutableList<UnlocalisedString>>().also { map ->
            for (string in strings) {
                val type_strings = map.getOrPut(string.type) { mutableListOf() }
                type_strings.add(string)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsItem.ItemTitleText(getString("s_key_unlocalised_strings"), player.theme)
        SettingsItem.ItemText(getString("s_sub_unlocalised_strings"), player.theme)

        Spacer(Modifier.height(20.dp))

        for (type in strings_by_type) {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = player.theme.accent.blendWith(player.theme.background, 0.1f),
                    contentColor = player.theme.on_background
                )
            ) {
                Column(Modifier.padding(10.dp)) {
                    WidthShrinkText(
                        type.key, 
                        Modifier.padding(bottom = 10.dp), 
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    for (string in type.value) {
                        string.InfoDisplay()
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlocalisedString.InfoDisplay() {
    val player = LocalPlayerState.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text("Key: $key")
            Text("Lang: $source_language")
        }

        player.context.CopyShareButtons {
            "String: ${this@InfoDisplay}\nStacktrace: ${stacktrace.joinToString("\n")}"
        }
    }
}
