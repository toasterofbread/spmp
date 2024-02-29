package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.unit.IntSize
import com.toasterofbread.spmp.db.mediaitem.ThumbnailProviderById

interface MediaItemThumbnailProvider {
    fun getThumbnailUrl(quality: Quality): String?
    override fun equals(other: Any?): Boolean

    data class Thumbnail(val url: String, val width: Int, val height: Int)
    enum class Quality {
        LOW, HIGH;

        fun getTargetSize(): IntSize {
            return when (this) {
                LOW -> IntSize(180, 180)
                HIGH -> IntSize(720, 720)
            }
        }

        companion object {
            fun byQuality(max: Quality = entries.last()): Iterable<Quality> =
                if (max == HIGH) listOf(HIGH, LOW)
                else listOf(LOW)
        }
    }

    companion object {
        fun fromImageUrl(url: String): MediaItemThumbnailProvider {
            return MediaItemThumbnailProviderImpl(url, null)
        }

        fun fromThumbnails(thumbnails: List<Thumbnail>): MediaItemThumbnailProvider? {
            if (thumbnails.isEmpty()) {
                return null
            }

            // Attempt to find dynamic thumbnail
            for (thumbnail in thumbnails) {
                val w_index = thumbnail.url.lastIndexOf("w${thumbnail.width}")
                if (w_index == -1) {
                    continue
                }

                val h_index = thumbnail.url.lastIndexOf("-h${thumbnail.height}")
                if (h_index == -1) {
                    continue
                }

                // Dynamic provider
                return MediaItemThumbnailProviderImpl(
                    thumbnail.url.substring(0, w_index + 1),
                    thumbnail.url.substring(h_index + 2 + thumbnail.height.toString().length)
                )
            }

            val high_url = thumbnails.maxByOrNull { it.width * it.height }!!.url
            val low_url = thumbnails.minByOrNull { it.width * it.height }!!.url

            // Set provider
            return MediaItemThumbnailProviderImpl(
                high_url,
                if (high_url == low_url) null else low_url
            )
        }
    }
}

data class MediaItemThumbnailProviderImpl(
    val url_a: String,
    val url_b: String?
): MediaItemThumbnailProvider {
    private fun isStatic(): Boolean {
        return url_b == null || url_b.startsWith("https://")
    }

    override fun getThumbnailUrl(quality: MediaItemThumbnailProvider.Quality): String? {
        // Set provider
        if (isStatic()) {
            if (url_b == null) {
                return url_a
            }

            return when (quality) {
                MediaItemThumbnailProvider.Quality.HIGH -> url_a
                MediaItemThumbnailProvider.Quality.LOW -> url_b
            }
        }

        // Dynamic provdier
        val target_size = quality.getTargetSize()
        return "$url_a${target_size.width}-h${target_size.height}$url_b"
    }
}

fun ThumbnailProviderById.toThumbnailProvider(): MediaItemThumbnailProvider? =
    if (thumb_url_a == null) null
    else MediaItemThumbnailProviderImpl(thumb_url_a, thumb_url_b)
