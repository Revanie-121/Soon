package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class MusicEntityType {
    @Json(name = "TRACK") TRACK,
    @Json(name = "ALBUM") ALBUM
}

@JsonClass(generateAdapter = true)
data class MusicItem(
    @param:Json(name = "asin") val asin: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "artist") val artist: String,
    @param:Json(name = "year") val year: Int? = null,
    @param:Json(name = "type") val type: MusicEntityType = MusicEntityType.TRACK,
    @param:Json(name = "artwork_url") val artworkUrl: String? = null,
    @param:Json(name = "source_url") val sourceUrl: String? = null
)
