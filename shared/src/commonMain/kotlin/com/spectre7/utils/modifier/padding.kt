package com.toasterofbread.utils.modifier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

val PaddingValues.horizontal: PaddingValues
    @Composable
    get() = PaddingValues(
        start = calculateStartPadding(LocalLayoutDirection.current),
        end = calculateEndPadding(LocalLayoutDirection.current)
    )

val PaddingValues.vertical: PaddingValues
    get() = PaddingValues(
        top = calculateTopPadding(),
        bottom = calculateBottomPadding()
    )
