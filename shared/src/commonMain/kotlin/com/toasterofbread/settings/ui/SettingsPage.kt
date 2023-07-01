package com.toasterofbread.composesettings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.settings.model.SettingsGroup
import com.toasterofbread.settings.model.SettingsItem
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText
import kotlin.math.ceil

abstract class SettingsPage {
    var id: Int? = null
        internal set
    internal lateinit var settings_interface: SettingsInterface

    open val disable_padding: Boolean = false
    open val scrolling: Boolean = true

    open val title: String? = null
    open val icon: ImageVector?
        @Composable
        get() = null

    @Composable
    fun Page(content_padding: PaddingValues, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit, goBack: () -> Unit) {
        PageView(content_padding, openPage, openCustomPage, goBack)
        BackHandler {
            goBack()
        }
    }

    @Composable
    fun TitleBar(is_root: Boolean, modifier: Modifier = Modifier, goBack: () -> Unit) {
        Crossfade(title, modifier) { title ->
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    icon?.also {
                        Icon(it, null)
                    }

                    if (title != null) {
                        WidthShrinkText(
                            title,
                            Modifier.padding(horizontal = 30.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = settings_interface.theme.on_background,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    if (icon != null) {
                        Spacer(Modifier.width(24.dp))
                    }
                }

                WaveBorder(
                    Modifier.requiredWidth(SpMp.context.getScreenWidth())
                )
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
    val getTitle: () -> String?,
    val getItems: () -> List<SettingsItem>,
    val modifier: Modifier = Modifier,
    val getIcon: (@Composable () -> ImageVector?)? = null
): SettingsPage() {

    override val title: String?
        get() = getTitle()
    override val icon: ImageVector?
        @Composable
        get() = getIcon?.invoke()

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
