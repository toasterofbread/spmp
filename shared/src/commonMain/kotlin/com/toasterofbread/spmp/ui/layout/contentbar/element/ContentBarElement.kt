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
import com.toasterofbread.spmp.platform.visualiser.Visualiser

interface ContentBarElement {
    fun getData(): ContentBarElementData

    @Composable
    fun isSelected(): Boolean = false

    @Composable
    fun shouldShow(): Boolean = true

    @Composable
    fun Element(vertical: Boolean, modifier: Modifier)

    @Composable
    fun Configuration(modifier: Modifier, onModification: () -> Unit)

    enum class Type {
        BUTTON,
        SPACER,
        LYRICS,
        VISUALISER;

        fun isAvailable(): Boolean =
            when (this) {
                VISUALISER -> Visualiser.isSupported()
                else -> true
            }

        fun getName(): String =
            when (this) {
                BUTTON -> getString("content_bar_element_type_button")
                SPACER -> getString("content_bar_element_type_spacer")
                LYRICS -> getString("content_bar_element_type_lyrics")
                VISUALISER -> getString("content_bar_element_type_visualiser")
            }

        fun getIcon(): ImageVector =
            when (this) {
                BUTTON -> Icons.Default.TouchApp
                SPACER -> Icons.Default.Expand
                LYRICS -> Icons.Default.MusicNote
                VISUALISER -> Icons.Default.Waves
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
            ContentBarElement.Type.BUTTON -> ContentBarElementButton(data)
            ContentBarElement.Type.SPACER -> ContentBarElementSpacer(data)
            ContentBarElement.Type.LYRICS -> ContentBarElementLyrics(data)
            ContentBarElement.Type.VISUALISER -> ContentBarElementVisualiser(data)
        }
}
