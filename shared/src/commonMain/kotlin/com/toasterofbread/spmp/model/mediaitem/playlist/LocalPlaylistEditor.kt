package com.toasterofbread.spmp.model.mediaitem.playlist

import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.model.external.PlaylistEditor

class LocalPlaylistEditor(
    val playlist: LocalPlaylist,
    val context: AppContext
): PlaylistEditor {
    override suspend fun performAndCommitActions(actions: List<PlaylistEditor.Action>): Result<Unit> {
        if (actions.isEmpty()) {
            return Result.success(Unit)
        }

        val param_data: LocalPlaylistData? = if (playlist is LocalPlaylistData) playlist else null

        val data: LocalPlaylistData = playlist.loadData(context).fold(
            { it },
            {
                return Result.failure(it)
            }
        )
        val items: MutableList<SongData> = (data.items ?: emptyList()).toMutableList()

        for ((i, action) in actions.withIndex()) {
            try {
                when (action) {
                    is PlaylistEditor.Action.Add -> {
                        items.add(SongData(action.song_id))
                    }
                    is PlaylistEditor.Action.Move -> {
                        items.add(action.to, items.removeAt(action.from))
                    }
                    is PlaylistEditor.Action.Remove -> {
                        items.removeAt(action.index)
                    }

                    is PlaylistEditor.Action.SetTitle -> {
                        data.name = action.title
                        param_data?.name = action.title
                    }
                    is PlaylistEditor.Action.SetDescription -> {
                        data.description = action.description
                        param_data?.description = action.description
                    }
                    is PlaylistEditor.Action.SetImage -> {
                        data.custom_image_url = action.image_url
                        param_data?.custom_image_url = action.image_url
                    }
                    is PlaylistEditor.Action.SetImageWidth -> {
                        data.image_width = action.image_width
                        param_data?.image_width = action.image_width
                    }
                }
            }
            catch (e: Throwable) {
                return Result.failure(
                    RuntimeException("Applying edit action $action (${i + 1} of ${actions.size}) to local playlist $playlist failed\nAll actions: ${actions.toList()}\nItems: ${items.toList()}", e)
                )
            }
        }

        data.items = items
        param_data?.items = items

        val file: PlatformFile =
            MediaItemLibrary.getLocalPlaylistFile(playlist, context)
            ?: return Result.success(Unit)

        return data.saveToFile(file, context)
    }

    override suspend fun performDeletion(): Result<Unit> {
        val file: PlatformFile =
            MediaItemLibrary.getLocalPlaylistFile(playlist, context)
            ?: return Result.success(Unit)

        if (file.delete()) {
            return Result.success(Unit)
        }
        else {
            return Result.failure(RuntimeException("Failed to delete file at ${file.absolute_path}"))
        }
    }
}
