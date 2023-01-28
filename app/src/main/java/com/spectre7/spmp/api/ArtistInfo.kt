package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.model.MediaItem
import okhttp3.Request

data class MediaItemList(val title: String, val items: List<MediaItem>)
class ArtistInfoItem(
    val items: MediaItemList?,
    val description: Triple<String, String, String>?
)

fun getArtistInfo(artist_id: String): Result<List<ArtistInfoItem>> {
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody("""
            {
                "browseId": "$artist_id"
            }
        """.trimIndent()))
        .build()

    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    val parsed = klaxon.parseJsonObject(response.body!!.charStream())

    val songs = parsed
        .obj("contents")!!
        .obj("singleColumnBrowseResultsRenderer")!!
        .array<JsonObject>("tabs")!![0]
        .obj("tabRenderer")!!
        .obj("content")!!
        .obj("sectionListRenderer")!!
        .array<JsonObject>("contents")!!

    val rows = klaxon.parseFromJsonArray<YoutubeiShelf>(songs)!!
    return Result.success(List(rows.size) { i ->
        val row = rows[i]

        when (val renderer = row.getRenderer()) {
            is MusicDescriptionShelfRenderer ->
                ArtistInfoItem(null, Triple(renderer.header.first_text, renderer.subheader.first_text, renderer.description.first_text))
            is MusicShelfRenderer, is MusicCarouselShelfRenderer ->
                ArtistInfoItem(MediaItemList(row.title.text, List(row.contents.size) { j ->
                    row.contents[j].toMediaItem()
                }), null)
            else -> throw NotImplementedError(renderer.javaClass.name)
        }
    })
}