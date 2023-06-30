package com.toasterofbread.spmp.model.mediaitem

class MediaItemBrowseEndpoint {
    val id: String
    val type: Type

    constructor(id: String, type: Type) {
        this.id = id
        this.type = type
    }

    constructor(id: String, type_name: String) {
        this.id = id
        this.type = Type.fromString(type_name)
    }

    enum class Type {
        CHANNEL,
        ARTIST,
        ALBUM;

        companion object {
            fun fromString(type_name: String): Type {
                return when (type_name) {
                    "MUSIC_PAGE_TYPE_USER_CHANNEL" -> CHANNEL
                    "MUSIC_PAGE_TYPE_ARTIST" -> ARTIST
                    "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                    else -> throw NotImplementedError(type_name)
                }
            }
        }
    }
}
