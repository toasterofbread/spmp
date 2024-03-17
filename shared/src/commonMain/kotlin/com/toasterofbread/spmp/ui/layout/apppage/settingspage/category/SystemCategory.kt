package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.composekit.settings.ui.item.DropdownSettingsItem
import com.toasterofbread.composekit.settings.ui.item.FileSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem.Companion.ItemTitleText
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.Languages
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URI

// TODO Allow setting to any language
fun getLanguageDropdownItem(
    key: SettingsKey,
    available_languages: List<Languages.LanguageInfo>,
    title: String,
    subtitle: String?
): SettingsItem {
    return DropdownSettingsItem(
        SettingsValueState(
            key.getName(),
            getValueConverter = {
                val language_code = it as String
                if (language_code.isBlank()) {
                    return@SettingsValueState 0
                }

                val index = available_languages.indexOfFirst { it.code == language_code }
                if (index == -1) {
                    key.set(null)
                    return@SettingsValueState 0
                }
                else {
                    return@SettingsValueState index + 1
                }
            },
            setValueConverter = { index ->
                if (index == 0) {
                    ""
                }
                else {
                    available_languages[index - 1].code
                }
            }
        ),
        title, subtitle,
        available_languages.size + 1,
        { i ->
            if (i == 0) {
                getString("system_language")
            }
            else {
                available_languages[i - 1].readable_name
            }
        }
    ) { i ->
        if (i == 0) {
            getString("system_language")
        }
        else {
            val lang = available_languages[i - 1]
            "${lang.code} / ${lang.readable_name}"
        }
    }
}

internal fun getSystemCategoryItems(context: AppContext): List<SettingsItem> {
    val language: String = context.getUiLanguage()
    val available_languages: List<Languages.LanguageInfo> = Languages.loadAvailableLanugages(context)

    return listOf(
        ComposableSettingsItem {
            WidthShrinkText(getString("language_change_restart_notice"))
        },

        getLanguageDropdownItem(
            SystemSettings.Key.LANG_UI,
            available_languages,
            getString("s_key_interface_lang"), getString("s_sub_interface_lang")
        ),

        getLanguageDropdownItem(
            SystemSettings.Key.LANG_DATA,
            available_languages,
            getString("s_key_data_lang"), getString("s_sub_data_lang")
        ),

        DropdownSettingsItem(
            SettingsValueState(SystemSettings.Key.FONT.getName()),
            getString("s_key_font"),
            null,
            FontMode.entries.size,
        ) { index ->
            FontMode.entries[index].getReadable(language)
        },

        ComposableSettingsItem(
            listOf(SystemSettings.Key.UI_SCALE.getName()),
            resetSettingsValues = {
                SystemSettings.Key.UI_SCALE.set(1f)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemTitleText(getString("s_key_ui_scale"), theme, Modifier.weight(1f))

                Spacer(Modifier.fillMaxWidth().weight(1f))

                var ui_scale: Float by SystemSettings.Key.UI_SCALE.rememberMutableState()

                ShapedIconButton({
                    ui_scale = (ui_scale - 0.1f).coerceAtLeast(0.1f)
                }) {
                    Icon(Icons.Default.Remove, null)
                }

                Text("${(ui_scale * 100).toInt()}%")

                ShapedIconButton({
                    ui_scale += 0.1f
                }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },

        ToggleSettingsItem(
            SettingsValueState(SystemSettings.Key.PERSISTENT_QUEUE.getName()),
            getString("s_key_persistent_queue"),
            getString("s_sub_persistent_queue")
        ),

        ToggleSettingsItem(
            SettingsValueState(SystemSettings.Key.ADD_SONGS_TO_HISTORY.getName()),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        ),

        FileSettingsItem(
            state = SettingsValueState(key = SystemSettings.Key.LIBRARY_PATH.getName()),
            title = getString("s_key_library_path"),
            subtitle = getString("s_sub_library_path"),
            getPathLabel = { path ->
                if (path.isBlank()) {
                    return@FileSettingsItem MediaItemLibrary.getDefaultLibraryDir(context).absolute_path
                }
                else {
                    // Format Android documents tree URI to standard path
                    val split_path: List<String> = URI.create(path).path.split(':')
                    if (split_path.size == 1) {
                        return@FileSettingsItem split_path.first().removePrefix("/tree/")
                    }
                    else {
                        val storage: String = split_path.first().split('/').last().capitalize(Locale(context.getUiLanguage()))
                        return@FileSettingsItem "($storage) ~/${split_path.last()}"
                    }
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
                context.promptUserForDirectory(true) { path ->
                    val old_location: PlatformFile = MediaItemLibrary.getLibraryDir(context, SystemSettings.Key.LIBRARY_PATH.get())
                    val new_location: PlatformFile = MediaItemLibrary.getLibraryDir(context, path ?: "")

                    fun processDialogSelection(accepted: Boolean, is_retry: Boolean = false) {
                        if (accepted) {
                            if (old_location.is_directory) {
                                val result: Result<PlatformFile> = old_location.moveDirContentTo(new_location)
                                result.onFailure { error ->
                                    showDialog(
                                        FileSettingsItem.Dialog(
                                            getStringTODO("Transfer failed"),
                                            error.toString(),
                                            getString("action_confirm_action"),
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
                        return@promptUserForDirectory
                    }

                    if (!old_location.is_directory) {
                        processDialogSelection(true)
                        return@promptUserForDirectory
                    }

                    showDialog(
                        FileSettingsItem.Dialog(
                            getStringTODO("Transfer existing library"),
                            getStringTODO("Move the library at ${old_location.path} to ${new_location.path}?"),
                            getString("action_confirm_action"),
                            getString("action_deny_action")
                        ) { accepted ->
                            processDialogSelection(accepted)
                        }
                    )
                }
            }
        )
    )
}
