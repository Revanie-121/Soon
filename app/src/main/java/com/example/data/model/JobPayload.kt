package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JobPayloadItem(
    @param:Json(name = "asin") val asin: String,
    @param:Json(name = "type") val type: String, // "TRACK" or "ALBUM"
    @param:Json(name = "url") val url: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "artist") val artist: String,
    @param:Json(name = "year") val year: Int? = null
)

@JsonClass(generateAdapter = true)
data class JobPayload(
    @param:Json(name = "schema_version") val schemaVersion: String = "1.0",
    @param:Json(name = "job_id") val jobId: String? = null,
    @param:Json(name = "source") val source: String = "amazon_music",
    @param:Json(name = "items") val items: List<JobPayloadItem>
)

@JsonClass(generateAdapter = true)
data class JobCreateResponse(
    @param:Json(name = "job_id") val jobId: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class JobStatusResponse(
    @param:Json(name = "job_id") val jobId: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "progress") val progress: Float? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "direct_url") val directUrl: String? = null,
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "logs") val logs: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "version") val version: String? = "1.0.0",
    @param:Json(name = "worker_status") val workerStatus: String? = "idle",
    @param:Json(name = "provider_status") val providerStatus: String? = "ready"
)

@JsonClass(generateAdapter = true)
data class GenericActionResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "message") val message: String
)
