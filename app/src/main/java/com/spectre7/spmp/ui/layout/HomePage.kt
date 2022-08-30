package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun HomePage() {
    Column {
        SearchPage()
        Button(onClick = { /*TODO*/ }, modifier = Modifier.requiredHeight(55.dp).fillMaxWidth().offset(y = (-30).dp), contentPadding = PaddingValues(10.dp), shape = RectangleShape) {
            Row(modifier = Modifier.fillMaxHeight()) {
                Text("Hello World!", modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}