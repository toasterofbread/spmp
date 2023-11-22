package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.composekit.settings.ui.item.DropdownSettingsItem
import com.toasterofbread.composekit.settings.ui.item.FileSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
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
            FontMode.values().size,
        ) { index ->
            FontMode.values()[index].getReadable(language)
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
            SettingsValueState(SystemSettings.Key.LIBRARY_PATH.getName()),
            getString("s_key_library_path"),
            getString("s_sub_library_path"),
            { path ->
                val player = LocalPlayerState.current
                if (path.isBlank()) MediaItemLibrary.getDefaultLibraryDir(player.context).let { location ->
                    location.absolute_path ?: location.path
                }
                else {
                    // Format Android documents tree URI to standard path
                    val split_path = URI.create(path).path.split(':')
                    if (split_path.size == 1) {
                        split_path.first().removePrefix("/tree/")
                    }
                    else {
                        val storage = split_path.first().split('/').last().capitalize(Locale(player.context.getUiLanguage()))
                        "($storage) ~/${split_path.last()}"
                    }
                }
            },
            { setValue, showDialog ->
                context.promptUserForDirectory(true) { path ->
                    val old_location = MediaItemLibrary.getLibraryDir(context, SystemSettings.Key.LIBRARY_PATH.get())
                    val new_location = MediaItemLibrary.getLibraryDir(context, path ?: "")

                    if (old_location.uri == new_location.uri) {
                        return@promptUserForDirectory
                    }

                    fun processDialogSelection(accepted: Boolean, is_retry: Boolean = false) {
                        if (accepted) {
                            val result = old_location.moveDirContentTo(new_location)
                            result.onFailure { error ->
                                showDialog(
                                    FileSettingsItem.Dialog(
                                        getStringTODO("Transfer failed"),
                                        error.toString(),
                                        getString("action_confirm_action"),
                                        getString("action_cancel")
                                    ) { accepted ->
                                        processDialogSelection(accepted, true)
                                    }
                                )
                                return@onFailure
                            }
                        }
                        else if (is_retry) {
                            return
                        }

                        setValue(path ?: "")
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
