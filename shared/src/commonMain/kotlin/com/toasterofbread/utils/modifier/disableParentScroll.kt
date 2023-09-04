package com.toasterofbread.utils.modifier

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.toasterofbread.utils.common.thenIf

@Composable
fun Modifier.disableParentScroll(disable_x: Boolean = true, disable_y: Boolean = true, child_does_not_scroll: Boolean = false) =
    nestedScroll(remember { object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            return Offset(
                if (disable_x) available.x else 0f,
                if (disable_y) available.y else 0f
            )
        }
    } })
    .thenIf(child_does_not_scroll) {
        verticalScroll(rememberScrollState())
    }
