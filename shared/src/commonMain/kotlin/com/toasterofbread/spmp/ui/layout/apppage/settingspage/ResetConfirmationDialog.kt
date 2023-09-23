package com.toasterofbread.spmp.ui.layout.apppage.settingspage

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
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString

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
        PlatformAlertDialog(
            { show_state.value = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        do_reset = true
                    }
                ) {
                    Text(getString("action_confirm_action"))
                }
            },
            dismissButton = { TextButton({ show_state.value = false }) { Text(getString("action_deny_action")) } },
            title = { Text(getString("prompt_confirm_action")) },
            text = {
                Text(getString("prompt_confirm_settings_page_reset"))
            }
        )
    }
}
