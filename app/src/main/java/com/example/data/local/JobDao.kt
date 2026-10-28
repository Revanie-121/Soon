package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Query("SELECT * FROM download_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM download_jobs WHERE status NOT IN ('completed', 'failed', 'cancelled', 'timed_out') ORDER BY createdAt DESC")
    fun getActiveJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM download_jobs WHERE status IN ('completed', 'failed', 'cancelled', 'timed_out') ORDER BY createdAt DESC")
    fun getHistoryJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM download_jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun getJobById(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(job: JobEntity)

    @Query("UPDATE download_jobs SET status = :status, message = :message, progress = :progress, directUrl = :directUrl, error = :error, updatedAt = :updatedAt, logSnippet = :logs WHERE jobId = :jobId")
    suspend fun updateJobStatus(
        jobId: String,
        status: String,
        message: String?,
        progress: Float?,
        directUrl: String?,
        error: String?,
        updatedAt: Long,
        logs: String?
    )

    @Query("DELETE FROM download_jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM download_jobs WHERE status IN ('completed', 'failed', 'cancelled', 'timed_out')")
    suspend fun clearHistory()
}
