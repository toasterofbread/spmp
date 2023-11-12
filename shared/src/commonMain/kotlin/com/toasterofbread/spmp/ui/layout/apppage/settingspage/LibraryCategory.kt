package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.toasterofbread.composekit.settings.ui.item.SettingsFileItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import java.net.URI

internal fun getLibraryCategory(context: AppContext): List<SettingsItem> {
    return listOf(
        SettingsFileItem(
            SettingsValueState(Settings.KEY_LIBRARY_PATH.name),
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
                context.promptForUserDirectory(true) { path ->
                    val old_location = MediaItemLibrary.getLibraryDir(context, Settings.KEY_LIBRARY_PATH.get())
                    val new_location = MediaItemLibrary.getLibraryDir(context, path ?: "")

                    if (old_location.uri == new_location.uri) {
                        return@promptForUserDirectory
                    }

                    fun processDialogSelection(accepted: Boolean, is_retry: Boolean = false) {
                        if (accepted) {
                            val result = old_location.moveDirContentTo(new_location)
                            result.onFailure { error ->
                                showDialog(
                                    SettingsFileItem.Dialog(
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
                        SettingsFileItem.Dialog(
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
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_SHOW_LIKES_PLAYLIST.name),
            getString("s_key_show_likes_playlist"), null
        )
    )
}