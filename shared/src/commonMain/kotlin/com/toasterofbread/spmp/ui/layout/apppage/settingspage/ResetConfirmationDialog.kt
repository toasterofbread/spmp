package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_deny_action
import spmp.shared.generated.resources.prompt_confirm_action
import spmp.shared.generated.resources.prompt_confirm_settings_page_reset

@Composable
internal fun ResetConfirmationDialog(show_state: MutableState<Boolean>, reset: suspend () -> Unit) {
    var do_reset: Boolean by remember { mutableStateOf(false) }
    LaunchedEffect(do_reset) {
        if (do_reset) {
            show_state.value = false
            reset()
        }
    }

    if (show_state.value) {
        AlertDialog(
            { show_state.value = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        do_reset = true
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm_action))
                }
            },
            dismissButton = { TextButton({ show_state.value = false }) { Text(stringResource(Res.string.action_deny_action)) } },
            title = { Text(stringResource(Res.string.prompt_confirm_action)) },
            text = {
                Text(stringResource(Res.string.prompt_confirm_settings_page_reset))
            }
        )
    }
}
