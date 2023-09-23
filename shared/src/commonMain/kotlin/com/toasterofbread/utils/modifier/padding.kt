package com.toasterofbread.utils.modifier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val PaddingValues.horizontal: PaddingValues
    @Composable
    get() = PaddingValues(
        start = calculateStartPadding(LocalLayoutDirection.current),
        end = calculateEndPadding(LocalLayoutDirection.current)
    )

@Composable
fun PaddingValues.getHorizontal(add: Dp = 0.dp): PaddingValues =
    PaddingValues(
        start = calculateStartPadding(LocalLayoutDirection.current) + add,
        end = calculateEndPadding(LocalLayoutDirection.current) + add
    )

val PaddingValues.vertical: PaddingValues
    get() = PaddingValues(
        top = calculateTopPadding(),
        bottom = calculateBottomPadding()
    )
