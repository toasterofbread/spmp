package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.toasterofbread.spmp.resources.getString

@Composable
fun CircularReferenceWarning(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onDismiss) {
                Text(getString("action_close"))
            }
        },
        title = { Text(getString("content_bar_warn_circular_reference")) },
        text = { Text(getString("content_bar_warn_circular_reference_change_reverted")) }
    )
}
