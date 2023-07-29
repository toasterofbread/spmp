package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.unit.IntSize
import mediaitem.CustomImageProviderById
import mediaitem.ThumbnailProviderById

data class MediaItemThumbnailProvider(
    val url_a: String,
    val url_b: String?
) {
    fun isStatic(): Boolean {
        return url_b == null || url_b.startsWith("https://")
    }

    fun getThumbnailUrl(quality: Quality): String? {
        // Set provider
        if (isStatic()) {
            if (url_b == null) {
                return url_a
            }

            return when (quality) {
                Quality.HIGH -> url_a
                Quality.LOW -> url_b
            }
        }

        // Dynamic provdier
        val target_size = quality.getTargetSize()
        return "$url_a${target_size.width}-h${target_size.height}$url_b"
    }

    companion object {
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
                return MediaItemThumbnailProvider(
                    thumbnail.url.substring(0, w_index + 1),
                    thumbnail.url.substring(h_index + 2 + thumbnail.height.toString().length)
                )
            }

            val high_url = thumbnails.maxByOrNull { it.width * it.height }!!.url
            val low_url = thumbnails.minByOrNull { it.width * it.height }!!.url

            // Set provider
            return MediaItemThumbnailProvider(
                high_url,
                if (high_url == low_url) null else low_url
            )
        }
    }

    data class Thumbnail(val url: String, val width: Int, val height: Int)
    enum class Quality {
        LOW, HIGH;

        fun getTargetSize(): IntSize {
            return when (this) {
                LOW -> IntSize(180, 180)
                HIGH -> IntSize(720, 720)
            }
        }
    }
}

fun ThumbnailProviderById.toThumbnailProvider(): MediaItemThumbnailProvider? =
    if (thumb_url_a == null) null
    else MediaItemThumbnailProvider(thumb_url_a, thumb_url_b)

fun CustomImageProviderById.toThumbnailProvider(): MediaItemThumbnailProvider? =
    if (custom_image_url_a == null) null
    else MediaItemThumbnailProvider(custom_image_url_a, custom_image_url_b)

