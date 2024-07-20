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
import dev.toastbits.composekit.platform.PlatformFile
import dev.toastbits.composekit.settings.ui.item.ComposableSettingsItem
import dev.toastbits.composekit.settings.ui.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.item.FileSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem.Companion.ItemTitleText
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import LocalPlayerState
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getAvailableLanguages
import com.toasterofbread.spmp.resources.getStringTODO
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.system_language
import spmp.shared.generated.resources.language_change_restart_notice
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_deny_action

// TODO Allow setting to any language
fun getLanguageDropdownItem(
    property: PreferencesProperty<String>,
    available_languages: List<Language>
): SettingsItem {
    return DropdownSettingsItem(
        property.getConvertedProperty(
            fromProperty = { language_code: String ->
                if (language_code.isBlank()) {
                    return@getConvertedProperty 0
                }

                val index = available_languages.indexOfFirst { it.identifier == language_code }
                if (index == -1) {
                    property.reset()
                    return@getConvertedProperty 0
                }
                else {
                    return@getConvertedProperty index + 1
                }
            },
            toProperty = { index: Int ->
                if (index == 0) {
                    ""
                }
                else {
                    available_languages[index - 1].identifier
                }
            }
        ),
        available_languages.size + 1,
        { i ->
            if (i == 0) {
                stringResource(Res.string.system_language)
            }
            else {
                available_languages[i - 1].readable_name
            }
        }
    ) { i ->
        if (i == 0) {
            stringResource(Res.string.system_language)
        }
        else {
            val lang = available_languages[i - 1]
            "${lang.identifier} / ${lang.readable_name}"
        }
    }
}

internal fun getSystemCategoryItems(context: AppContext, available_languages: List<Language>): List<SettingsItem> =
    listOf(
        ComposableSettingsItem {
            WidthShrinkText(stringResource(Res.string.language_change_restart_notice))
        },

        getLanguageDropdownItem(
            context.settings.system.LANG_UI,
            available_languages
        ),

        getLanguageDropdownItem(
            context.settings.system.LANG_DATA,
            available_languages
        ),

        DropdownSettingsItem(
            context.settings.system.FONT
        ) { mode ->
            mode.getReadable(context.observeUiLanguage().value)
        },

        ComposableSettingsItem(
            listOf(context.settings.system.UI_SCALE),
            resetSettingsValues = {
                context.settings.system.UI_SCALE.set(1f)
            }
        ) {
            val player: PlayerState = LocalPlayerState.current

            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemTitleText(context.settings.system.UI_SCALE.getName(), theme, Modifier.weight(1f))

                Spacer(Modifier.fillMaxWidth().weight(1f))

                var ui_scale: Float by player.settings.system.UI_SCALE.observe()

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
            context.settings.system.PERSISTENT_QUEUE
        ),

        ToggleSettingsItem(
            context.settings.system.ADD_SONGS_TO_HISTORY
        ),

        FileSettingsItem(
            state = context.settings.system.LIBRARY_PATH,
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
                context.promptUserForDirectory(true) { path ->
                    context.coroutine_scope.launch {
                        val old_location: PlatformFile = MediaItemLibrary.getLibraryDir(context, context.settings.system.LIBRARY_PATH.get())!!
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
            }
        )
    )
