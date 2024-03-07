package com.toasterofbread.spmp.ui.layout.contentbar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.resources.getString
import kotlinx.serialization.encodeToString
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.PaddingValues

@Serializable
data class CustomContentBar(
    val bar_name: String,
    val buttons: List<ContentBarButton> = emptyList(),
    val alignment: Int = -1
): ContentBar() {
    enum class Alignment {
        START, CENTER, END;
    }

    override fun getName(): String = bar_name
    override fun getDescription(): String? = null
    override fun getIcon(): ImageVector = Icons.Default.Build

    @Composable
    override fun BarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        Text(buttons.toString())
        return true
    }
}
