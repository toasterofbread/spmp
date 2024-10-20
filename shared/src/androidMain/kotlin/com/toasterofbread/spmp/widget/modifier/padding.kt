package com.toasterofbread.spmp.widget.modifier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.glance.GlanceModifier
import androidx.glance.layout.padding

@Composable
fun GlanceModifier.padding(padding_values: PaddingValues): GlanceModifier =
    padding(
        top = padding_values.calculateTopPadding(),
        bottom = padding_values.calculateBottomPadding(),
        start = padding_values.calculateStartPadding(LocalLayoutDirection.current),
        end = padding_values.calculateEndPadding(LocalLayoutDirection.current)
    )
