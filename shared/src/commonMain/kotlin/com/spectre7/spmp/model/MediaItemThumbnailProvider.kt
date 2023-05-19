package com.spectre7.spmp.model

import androidx.compose.ui.unit.IntSize
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon

abstract class MediaItemThumbnailProvider {
    abstract fun getThumbnailUrl(quality: Quality): String?

    data class SetProvider(val thumbnails: List<Thumbnail>): MediaItemThumbnailProvider() {
        override fun getThumbnailUrl(quality: Quality): String? {
            return when (quality) {
                Quality.HIGH -> thumbnails.minByOrNull { it.width * it.height }
                Quality.LOW -> thumbnails.maxByOrNull { it.width * it.height }
            }?.url
        }
    }

    data class DynamicProvider(val url_a: String, val url_b: String): MediaItemThumbnailProvider() {
        override fun getThumbnailUrl(quality: Quality): String {
            val target_size = quality.getTargetSize()
            return "$url_a${target_size.width}-h${target_size.height}$url_b"
        }

        companion object {
            fun fromDynamicUrl(url: String, width: Int, height: Int): DynamicProvider? {
                val w_index = url.lastIndexOf("w$width")
                val h_index = url.lastIndexOf("-h$height")

                if (w_index == -1 || h_index == -1) {
                    return null
                }

                return DynamicProvider(
                    url.substring(0, w_index + 1),
                    url.substring(h_index + 2 + height.toString().length)
                )
            }
        }
    }
    data class Thumbnail(val url: String, val width: Int, val height: Int)

    companion object {
        fun fromThumbnails(thumbnails: List<Thumbnail>): MediaItemThumbnailProvider? {
            if (thumbnails.isEmpty()) {
                return null
            }

            for (thumbnail in thumbnails) {
                val dynamic_provider = DynamicProvider.fromDynamicUrl(thumbnail.url, thumbnail.width, thumbnail.height)
                if (dynamic_provider != null) {
                    return dynamic_provider
                }
            }
            return SetProvider(thumbnails)
        }

        fun fromJsonObject(obj: JsonObject, klaxon: Klaxon): MediaItemThumbnailProvider? {
            if (obj.containsKey("thumbnails")) {
                return klaxon.parseFromJsonObject<SetProvider>(obj)
            }
            return klaxon.parseFromJsonObject<DynamicProvider>(obj)
        }
    }

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
