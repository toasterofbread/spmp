package com.toasterofbread.spmp.ui.component

import dev.toastbits.ytmkt.model.ApiImplementable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ApiImplementable.NotImplementedMessage(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(getNotImplementedMessage())
    }
}
