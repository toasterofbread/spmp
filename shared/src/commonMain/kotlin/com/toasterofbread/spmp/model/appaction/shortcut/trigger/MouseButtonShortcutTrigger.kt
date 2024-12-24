package com.toasterofbread.spmp.ui.component.shortcut.trigger

import androidx.compose.ui.input.key.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.model.appaction.shortcut.LocalShortcutState
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.shortcut_button_config_none_selected
import spmp.shared.generated.resources.shortcut_button_config_selected_button
import spmp.shared.generated.resources.shortcut_button_config_detecting_button

@Serializable
data class MouseButtonShortcutTrigger(
    val button_code: Int? = null
): ShortcutTrigger {
    fun isTriggeredBy(button_code: Int): Boolean {
        return button_code == this.button_code
    }

    override fun getType(): ShortcutTrigger.Type =
        ShortcutTrigger.Type.MOUSE_BUTTON

    @Composable
    override fun IndicatorContent(modifier: Modifier) {
        Text(
            button_code?.toString() ?: stringResource(Res.string.shortcut_button_config_none_selected),
            modifier,
            softWrap = false
        )
    }

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (ShortcutTrigger) -> Unit) {
        var detecting_button: Boolean by remember { mutableStateOf(false) }
        val shortcut_state: ShortcutState = LocalShortcutState.current

        DisposableEffect(detecting_button) {
            if (detecting_button) {
                shortcut_state.setButtonDetectionState {
                    onModification(copy(button_code = it))
                    detecting_button = false
                }
            }

            onDispose {
                shortcut_state.setButtonDetectionState(null)
            }
        }

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.shortcut_button_config_selected_button),
                Modifier.align(Alignment.CenterVertically)
            )

            Button({
                detecting_button = !detecting_button
            }) {
                if (detecting_button) {
                    Text(stringResource(Res.string.shortcut_button_config_detecting_button))
                }
                else {
                    IndicatorContent(Modifier)
                }
            }
        }
    }
}
