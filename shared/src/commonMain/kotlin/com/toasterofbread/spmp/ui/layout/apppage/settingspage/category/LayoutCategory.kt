package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import androidx.compose.runtime.Composable
import com.toasterofbread.composekit.settings.ui.SettingsInterface
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import kotlinx.serialization.json.Json
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import org.burnoutcrew.reorderable.ReorderableItem
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlotEditor
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.items
import LocalPlayerState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.foundation.layout.fillMaxSize
import org.burnoutcrew.reorderable.detectReorder
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color

internal fun getLayoutCategoryItem(): SettingsItem =
    ComposableSettingsItem(
        listOf(
            LayoutSettings.Key.PORTRAIT_SLOTS.getName(),
            LayoutSettings.Key.LANDSCAPE_SLOTS.getName(),
            LayoutSettings.Key.CUSTOM_BARS.getName()
        ),
        composable = {
            LayoutSlotEditor(it)
        }
    )
