package com.toasterofbread.spmp.ui.layout.radiobuilder

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadioBuilderIcon(modifier: Modifier = Modifier) {
    Row(modifier.requiredSize(RADIO_BUILDER_ICON_WIDTH.dp, 25.dp)) {
        Icon(Icons.Default.Radio, null, Modifier.align(Alignment.CenterVertically))
        Icon(Icons.Default.Add, null, Modifier.align(Alignment.Top))
    }
}
