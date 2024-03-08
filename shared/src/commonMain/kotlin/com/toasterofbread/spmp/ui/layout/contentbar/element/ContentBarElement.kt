package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.spmp.resources.getString

interface ContentBarElement {
    fun getData(): ContentBarElementData

    @Composable
    fun isSelected(): Boolean = false

    @Composable
    fun Element(vertical: Boolean, modifier: Modifier)

    @Composable
    fun Configuration(modifier: Modifier, onModification: () -> Unit)

    enum class Type {
        BUILT_IN,
        BUTTON,
        SPACER;

        fun getName(): String =
            when (this) {
                BUILT_IN -> getString("content_bar_element_type_builtin")
                BUTTON -> getString("content_bar_element_type_button")
                SPACER -> getString("content_bar_element_type_spacer")
            }

        fun getIcon(): ImageVector =
            when (this) {
                BUILT_IN -> Icons.Default.Fullscreen
                BUTTON -> Icons.Default.TouchApp
                SPACER -> Icons.Default.Expand
            }
    }
}

@Serializable
data class ContentBarElementData(
    val type: ContentBarElement.Type,
    val data: JsonObject? = null
) {
    fun toElement(): ContentBarElement =
        when (type) {
            ContentBarElement.Type.BUILT_IN -> ContentBarElementBuiltIn(data)
            ContentBarElement.Type.BUTTON -> ContentBarElementButton(data)
            ContentBarElement.Type.SPACER -> ContentBarElementSpacer(data)
        }
}
