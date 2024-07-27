package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.content_bar_warn_circular_reference
import spmp.shared.generated.resources.content_bar_warn_circular_reference_change_reverted

@Composable
fun CircularReferenceWarning(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
        title = { Text(stringResource(Res.string.content_bar_warn_circular_reference)) },
        text = { Text(stringResource(Res.string.content_bar_warn_circular_reference_change_reverted)) }
    )
}
