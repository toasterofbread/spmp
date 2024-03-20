package com.toasterofbread.spmp.ui.layout.contentbar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.model.settings.category.LayoutSettings

@Serializable
data class ContentBarReference(val type: Type, val index: Int) {
    enum class Type {
        INTERNAL,
        CUSTOM
    }

    fun getBar(): ContentBar? =
        when (type) {
            Type.INTERNAL -> InternalContentBar.ALL.getOrNull(index)
            Type.CUSTOM -> {
                val bars: List<CustomContentBar> = Json.decodeFromString(LayoutSettings.Key.CUSTOM_BARS.get())
                bars.getOrNull(index)
            }
        }

    companion object {
        fun ofInternalBar(internal_bar: InternalContentBar): ContentBarReference =
            ContentBarReference(Type.INTERNAL, internal_bar.index)

        fun ofCustomBar(bar_index: Int): ContentBarReference =
            ContentBarReference(Type.CUSTOM, bar_index)
    }
}
