package com.toasterofbread.spmp.ui.layout.contentbar.element

import kotlinx.serialization.json.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class ContentBarElementVisualiser(data: JsonObject?): ContentBarElement {
    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.VISUALISER)

    @Composable
    override fun Element(vertical: Boolean, modifier: Modifier) {

    }

    @Composable
    override fun Configuration(modifier: Modifier, onModification: () -> Unit) {
    }
}
