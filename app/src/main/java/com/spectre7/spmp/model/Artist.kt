package com.spectre7.spmp.model

import java.util.*

data class ArtistData (
    val locale: String?,
    val name: String,
    val description: String
)

data class Artist (
    val id: String,
    val nativeData: ArtistData,
    val creationDate: Date,

    val viewCount: String,
    val subscriberCount: String,
    val hiddenSubscriberCount: Boolean,
    val videoCount: String
)