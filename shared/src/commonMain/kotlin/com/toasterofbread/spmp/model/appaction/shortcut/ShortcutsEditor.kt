package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyRow
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.ui.component.shortcut.ShortcutPreview
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalPlayerState
import dev.toastbits.composekit.components.utils.composable.StickyHeightColumn
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_navigate_song_with_numbers

@Composable
fun ShortcutsEditor(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    var shortcuts: List<Shortcut>? by player.settings.Shortcut.CONFIGURED_SHORTCUTS.observe()
    val default_shortcuts: List<Shortcut> = remember { getDefaultShortcuts() }

    StickyHeightColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var navigate_song_with_numbers: Boolean by player.settings.Shortcut.NAVIGATE_SONG_WITH_NUMBERS.observe()
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(Res.string.s_key_navigate_song_with_numbers), Modifier.align(Alignment.CenterVertically))

            Switch(
                navigate_song_with_numbers,
                { navigate_song_with_numbers = it }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Add, null)

            ScrollBarLazyRow(
                Modifier.fillMaxWidth().weight(1f).height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(AppAction.Type.entries) { action_type ->
                    Button({
                        val new_shortcut: Shortcut = Shortcut(trigger = null, action = action_type.createAction())
                        shortcuts = listOf(new_shortcut) + (shortcuts ?: default_shortcuts)
                    }) {
                        action_type.Preview()
                    }
                }
            }
        }

        for ((index, shortcut) in (shortcuts ?: default_shortcuts).withIndex()) {
            ShortcutPreview(
                shortcut,
                onModification = { new_shortcut ->
                    shortcuts = (shortcuts ?: default_shortcuts).toMutableList().apply {
                        if (new_shortcut == null) {
                            removeAt(index)
                        }
                        else {
                            set(index, new_shortcut)
                        }
                    }
                }
            )
        }
    }
}
