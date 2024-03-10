package com.toasterofbread.spmp.ui.layout.contentbar.element

import kotlinx.serialization.json.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

class ContentBarElementVisualiser(data: JsonObject?): ContentBarElement {
    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.VISUALISER)

    @Composable
    override fun Element(vertical: Boolean, bar_width: Dp, modifier: Modifier) {

    }

    @Composable
    override fun ConfigurationItems(modifier: Modifier, onModification: () -> Unit) {
    }
}
