package com.toasterofbread.spmp.model.mediaitem.library

import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformFile

object MediaItemLibrary {
    fun getStorageLocation(
        context: PlatformContext,
        custom_location_uri: String = Settings.KEY_LIBRARY_PATH.get(context),
    ): PlatformFile {
        if (custom_location_uri.isBlank()) {
            return getDefaultStorageLocation(context)
        }
        return context.getUserDirectoryFile(custom_location_uri)
    }

    fun getDefaultStorageLocation(context: PlatformContext): PlatformFile =
        PlatformFile.fromFile(context.getFilesDir(), context).resolve("library")

    fun getLocalSongsDir(context: PlatformContext): PlatformFile {
        return getStorageLocation(context).resolve("songs")
    }
}
