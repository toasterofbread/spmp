package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ComposableSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.button_android_monet_open_dependency
import spmp.shared.generated.resources.info_android_monet
import spmp.shared.generated.resources.url_android_monet_open_dependency

internal fun getExperimentalCategoryItems(context: AppContext): List<SettingsItem> =
    listOf(
        ToggleSettingsItem(context.settings.Experimental.ANDROID_MONET_COLOUR_ENABLE),

        ComposableSettingsItem(resetComposeUiState = {}) { modifier ->
            Column(
                modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(stringResource(Res.string.info_android_monet))

                val fork_page_url: String = stringResource(Res.string.url_android_monet_open_dependency)
                Button(
                    {
                        context.openUrl(fork_page_url)
                    },
                    Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(Res.string.button_android_monet_open_dependency))
                }
            }
        }
    )
