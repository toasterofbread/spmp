package com.spectre7.spmp.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.modifier.background

@Composable
fun ErrorInfoDisplay(error: Throwable, modifier: Modifier = Modifier) {
    var expanded: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier
            .animateContentSize()
            .background(RoundedCornerShape(16.dp), Theme.current.accent_provider)
            .clickable {
                expanded = !expanded
            }
    ) {
        val message = if (expanded) null else error.message?.let { " - $it" }
        Text(error::class.java.simpleName + (message ?: ""))

        if (expanded) {
            Text(error.stackTraceToString())
        }
    }
}
