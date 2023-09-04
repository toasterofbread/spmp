package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider

data class Thumbnails(val musicThumbnailRenderer: MusicThumbnailRenderer?, val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?) {
    init {
        assert(musicThumbnailRenderer != null || croppedSquareThumbnailRenderer != null)
    }
    val thumbnails: List<MediaItemThumbnailProvider.Thumbnail> get() = (musicThumbnailRenderer ?: croppedSquareThumbnailRenderer!!).thumbnail.thumbnails
}
data class MusicThumbnailRenderer(val thumbnail: Thumbnail) {
    data class Thumbnail(val thumbnails: List<MediaItemThumbnailProvider.Thumbnail>)
}

data class ThumbnailRenderer(val musicThumbnailRenderer: MusicThumbnailRenderer) {
    fun toThumbnailProvider(): MediaItemThumbnailProvider {
        return MediaItemThumbnailProvider.fromThumbnails(musicThumbnailRenderer.thumbnail.thumbnails)!!
    }
}
