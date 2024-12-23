package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.TextFieldSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getStringTODO
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.FileSettingsItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_deny_action
import spmp.shared.generated.resources.s_group_caching

internal fun getMiscCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.Misc.PERSISTENT_QUEUE
        ),

        ToggleSettingsItem(
            context.settings.Misc.ADD_SONGS_TO_HISTORY
        ),

        FileSettingsItem(
            state = context.settings.Misc.LIBRARY_PATH,
            getPathLabel = { path ->
                if (path.isBlank()) {
                    return@FileSettingsItem MediaItemLibrary.getDefaultLibraryDir(context)!!.absolute_path
                }
                else {
                    return@FileSettingsItem path
                    // Format Android documents tree URI to standard path
//                    val split_path: List<String> = URI.create(path).path.split(':')
//                    if (split_path.size == 1) {
//                        return@FileSettingsItem split_path.first().removePrefix("/tree/")
//                    }
//                    else {
//                        val storage: String = split_path.first().split('/').last().capitalize(Locale(context.getUiLanguage()))
//                        return@FileSettingsItem "($storage) ~/${split_path.last()}"
//                    }
                }
            },
            extraContent = {
                val coroutine_scope: CoroutineScope = rememberCoroutineScope()

                IconButton({
                    if (MediaItemLibrary.song_sync_in_progress) {
                        return@IconButton
                    }

                    coroutine_scope.launch {
                        MediaItemLibrary.syncLocalSongs(context)
                    }
                }) {
                    Crossfade(MediaItemLibrary.song_sync_in_progress) { syncing ->
                        if (syncing) {
                            SubtleLoadingIndicator()
                        }
                        else {
                            Icon(Icons.Default.Sync, null)
                        }
                    }
                }
            },
            onSelectRequested = { setValue, showDialog ->
                val path: String? = context.promptUserForDirectory(true)?.uri
                context.coroutineScope.launch {
                    val old_location: PlatformFile = MediaItemLibrary.getLibraryDir(context, context.settings.Misc.LIBRARY_PATH.get())!!
                    val new_location: PlatformFile = MediaItemLibrary.getLibraryDir(context, path ?: "")!!

                    suspend fun processDialogSelection(accepted: Boolean, is_retry: Boolean = false) {
                        if (accepted) {
                            if (old_location.is_directory) {
                                val result: Result<PlatformFile> = old_location.moveDirContentTo(new_location)
                                result.onFailure { error ->
                                    showDialog(
                                        FileSettingsItem.Dialog(
                                            getStringTODO("Transfer failed"),
                                            error.toString(),
                                            getString(Res.string.action_confirm_action),
                                            null
                                        ) {}
                                    )
                                    return@onFailure
                                }
                            }
                        } else if (is_retry) {
                            return
                        }

                        setValue(path ?: "")
                    }

                    if (old_location.uri == new_location.uri) {
                        return@launch
                    }

                    if (!old_location.is_directory) {
                        processDialogSelection(true)
                        return@launch
                    }

                    showDialog(
                        FileSettingsItem.Dialog(
                            getStringTODO("Transfer existing library"),
                            getStringTODO("Move the library at ${old_location.path} to ${new_location.path}?"),
                            getString(Res.string.action_confirm_action),
                            getString(Res.string.action_deny_action)
                        ) { accepted ->
                            processDialogSelection(accepted)
                        }
                    )
                }
            }
        ),

        AppSliderItem(
            context.settings.Misc.NAVBAR_HEIGHT_MULTIPLIER
        ),

        TextFieldSettingsItem(
            context.settings.Misc.STATUS_WEBHOOK_URL,
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.Misc.STATUS_WEBHOOK_PAYLOAD,
            getFieldModifier = { Modifier.appTextField() }
        )
    ) + getCachingGroup(context)
}

private fun getCachingGroup(context: AppContext): List<SettingsItem> {
    return listOf(
        GroupSettingsItem(Res.string.s_group_caching),
        ToggleSettingsItem(
            context.settings.Misc.THUMB_CACHE_ENABLED
        )
    )
}
