package com.spectre7.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectre7.settings.model.SettingsGroup
import com.spectre7.settings.model.SettingsItem
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.utils.composable.WidthShrinkText

abstract class SettingsPage(
    private val getTitle: (() -> String?)? = null,
    private val getIcon: (@Composable () -> ImageVector?)? = null
) {
    var id: Int? = null
        internal set
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
                val icon = getIcon?.invoke()
                if (icon != null) {
                    Icon(icon, null)
                }

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

                if (icon != null) {
                    Spacer(Modifier.width(24.dp))
                }
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
    val modifier: Modifier = Modifier,
    getIcon: (@Composable () -> ImageVector?)? = null
): SettingsPage(getTitle, getIcon) {

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

                items(items.size) { i ->
                    val item = items[i]
                    item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)

                    if (i != 0 && item is SettingsGroup) {
                        Spacer(Modifier.height(30.dp))
                    }
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
