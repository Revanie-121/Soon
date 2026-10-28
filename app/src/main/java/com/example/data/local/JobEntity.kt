package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus

@Entity(tableName = "download_jobs")
data class JobEntity(
    @PrimaryKey val jobId: String,
    val asin: String,
    val type: String,
    val url: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val status: String,
    val progress: Float?,
    val message: String?,
    val directUrl: String?,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val logSnippet: String?
) {
    fun toDomain(): DownloadJob {
        return DownloadJob(
            jobId = jobId,
            asin = asin,
            type = type,
            url = url,
            title = title,
            artist = artist,
            year = year,
            status = JobStatus.fromString(status),
            progress = progress,
            message = message,
            directUrl = directUrl,
            error = error,
            createdAt = createdAt,
            updatedAt = updatedAt,
            logSnippet = logSnippet
        )
    }

    companion object {
        fun fromDomain(job: DownloadJob): JobEntity {
            return JobEntity(
                jobId = job.jobId,
                asin = job.asin,
                type = job.type,
                url = job.url,
                title = job.title,
                artist = job.artist,
                year = job.year,
                status = job.status.value,
                progress = job.progress,
                message = job.message,
                directUrl = job.directUrl,
                error = job.error,
                createdAt = job.createdAt,
                updatedAt = job.updatedAt,
                logSnippet = job.logSnippet
            )
        }
    }
}
