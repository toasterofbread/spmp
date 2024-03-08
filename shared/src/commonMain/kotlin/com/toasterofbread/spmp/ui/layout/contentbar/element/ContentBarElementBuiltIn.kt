package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.platform.visualiser.Visualiser
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import kotlinx.serialization.json.*

class ContentBarElementBuiltIn(data: JsonObject?): ContentBarElement {
    private var type: Type by mutableStateOf(
        data?.get("type")?.jsonPrimitive?.int?.let { type ->
            Type.entries[type]
        } ?: Type.DEFAULT
    )

    private fun getJsonData(): JsonObject = Json.encodeToJsonElement(
        mapOf(
            "type" to type.ordinal
        )
    ).jsonObject

    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.BUILT_IN, data = getJsonData())

    @Composable
    override fun Element(vertical: Boolean, modifier: Modifier) {
        when (type) {
            Type.LYRICS -> ContentBarElementLyrics()
            Type.VISUALISER -> ContentBarElementVisualiser()
        }
    }
    
    @Composable
    override fun Configuration(modifier: Modifier, onModification: () -> Unit) {
        var show_type_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_type_selector,
            onDismissRequest = { show_type_selector = false },
            item_count = Type.entries.size,
            selected = type.ordinal,
            getItem = {
                val type: Type = Type.entries[it]
                val suffix: String =
                    if (type.isAvailable()) ""
                    else getString("content_bar_element_builtin_unsupported_on_platform_suffix")
                type.getName() + suffix
            },
            onSelected = {
                val selected: Type = Type.entries[it]
                if (!selected.isAvailable()) {
                    return@LargeDropdownMenu
                }

                type = selected
                show_type_selector = false
                onModification()
            }
        )

        Column {
            Button({ show_type_selector = !show_type_selector }) {
                Text(getString("content_bar_element_builtin_config_type"))
            }
        }
    }

    enum class Type {
        LYRICS,
        VISUALISER;

        fun getName(): String =
            when (this) {
                LYRICS -> getString("content_bar_element_builtin_lyrics")
                VISUALISER -> getString("content_bar_element_builtin_visualiser")
            }

        fun isAvailable(): Boolean =
            when (this) {
                VISUALISER -> Visualiser.isSupported()
                else -> true
            }

        companion object {
            val DEFAULT: Type = LYRICS
            val ALL: List<Type> = Type.entries.filter { it.isAvailable() }
        }
    }
}
