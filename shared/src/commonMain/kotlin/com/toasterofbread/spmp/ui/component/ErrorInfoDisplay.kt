package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.NoRipple
import com.toasterofbread.utils.modifier.background
import kotlin.math.roundToInt

// TODO
@Composable
fun ErrorInfoDisplay(error: Throwable, modifier: Modifier = Modifier) {
    var expanded: Boolean by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    NoRipple {
        Column(
            modifier
                .animateContentSize()
                .background(shape, Theme.current.accent_provider)
                .padding(10.dp)
                .clickable {
                    expanded = !expanded
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val message = if (expanded) null else error.message?.let { " - $it" }
            Text(error::class.java.simpleName + (message ?: ""), color = Theme.current.on_accent)

            if (expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(shape, Theme.current.background_provider)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        error.stackTraceToString(),
                        Modifier.verticalScroll(rememberScrollState())
                    )

                    Button({ throw error }) {
                        Text(getStringTODO("Throw"))
                    }
                }
            }
        }
    }
}
