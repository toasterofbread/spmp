package com.spectre7.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.theme.Theme

@Composable
fun MainPageLoadingView(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(getString("loading_feed"), Modifier.alpha(0.4f), fontSize = 12.sp, color = Theme.current.on_background)
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            Modifier
                .alpha(0.4f)
                .fillMaxWidth(0.35f), color = Theme.current.on_background
        )
    }
}
