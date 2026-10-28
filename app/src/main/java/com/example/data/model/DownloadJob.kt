package com.example.data.model

data class DownloadJob(
    val jobId: String,
    val asin: String,
    val type: String, // "TRACK" or "ALBUM"
    val url: String,
    val title: String,
    val artist: String,
    val year: Int? = null,
    val status: JobStatus = JobStatus.QUEUED,
    val progress: Float? = null,
    val message: String? = null,
    val directUrl: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val logSnippet: String? = null
)
