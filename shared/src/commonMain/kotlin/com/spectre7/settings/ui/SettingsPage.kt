package com.spectre7.composesettings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.spectre7.settings.model.SettingsGroup
import com.spectre7.settings.model.SettingsItem
import com.spectre7.spmp.platform.BackHandler
import com.spectre7.utils.WidthShrinkText

abstract class SettingsPage(val title: String?) {
    internal var id: Int? = null
    internal lateinit var settings_interface: SettingsInterface

    open val disable_padding: Boolean = false
    open val scrolling: Boolean = true

    @Composable
    fun Page(openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit, goBack: () -> Unit) {
        PageView(openPage, openCustomPage, goBack)
        BackHandler {
            goBack()
        }
    }

    @Composable
    fun TitleBar(is_root: Boolean, modifier: Modifier = Modifier, goBack: () -> Unit) {
        if (title != null) {
            WidthShrinkText(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = settings_interface.theme.on_background,
                    fontWeight = FontWeight.Bold
                ),
                modifier = modifier
            )
        }
    }

    @Composable
    protected abstract fun PageView(openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit, goBack: () -> Unit)

    abstract suspend fun resetKeys()
    open suspend fun onClosed() {}
}

class SettingsPageWithItems(
    title: String?,
    val items: List<SettingsItem>,
    val modifier: Modifier = Modifier
): SettingsPage(title) {

    @Composable
    override fun PageView(
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit,
        goBack: () -> Unit,
    ) {
        val spacing = 20.dp
        Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            if (items.isNotEmpty() && items[0] !is SettingsGroup) {
                Spacer(Modifier.requiredHeight(spacing))
            }

            for (item in items) {
                item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
                item.GetItem(settings_interface.theme, openPage, openCustomPage)
            }
        }
    }

    override suspend fun resetKeys() {
        for (item in items) {
            item.initialise(settings_interface.context, settings_interface.prefs, settings_interface.default_provider)
            item.resetValues()
        }
    }
}
