package com.toasterofbread.spmp.model.settings.category

import dev.toastbits.composekit.settings.ui.item.ComposableSettingsItem
import dev.toastbits.composekit.utils.common.thenIf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Button
import com.toasterofbread.spmp.SpMpDeps
import com.toasterofbread.spmp.DependencyInfo
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.AppContext
import LocalPlayerState
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_dependencies
import spmp.shared.generated.resources.s_cat_desc_dependencies
import spmp.shared.generated.resources.dependency_list_title
import spmp.shared.generated.resources.dependency_list_dep_using_fork
import spmp.shared.generated.resources.`dependency_list_dep_$author`
import spmp.shared.generated.resources.`dependency_list_dep_$license`
import spmp.shared.generated.resources.dependency_list_dep_view_source

class DependencySettings(val context: AppContext): SettingsGroup("DEPENDENCY", context.getPrefs()) {
    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_dependencies) },
            { stringResource(Res.string.s_cat_desc_dependencies) },
            { listOf(
                ComposableSettingsItem {
                    DependencyList(Modifier.fillMaxSize())
                }
            ) },
            { Icons.Outlined.LibraryBooks },
            titleBarEndContent = { modifier ->
                val player: PlayerState = LocalPlayerState.current

                if (player.context.canOpenUrl()) {
                    IconButton(
                        { player.context.openUrl("https://github.com/toasterofbread/spmp/blob/main/buildSrc/src/main/kotlin/plugins/spmp/Dependencies.kt") },
                        modifier
                    ) {
                        Icon(Icons.Default.OpenInNew, null)
                    }
                }
            }
        )
}

@Composable
private fun DependencyList(modifier: Modifier = Modifier) {
    val dependencies: List<DependencyInfo> = remember { SpMpDeps.dependencies.values.sortedBy { it.name } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(stringResource(Res.string.dependency_list_title), style = MaterialTheme.typography.titleSmall)

        for (dependency in dependencies) {
            DependencyInfo(dependency, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DependencyInfo(dependency: DependencyInfo, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    Column(
        modifier
            .background(
                player.theme.vibrant_accent.copy(alpha = 0.2f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                dependency.name,
                Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.fillMaxWidth().weight(1f))

            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelSmall) {
                if (dependency.fork_url != null) {
                    Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.dependency_list_dep_using_fork), Modifier.thenIf(player.context.canOpenUrl()) { offset(x = 10.dp) })

                        if (player.context.canOpenUrl()) {
                            IconButton({ player.context.openUrl(dependency.fork_url) }) {
                                Icon(Icons.Default.OpenInNew, null, Modifier.size(15.dp))
                            }
                        }
                    }
                }

                if (dependency.version != null) {
                    Text(dependency.version, Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelMedium) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.`dependency_list_dep_$author`).replace("\$author", dependency.author), Modifier.align(Alignment.CenterVertically))

                Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.`dependency_list_dep_$license`).replace("\$license", dependency.license))

                    if (player.context.canOpenUrl()) {
                        IconButton(
                            { player.context.openUrl(dependency.license_url) },
                            Modifier.offset(x = (-10).dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
                        }
                    }
                }

                if (player.context.canOpenUrl()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Button({ player.context.openUrl(dependency.url) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, null, Modifier.padding(end = 10.dp))
                                Text(stringResource(Res.string.dependency_list_dep_view_source))
                            }
                        }
                    }
                }
            }
        }
    }
}
