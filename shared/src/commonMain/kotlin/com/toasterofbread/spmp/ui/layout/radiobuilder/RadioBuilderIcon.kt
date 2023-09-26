package com.toasterofbread.spmp.ui.layout.radiobuilder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
    Box(modifier.requiredSize(RADIO_BUILDER_ICON_WIDTH_DP.dp, 24.dp)) {
        Icon(Icons.Default.Radio, null, Modifier.align(Alignment.Center))

        val add_icon_size = 15.dp
        Icon(
            Icons.Default.Add,
            null,
            Modifier
                .align(Alignment.TopCenter)
                .size(add_icon_size)
                .offset(
                    x = (-5).dp + ((RADIO_BUILDER_ICON_WIDTH_DP.dp) / 2),
                    y = (-5).dp
                )
        )
    }
}
