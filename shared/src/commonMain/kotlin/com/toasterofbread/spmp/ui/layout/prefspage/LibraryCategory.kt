package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.settings.ui.item.SettingsFileItem
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import java.net.URI

internal fun getLibraryCategory(): List<SettingsItem> {
    return listOf(
        SettingsFileItem(
            SettingsValueState(Settings.KEY_LIBRARY_PATH.name),
            getString("s_key_library_path"),
            getString("s_sub_library_path"),
            { path ->
                if (path.isBlank()) MediaItemLibrary.getDefaultStorageLocation(SpMp.context).let { location ->
                    location.absolute_path ?: location.path
                }
                else {
                    // Format Android documents tree URI to standard path
                    val split_path = URI.create(path).path.split(':')
                    if (split_path.size == 1) {
                        split_path.first().removePrefix("/tree/")
                    }
                    else {
                        val storage = split_path.first().split('/').last().capitalize(Locale(SpMp.ui_language))
                        "($storage) ~/${split_path.last()}"
                    }
                }
            },
            { setValue, showDialog ->
                SpMp.context.promptForUserDirectory(true) { path ->
                    val old_location = MediaItemLibrary.getStorageLocation(SpMp.context)
                    setValue(path ?: "")
                    val new_location = MediaItemLibrary.getStorageLocation(SpMp.context)

                    if (old_location.uri == new_location.uri) {
                        return@promptForUserDirectory
                    }

                    showDialog(
                        getStringTODO("Transfer existing library"),
                        getStringTODO("Move the library at ${old_location.path} to ${new_location.path}?")
                    ) {
                        old_location.moveDirContentTo(new_location) { error ->
                            // TODO
                        }
                    }
                }
            }
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_SHOW_LIKES_PLAYLIST.name),
            getString("s_key_show_likes_playlist"), null
        )
    )
}