package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemData

data class YoutubeiShelfContentsItem(
    val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    val musicMultiRowListItemRenderer: MusicMultiRowListItemRenderer?
) {
    // Pair(item, playlistSetVideoId)
    fun toMediaItemData(hl: String): Pair<MediaItemData, String?>? {
        if (musicTwoRowItemRenderer != null) {
            return musicTwoRowItemRenderer.toMediaItem(hl)?.let { Pair(it, null) }
        }
        else if (musicResponsiveListItemRenderer != null) {
            return musicResponsiveListItemRenderer.toMediaItemAndPlaylistSetVideoId(hl)
        }
        else if (musicMultiRowListItemRenderer != null) {
            return Pair(musicMultiRowListItemRenderer.toMediaItem(hl), null)
        }

        throw NotImplementedError()
    }
}
