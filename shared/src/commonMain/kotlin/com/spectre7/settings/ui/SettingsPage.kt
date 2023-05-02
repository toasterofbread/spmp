package com.spectre7.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.spectre7.settings.model.SettingsGroup
import com.spectre7.settings.model.SettingsItem
import com.spectre7.spmp.platform.BackHandler
import com.spectre7.utils.WidthShrinkText
import com.spectre7.utils.background

abstract class SettingsPage(private val getTitle: (() -> String?)? = null) {
    internal var id: Int? = null
    internal lateinit var settings_interface: SettingsInterface

    open val disable_padding: Boolean = false
    open val scrolling: Boolean = true

    @Composable
    fun Page(content_padding: PaddingValues, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit, goBack: () -> Unit) {
        PageView(content_padding, openPage, openCustomPage, goBack)
        BackHandler {
            goBack()
        }
    }

    @Composable
    fun TitleBar(is_root: Boolean, modifier: Modifier = Modifier, goBack: () -> Unit) {
        Crossfade(getTitle?.invoke()) { title ->
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null)

                if (title != null) {
                    WidthShrinkText(
                        title,
                        Modifier.padding(horizontal = 30.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = settings_interface.theme.on_background,
                            fontWeight = FontWeight.Light
                        )
                    )
                }

                Spacer(Modifier.width(24.dp))
            }
        }
    }

    @Composable
    protected abstract fun PageView(content_padding: PaddingValues, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit, goBack: () -> Unit)

    abstract suspend fun resetKeys()
    open suspend fun onClosed() {}
}

private const val SETTINGS_PAGE_WITH_ITEMS_SPACING = 20f

class SettingsPageWithItems(
    getTitle: () -> String?,
    val getItems: () -> List<SettingsItem>,
    val modifier: Modifier = Modifier
): SettingsPage(getTitle) {

    @Composable
    override fun PageView(
        content_padding: PaddingValues,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit,
        goBack: () -> Unit
    ) {
        Crossfade(getItems()) { items ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SETTINGS_PAGE_WITH_ITEMS_SPACING.dp), contentPadding = content_padding) {
                item {
                    Spacer(Modifier.requiredHeight(SETTINGS_PAGE_WITH_ITEMS_SPACING.dp))
                }

                items(items) { item ->
                    item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
                    item.GetItem(settings_interface.theme, openPage, openCustomPage)
                }
            }
        }
    }

    override suspend fun resetKeys() {
        for (item in getItems()) {
            item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
            item.resetValues()
        }
    }
}
