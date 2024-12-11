package com.toasterofbread.spmp.widget.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import com.toasterofbread.spmp.widget.modifier.padding
import dev.toastbits.composekit.components.utils.modifier.horizontal

@Composable
internal fun GlanceLazyColumn(content_padding: PaddingValues, modifier: GlanceModifier, content: LazyListScope.() -> Unit) {
    androidx.glance.appwidget.lazy.LazyColumn(
        modifier.padding(content_padding.horizontal)
    ) {
        item {
            Spacer(GlanceModifier.height(content_padding.calculateTopPadding()))
        }
        content()
        item {
            Spacer(GlanceModifier.height(content_padding.calculateBottomPadding()))
        }
    }
}
