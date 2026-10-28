package com.example.data.model

import com.squareup.moshi.Json

enum class JobStatus(val value: String) {
    @Json(name = "queued") QUEUED("queued"),
    @Json(name = "starting") STARTING("starting"),
    @Json(name = "processing") PROCESSING("processing"),
    @Json(name = "waiting_for_result") WAITING_FOR_RESULT("waiting_for_result"),
    @Json(name = "completed") COMPLETED("completed"),
    @Json(name = "failed") FAILED("failed"),
    @Json(name = "cancelled") CANCELLED("cancelled"),
    @Json(name = "timed_out") TIMED_OUT("timed_out");

    val isTerminal: Boolean
        get() = this in listOf(COMPLETED, FAILED, CANCELLED, TIMED_OUT)

    val isCancellable: Boolean
        get() = this in listOf(QUEUED, STARTING, PROCESSING, WAITING_FOR_RESULT)

    val isRetryable: Boolean
        get() = this in listOf(FAILED, CANCELLED, TIMED_OUT)

    companion object {
        fun fromString(str: String?): JobStatus {
            if (str == null) return QUEUED
            val normalized = str.lowercase().trim()
            return entries.firstOrNull { it.value == normalized } ?: QUEUED
        }
    }
}
